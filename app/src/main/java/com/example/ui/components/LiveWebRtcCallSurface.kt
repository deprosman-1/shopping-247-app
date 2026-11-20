package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.remote.LiveCallMode
import org.json.JSONObject

class LiveWebRtcBridge(
    private val onOutboundSignal: (type: String, dataJson: String) -> Unit,
    private val onConnectionStateChanged: (String) -> Unit,
    private val onAudioLevels: (local: Int, remote: Int) -> Unit,
    private val onError: (String) -> Unit
) {
    @JavascriptInterface
    fun postSignal(type: String, dataJson: String) {
        onOutboundSignal(type, dataJson)
    }

    @JavascriptInterface
    fun postConnectionState(state: String) {
        onConnectionStateChanged(state)
    }

    @JavascriptInterface
    fun postAudioLevels(localLevel: Int, remoteLevel: Int) {
        onAudioLevels(localLevel, remoteLevel)
    }

    @JavascriptInterface
    fun postError(message: String) {
        onError(message)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveWebRtcCallSurface(
    roomId: String,
    isActiveCall: Boolean,
    isCallerInitiator: Boolean,
    mode: LiveCallMode,
    isMicMuted: Boolean,
    isSpeakerOn: Boolean,
    isUsingFrontCamera: Boolean,
    inboundSignalJson: String?,
    onInboundSignalConsumed: () -> Unit,
    onOutboundSignal: (signalJson: String) -> Unit,
    onConnectionStateChanged: (String) -> Unit,
    onAudioLevelsChanged: (local: Int, remote: Int) -> Unit,
    onMediaError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPageReady by remember { mutableStateOf(false) }

    // Configure Android AudioManager for live voice/video communication
    LaunchedEffect(isActiveCall, isSpeakerOn) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am != null) {
            try {
                if (isActiveCall) {
                    am.mode = AudioManager.MODE_IN_COMMUNICATION
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = isSpeakerOn
                } else {
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = false
                    am.mode = AudioManager.MODE_NORMAL
                }
            } catch (_: Exception) {
            }
        }
    }

    val htmlContent = remember { buildWebRtcHtml() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#052E16"))
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                addJavascriptInterface(
                    LiveWebRtcBridge(
                        onOutboundSignal = { type, dataJson ->
                            val wrapped = JSONObject().apply {
                                put("signalKind", type)
                                put("data", dataJson)
                            }.toString()
                            onOutboundSignal(wrapped)
                        },
                        onConnectionStateChanged = onConnectionStateChanged,
                        onAudioLevels = onAudioLevelsChanged,
                        onError = onMediaError
                    ),
                    "DeprosNative"
                )

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        isPageReady = true
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        if (request == null) return
                        val allowed = mutableListOf<String>()
                        val hasAudio = ContextCompat.checkSelfPermission(
                            ctx,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCamera = ContextCompat.checkSelfPermission(
                            ctx,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        request.resources.forEach { res ->
                            when (res) {
                                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> if (hasAudio) allowed.add(res)
                                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> if (hasCamera) allowed.add(res)
                            }
                        }
                        if (allowed.isNotEmpty()) {
                            request.grant(allowed.toTypedArray())
                        } else {
                            request.deny()
                        }
                    }
                }

                loadDataWithBaseURL(
                    "https://deprosman-1.github.io/",
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
                webViewRef = this
            }
        }
    )

    // Start or stop peer session cleanly
    LaunchedEffect(isPageReady, isActiveCall, roomId) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (!isPageReady) return@LaunchedEffect
        if (isActiveCall && roomId.isNotBlank()) {
            val polite = !isCallerInitiator
            val wantVideo = mode == LiveCallMode.VIDEO
            val facing = if (isUsingFrontCamera) "user" else "environment"
            wv.evaluateJavascript(
                "window.__deprosCall && window.__deprosCall.start(${JSONObject.quote(roomId)}, $polite, $wantVideo, ${JSONObject.quote(facing)}, $isMicMuted);",
                null
            )
        } else {
            wv.evaluateJavascript("window.__deprosCall && window.__deprosCall.stop();", null)
        }
    }

    // Live switch between Audio Talk and Video Call mid-call
    LaunchedEffect(isPageReady, isActiveCall, mode, isUsingFrontCamera) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (isPageReady && isActiveCall) {
            val wantVideo = mode == LiveCallMode.VIDEO
            val facing = if (isUsingFrontCamera) "user" else "environment"
            wv.evaluateJavascript(
                "window.__deprosCall && window.__deprosCall.setVideoMode($wantVideo, ${JSONObject.quote(facing)});",
                null
            )
        }
    }

    // Live mic mute toggle
    LaunchedEffect(isPageReady, isActiveCall, isMicMuted) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (isPageReady && isActiveCall) {
            wv.evaluateJavascript(
                "window.__deprosCall && window.__deprosCall.setMuted($isMicMuted);",
                null
            )
        }
    }

    // Deliver inbound SDP offer / answer / ICE candidate from remote device
    LaunchedEffect(isPageReady, inboundSignalJson) {
        val raw = inboundSignalJson ?: return@LaunchedEffect
        val wv = webViewRef ?: return@LaunchedEffect
        if (isPageReady) {
            wv.evaluateJavascript(
                "window.__deprosCall && window.__deprosCall.onRemoteSignal(${JSONObject.quote(raw)});",
                null
            )
            onInboundSignalConsumed()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (am != null) {
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = false
                    am.mode = AudioManager.MODE_NORMAL
                }
            } catch (_: Exception) {
            }
            webViewRef?.apply {
                evaluateJavascript("window.__deprosCall && window.__deprosCall.stop();", null)
                stopLoading()
                destroy()
            }
            webViewRef = null
        }
    }
}

private fun buildWebRtcHtml(): String {
    return """
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    html, body {
      width: 100%;
      height: 100%;
      background: #052E16;
      overflow: hidden;
      font-family: -apple-system, BlinkMacSystemFont, sans-serif;
    }
    #stage {
      position: relative;
      width: 100%;
      height: 100%;
      background: radial-gradient(circle at top right, #064E3B 0%, #052E16 70%);
    }
    #remoteVideo {
      width: 100%;
      height: 100%;
      object-fit: cover;
      background: #052E16;
      display: block;
    }
    #localWrap {
      position: absolute;
      right: 14px;
      bottom: 14px;
      width: 104px;
      height: 144px;
      border-radius: 16px;
      overflow: hidden;
      border: 2px solid rgba(245, 158, 11, 0.85);
      box-shadow: 0 10px 26px rgba(0,0,0,0.55);
      background: #064E3B;
      display: none;
    }
    #localVideo {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transform: scaleX(-1);
    }
  </style>
</head>
<body>
  <div id="stage">
    <video id="remoteVideo" autoplay playsinline></video>
    <div id="localWrap">
      <video id="localVideo" autoplay muted playsinline></video>
    </div>
  </div>
  <script>
    (function() {
      const ICE_SERVERS = [
        { urls: "stun:stun.l.google.com:19302" },
        { urls: "stun:stun1.l.google.com:19302" },
        { urls: "stun:stun2.l.google.com:19302" },
        { urls: "stun:stun.cloudflare.com:3478" }
      ];

      let pc = null;
      let localStream = null;
      let remoteStream = new MediaStream();
      let audioTrack = null;
      let videoTrack = null;
      let videoSender = null;
      let audioSender = null;
      let polite = false;
      let makingOffer = false;
      let ignoreOffer = false;
      let activeRoom = "";
      let wantVideoMode = false;
      let currentFacing = "user";
      let isMuted = false;
      let levelTimer = null;
      let audioCtx = null;
      let localAnalyser = null;
      let remoteAnalyser = null;
      let pendingCandidates = [];

      const remoteVideo = document.getElementById("remoteVideo");
      const localVideo = document.getElementById("localVideo");
      const localWrap = document.getElementById("localWrap");
      remoteVideo.srcObject = remoteStream;

      function emitSignal(kind, data) {
        if (window.DeprosNative && window.DeprosNative.postSignal) {
          window.DeprosNative.postSignal(kind, JSON.stringify(data));
        }
      }

      function emitState(st) {
        if (window.DeprosNative && window.DeprosNative.postConnectionState) {
          window.DeprosNative.postConnectionState(st);
        }
      }

      function emitErr(msg) {
        if (window.DeprosNative && window.DeprosNative.postError) {
          window.DeprosNative.postError(String(msg || "Media error"));
        }
      }

      function setupAudioMeter(stream, isLocal) {
        try {
          if (!audioCtx) {
            const Ctx = window.AudioContext || window.webkitAudioContext;
            if (!Ctx) return;
            audioCtx = new Ctx();
          }
          if (audioCtx.state === "suspended") audioCtx.resume();
          const src = audioCtx.createMediaStreamSource(stream);
          const analyser = audioCtx.createAnalyser();
          analyser.fftSize = 128;
          src.connect(analyser);
          if (isLocal) localAnalyser = analyser;
          else remoteAnalyser = analyser;
        } catch (_) {}
      }

      function readLevel(analyser) {
        if (!analyser) return 0;
        const arr = new Uint8Array(analyser.frequencyBinCount);
        analyser.getByteFrequencyData(arr);
        let sum = 0;
        for (let i = 0; i < arr.length; i++) sum += arr[i];
        const avg = sum / (arr.length || 1);
        return Math.min(100, Math.round((avg / 140) * 100));
      }

      function startLevelPolling() {
        if (levelTimer) clearInterval(levelTimer);
        levelTimer = setInterval(function() {
          const l = isMuted ? 0 : readLevel(localAnalyser);
          const r = readLevel(remoteAnalyser);
          if (window.DeprosNative && window.DeprosNative.postAudioLevels) {
            window.DeprosNative.postAudioLevels(l, r);
          }
        }, 120);
      }

      function createPeer() {
        if (pc) return pc;
        pc = new RTCPeerConnection({ iceServers: ICE_SERVERS });

        pc.onicecandidate = function(ev) {
          if (ev.candidate) {
            emitSignal("candidate", { candidate: ev.candidate });
          }
        };

        pc.ontrack = function(ev) {
          const stream = (ev.streams && ev.streams[0]) ? ev.streams[0] : null;
          if (ev.track) {
            const existing = remoteStream.getTracks().find(function(t) { return t.id === ev.track.id; });
            if (!existing) remoteStream.addTrack(ev.track);
            if (ev.track.kind === "audio") {
              setupAudioMeter(new MediaStream([ev.track]), false);
            }
          }
          if (stream) {
            remoteVideo.srcObject = stream;
          } else {
            remoteVideo.srcObject = remoteStream;
          }
        };

        pc.onnegotiationneeded = async function() {
          if (!pc || pc.signalingState !== "stable") return;
          try {
            makingOffer = true;
            await pc.setLocalDescription();
            emitSignal("description", { description: pc.localDescription });
          } catch (e) {
            emitErr("Negotiation error: " + e.message);
          } finally {
            makingOffer = false;
          }
        };

        pc.onconnectionstatechange = function() {
          if (!pc) return;
          emitState(pc.connectionState || "connecting");
          if (pc.connectionState === "failed") {
            try { pc.restartIce(); } catch (_) {}
          }
        };

        pc.oniceconnectionstatechange = function() {
          if (!pc) return;
          if (pc.iceConnectionState === "connected" || pc.iceConnectionState === "completed") {
            emitState("connected");
          } else if (pc.iceConnectionState === "disconnected") {
            emitState("reconnecting");
          }
        };
        return pc;
      }

      async function ensureAudioTrack() {
        if (audioTrack && audioTrack.readyState === "live") {
          audioTrack.enabled = !isMuted;
          return audioTrack;
        }
        const s = await navigator.mediaDevices.getUserMedia({
          audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
          video: false
        });
        audioTrack = s.getAudioTracks()[0] || null;
        if (audioTrack) {
          audioTrack.enabled = !isMuted;
          localStream = localStream || new MediaStream();
          localStream.addTrack(audioTrack);
          setupAudioMeter(new MediaStream([audioTrack]), true);
          const peer = createPeer();
          if (audioSender) {
            await audioSender.replaceTrack(audioTrack);
          } else {
            audioSender = peer.addTrack(audioTrack, localStream);
          }
        }
        return audioTrack;
      }

      async function applyVideoState(wantVideo, facing) {
        wantVideoMode = !!wantVideo;
        currentFacing = facing || "user";
        localVideo.style.transform = (currentFacing === "user") ? "scaleX(-1)" : "none";
        if (!pc) return;

        if (!wantVideoMode) {
          localWrap.style.display = "none";
          if (videoTrack) {
            try { videoTrack.stop(); } catch (_) {}
            videoTrack = null;
          }
          if (videoSender) {
            try { await videoSender.replaceTrack(null); } catch (_) {}
          }
          localVideo.srcObject = null;
          return;
        }

        try {
          if (videoTrack) {
            try { videoTrack.stop(); } catch (_) {}
            videoTrack = null;
          }
          const vs = await navigator.mediaDevices.getUserMedia({
            audio: false,
            video: { facingMode: currentFacing, width: { ideal: 960 }, height: { ideal: 720 } }
          });
          videoTrack = vs.getVideoTracks()[0] || null;
          if (videoTrack) {
            localStream = localStream || new MediaStream();
            localStream.addTrack(videoTrack);
            localVideo.srcObject = new MediaStream([videoTrack]);
            localWrap.style.display = "block";
            if (videoSender) {
              await videoSender.replaceTrack(videoTrack);
            } else {
              videoSender = pc.addTrack(videoTrack, localStream);
            }
          }
        } catch (e) {
          localWrap.style.display = "none";
          emitErr("Camera permission or hardware unavailable: " + e.message);
        }
      }

      window.__deprosCall = {
        start: async function(roomId, isPolite, wantVideo, facing, muted) {
          if (activeRoom === roomId && pc) return;
          this.stop();
          activeRoom = roomId;
          polite = !!isPolite;
          isMuted = !!muted;
          wantVideoMode = !!wantVideo;
          currentFacing = facing || "user";
          createPeer();
          startLevelPolling();
          try {
            await ensureAudioTrack();
            await applyVideoState(wantVideoMode, currentFacing);
          } catch (e) {
            emitErr("Microphone permission required for live call: " + e.message);
          }
        },
        setVideoMode: async function(wantVideo, facing) {
          if (!pc) return;
          await applyVideoState(wantVideo, facing);
        },
        setMuted: function(muted) {
          isMuted = !!muted;
          if (audioTrack) audioTrack.enabled = !isMuted;
        },
        onRemoteSignal: async function(raw) {
          try {
            const parsed = JSON.parse(raw);
            const kind = parsed.signalKind;
            const data = typeof parsed.data === "string" ? JSON.parse(parsed.data) : parsed.data;
            const peer = createPeer();

            if (kind === "description" && data && data.description) {
              const desc = data.description;
              const offerCollision = (desc.type === "offer") && (makingOffer || peer.signalingState !== "stable");
              ignoreOffer = !polite && offerCollision;
              if (ignoreOffer) return;

              await peer.setRemoteDescription(desc);
              while (pendingCandidates.length) {
                const c = pendingCandidates.shift();
                try { await peer.addIceCandidate(c); } catch (_) {}
              }
              if (desc.type === "offer") {
                await ensureAudioTrack();
                await peer.setLocalDescription();
                emitSignal("description", { description: peer.localDescription });
              }
            } else if (kind === "candidate" && data && data.candidate) {
              try {
                if (peer.remoteDescription) {
                  await peer.addIceCandidate(data.candidate);
                } else {
                  pendingCandidates.push(data.candidate);
                }
              } catch (e) {
                if (!ignoreOffer) throw e;
              }
            }
          } catch (e) {
            emitErr("Signal error: " + e.message);
          }
        },
        stop: function() {
          activeRoom = "";
          pendingCandidates = [];
          if (levelTimer) { clearInterval(levelTimer); levelTimer = null; }
          if (audioTrack) { try { audioTrack.stop(); } catch (_) {} audioTrack = null; }
          if (videoTrack) { try { videoTrack.stop(); } catch (_) {} videoTrack = null; }
          audioSender = null;
          videoSender = null;
          if (pc) { try { pc.close(); } catch (_) {} pc = null; }
          remoteStream = new MediaStream();
          remoteVideo.srcObject = remoteStream;
          localVideo.srcObject = null;
          localWrap.style.display = "none";
          localAnalyser = null;
          remoteAnalyser = null;
        }
      };
    })();
  </script>
</body>
</html>
    """.trimIndent()
}
