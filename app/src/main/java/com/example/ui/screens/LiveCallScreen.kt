package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.MainViewModel
import com.example.data.remote.LiveCallMode
import com.example.data.remote.LiveCallPhase
import com.example.ui.components.LiveWebRtcCallSurface
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SuccessGreen
import com.example.util.WhatsAppHelper
import java.util.Locale

private val DeepEmerald = Color(0xFF052E16)
private val EmeraldSurface = Color(0xFF064E3B)
private val EmeraldHighlight = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveCallScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val callState by viewModel.liveCallState.collectAsState()
    val inboundSignal by viewModel.inboundWebRtcSignal.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val isSupabaseConnected by viewModel.isSupabaseConnected.collectAsState()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsState()
    val currentCustomer by viewModel.currentCustomer.collectAsState()
    val orders by viewModel.allOrders.collectAsState()
    val showAdminControl = isAdminUnlocked || (currentCustomer?.isAdmin == true)

    var selectedTopic by remember { mutableStateOf("Product & Stock Inquiry") }
    var customNote by remember { mutableStateOf("") }
    var showChatOverlay by remember { mutableStateOf(false) }
    var chatDraft by remember { mutableStateOf("") }
    var pendingActionAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micGranted = results[Manifest.permission.RECORD_AUDIO]
            ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        if (micGranted) {
            pendingActionAfterPermission?.invoke()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is needed to talk live with Admin.",
                Toast.LENGTH_LONG
            ).show()
        }
        pendingActionAfterPermission = null
    }

    fun ensurePermissionsAndRun(requireCamera: Boolean, action: () -> Unit) {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (requireCamera && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CAMERA)
        }
        if (needed.isEmpty()) {
            action()
        } else {
            pendingActionAfterPermission = action
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    val isCallInProgress = callState.phase == LiveCallPhase.RINGING_OUTGOING ||
        callState.phase == LiveCallPhase.CONNECTING ||
        callState.phase == LiveCallPhase.CONNECTED ||
        callState.phase == LiveCallPhase.RECONNECTING

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepEmerald,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                title = {
                    Column {
                        Text(
                            text = if (isCallInProgress) callState.peerName else "Live Audio & Video Desk",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isCallInProgress) {
                                "${callState.phase.label} • ${formatCallDuration(callState.elapsedSeconds)}"
                            } else {
                                if (showAdminControl && callState.isRealtimeConnected) "Live Cloud Ready" else "Store Interactive Desk"
                            },
                            fontSize = 11.sp,
                            color = AccentGold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isCallInProgress) {
                                Toast.makeText(context, "Call stays active in background or tap End to finish.", Toast.LENGTH_SHORT).show()
                            }
                            viewModel.navigateBack()
                        },
                        modifier = Modifier.testTag("live_call_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Instant Audio <-> Video Mode Switcher Pill in Header
                    val targetMode = if (callState.mode == LiveCallMode.AUDIO) LiveCallMode.VIDEO else LiveCallMode.AUDIO
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (callState.mode == LiveCallMode.VIDEO) AccentGold else EmeraldSurface,
                        border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                ensurePermissionsAndRun(requireCamera = targetMode == LiveCallMode.VIDEO) {
                                    if (isCallInProgress) {
                                        viewModel.switchLiveCallMode(targetMode)
                                    } else {
                                        viewModel.selectLiveCallPreMode(targetMode)
                                    }
                                }
                            }
                            .testTag("switch_audio_video_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (callState.mode == LiveCallMode.VIDEO) Icons.Default.Videocam else Icons.Default.Mic,
                                contentDescription = "Switch between Audio and Video",
                                tint = if (callState.mode == LiveCallMode.VIDEO) DeepEmerald else AccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (callState.mode == LiveCallMode.VIDEO) "Video" else "Audio",
                                color = if (callState.mode == LiveCallMode.VIDEO) DeepEmerald else Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
            .fillMaxSize()
            .background(DeepEmerald)
            .testTag("live_call_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(DeepEmerald, Color(0xFF021F10))
                    )
                )
                .padding(innerPadding)
        ) {
            if (!isCallInProgress) {
                // =============================================================
                // PRE-CALL LOBBY STAGE (SWITCHABLE AUDIO / VIDEO SETUP & ADMIN QUEUE)
                // =============================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Store Cloud Status Notice (Admin only)
                        if (showAdminControl && !isSupabaseConnected) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF78350F).copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Store Cloud is currently offline on this device. Connect Store Cloud (or ask Admin for the Store Sync Link) for live cross-device Audio/Video calls.",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Admin Availability & Incoming Queue Card (Visible on Admin Devices)
                        if (showAdminControl) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
                                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Admin Live Call Desk",
                                                color = AccentGold,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = if (callState.adminOnline) "Customers see you ONLINE for live calls" else "Switch ON to receive customer rings",
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 11.5.sp
                                            )
                                        }
                                        Switch(
                                            checked = callState.adminOnline,
                                            onCheckedChange = { viewModel.setAdminCallAvailability(it) },
                                            modifier = Modifier.testTag("admin_call_availability_switch")
                                        )
                                    }

                                    if (callState.incomingRequests.isEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = DeepEmerald.copy(alpha = 0.6f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "No customers ringing right now. Waiting calls will appear here automatically with voice alerts.",
                                                color = Color.White.copy(alpha = 0.78f),
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    } else {
                                        callState.incomingRequests.forEach { req ->
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = DeepEmerald,
                                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "${req.callerName} • ${req.mode.label}",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 14.sp
                                                            )
                                                            Text(
                                                                text = "Topic: ${req.topic}${if (req.callerPhone.isNotBlank()) " • ${req.callerPhone}" else ""}",
                                                                color = CyberCyan,
                                                                fontSize = 11.5.sp
                                                            )
                                                        }
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                ensurePermissionsAndRun(requireCamera = false) {
                                                                    viewModel.answerIncomingLiveCall(req, LiveCallMode.AUDIO)
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                            modifier = Modifier.weight(1f).height(48.dp)
                                                        ) {
                                                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("Audio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        Button(
                                                            onClick = {
                                                                ensurePermissionsAndRun(requireCamera = true) {
                                                                    viewModel.answerIncomingLiveCall(req, LiveCallMode.VIDEO)
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = DeepEmerald),
                                                            modifier = Modifier.weight(1f).height(48.dp)
                                                        ) {
                                                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(Modifier.width(4.dp))
                                                            Text("Video", fontSize = 12.sp, fontWeight = FontWeight.Black)
                                                        }
                                                        IconButton(
                                                            onClick = { viewModel.declineIncomingLiveCall(req) },
                                                            modifier = Modifier
                                                                .size(48.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(Color(0xFF991B1B))
                                                        ) {
                                                            Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = DeepEmerald.copy(alpha = 0.72f),
                                        border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("admin_studio_quick_guide_strip")
                                    ) {
                                        Text(
                                            text = "Admin Guide: Switch Desk ON -> Tap Audio or Video when a customer rings -> Switch Audio/Video, Mute, Speaker, Flip Camera, or Chat anytime during the call.",
                                            color = AccentGold.copy(alpha = 0.95f),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Hero Interactive Mode Card
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldSurface.copy(alpha = 0.92f)),
                            border = BorderStroke(1.2.dp, EmeraldHighlight.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (callState.adminOnline) SuccessGreen.copy(alpha = 0.2f) else AccentGold.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, if (callState.adminOnline) SuccessGreen else AccentGold)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (callState.mode == LiveCallMode.VIDEO) Icons.Default.Videocam else Icons.Default.HeadsetMic,
                                            contentDescription = null,
                                            tint = if (callState.adminOnline) SuccessGreen else AccentGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (callState.adminOnline) "Admin Online • Ready for Live Call" else "Live Call Studio • Switch Audio / Video Anytime",
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "Talk or Video Call Admin Live",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 21.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Choose Live Audio Talk or 2-Way Video Call below. You can switch smoothly between Audio and Video at any moment during the conversation.",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )

                                // Switchable Mode Selector Cards
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val audioSelected = callState.mode == LiveCallMode.AUDIO
                                    Surface(
                                        onClick = { viewModel.selectLiveCallPreMode(LiveCallMode.AUDIO) },
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (audioSelected) SuccessGreen.copy(alpha = 0.28f) else DeepEmerald.copy(alpha = 0.7f),
                                        border = BorderStroke(
                                            width = if (audioSelected) 2.dp else 1.dp,
                                            color = if (audioSelected) SuccessGreen else Color.White.copy(alpha = 0.22f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 108.dp)
                                            .testTag("call_mode_audio_chip")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Mic,
                                                contentDescription = "Audio Talk Mode",
                                                tint = if (audioSelected) SuccessGreen else Color.White,
                                                modifier = Modifier.size(30.dp)
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Text("Audio Talk", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                            Text("Voice + Waveform", color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
                                        }
                                    }

                                    val videoSelected = callState.mode == LiveCallMode.VIDEO
                                    Surface(
                                        onClick = { viewModel.selectLiveCallPreMode(LiveCallMode.VIDEO) },
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (videoSelected) AccentGold.copy(alpha = 0.22f) else DeepEmerald.copy(alpha = 0.7f),
                                        border = BorderStroke(
                                            width = if (videoSelected) 2.dp else 1.dp,
                                            color = if (videoSelected) AccentGold else Color.White.copy(alpha = 0.22f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 108.dp)
                                            .testTag("call_mode_video_chip")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Videocam,
                                                contentDescription = "Video Call Mode",
                                                tint = if (videoSelected) AccentGold else Color.White,
                                                modifier = Modifier.size(30.dp)
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Text("Video Call", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                            Text("2-Way Live Camera", color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
                                        }
                                    }
                                }

                                // Call Topic Chips
                                val topics = listOf("Product & Stock Inquiry", "Order & Payment", "Delivery Rider", "Bulk / Partner Deal")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    topics.forEach { t ->
                                        FilterChip(
                                            selected = selectedTopic == t,
                                            onClick = { selectedTopic = t },
                                            label = { Text(t, fontSize = 11.5.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AccentGold,
                                                selectedLabelColor = DeepEmerald,
                                                containerColor = DeepEmerald,
                                                labelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = customNote,
                                    onValueChange = { customNote = it },
                                    label = { Text("Quick Note for Admin (Optional)", color = Color.White.copy(alpha = 0.8f)) },
                                    placeholder = { Text("e.g. Confirming transfer or checking size 42", color = Color.White.copy(alpha = 0.5f)) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = AccentGold,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (callState.statusBanner.isNotBlank()) {
                                    Text(
                                        text = callState.statusBanner,
                                        color = AccentGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Primary Call Launch Button
                                Button(
                                    onClick = {
                                        val isVideo = callState.mode == LiveCallMode.VIDEO
                                        val finalTopic = customNote.trim().ifBlank { selectedTopic }
                                        ensurePermissionsAndRun(requireCamera = isVideo) {
                                            viewModel.startCustomerLiveCall(callState.mode, finalTopic)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (callState.mode == LiveCallMode.VIDEO) AccentGold else SuccessGreen,
                                        contentColor = if (callState.mode == LiveCallMode.VIDEO) DeepEmerald else Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("start_live_call_button")
                                ) {
                                    Icon(
                                        imageVector = if (callState.mode == LiveCallMode.VIDEO) Icons.Default.Videocam else Icons.Default.PhoneInTalk,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (callState.mode == LiveCallMode.VIDEO) "Start Live Video Call" else "Start Live Audio Call",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                }

                                // Direct Backup Row (WhatsApp / Phone)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val wa = storeProfile.whatsappNumber.ifBlank { storeProfile.phoneNumber }
                                            WhatsAppHelper.openWhatsAppChat(
                                                context = context,
                                                rawPhone = wa,
                                                message = "Hello ${storeProfile.storeName} Admin, I am calling about: ${customNote.ifBlank { selectedTopic }}"
                                            )
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("WhatsApp Admin", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val phone = storeProfile.phoneNumber.ifBlank { storeProfile.whatsappNumber }
                                            if (phone.isBlank()) {
                                                Toast.makeText(context, "Store phone not set yet.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Direct Dial", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Customer Quick Operating Guide Card
                        var showCustomerGuide by remember { mutableStateOf(true) }
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldSurface.copy(alpha = 0.85f)),
                            border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_live_call_guide_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showCustomerGuide = !showCustomerGuide },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                            contentDescription = "How to Use Guide",
                                            tint = AccentGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "How to Use Live Audio & Video Call",
                                            color = AccentGold,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showCustomerGuide) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (showCustomerGuide) "Hide Guide" else "Show Guide",
                                        tint = AccentGold
                                    )
                                }

                                AnimatedVisibility(visible = showCustomerGuide) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val customerSteps = listOf(
                                            "1. Pick Mode & Topic" to "Choose Audio Talk or Video Call above, pick a topic (e.g. Order & Payment), and add an optional quick note.",
                                            "2. Connect Live" to "Tap Start Live Audio Call or Start Live Video Call and allow Microphone (plus Camera for video) permission.",
                                            "3. Switch & Chat Mid-Call" to "Tap To Video / To Audio anytime without hanging up. Use Mic, Speaker, Flip Camera, Interactive Chat, or quick prompt buttons.",
                                            "4. Direct Backup" to "If Admin is offline, tap WhatsApp Admin or Direct Dial right above to reach support immediately."
                                        )
                                        customerSteps.forEach { (stepTitle, stepBody) ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = DeepEmerald.copy(alpha = 0.65f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Text(
                                                        text = stepTitle,
                                                        color = Color(0xFF6EE7B7),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = stepBody,
                                                        color = Color.White.copy(alpha = 0.88f),
                                                        fontSize = 11.5.sp,
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // =============================================================
                // ACTIVE LIVE CALL STAGE (SWITCHABLE AUDIO VISUALIZER <-> VIDEO)
                // =============================================================
                Box(modifier = Modifier.fillMaxSize()) {
                    // Persistent WebRTC engine (keeps voice connected seamlessly across Audio <-> Video switches)
                    LiveWebRtcCallSurface(
                        roomId = callState.roomId,
                        isActiveCall = true,
                        isCallerInitiator = callState.isCallerInitiator,
                        mode = callState.mode,
                        isMicMuted = callState.isMicMuted,
                        isSpeakerOn = callState.isSpeakerOn,
                        isUsingFrontCamera = callState.isUsingFrontCamera,
                        inboundSignalJson = inboundSignal,
                        onInboundSignalConsumed = { viewModel.consumeInboundWebRtcSignal() },
                        onOutboundSignal = { sigJson -> viewModel.sendWebRtcOutboundSignal(sigJson) },
                        onConnectionStateChanged = { st -> viewModel.onWebRtcConnectionState(st) },
                        onAudioLevelsChanged = { l, r -> viewModel.onWebRtcAudioLevels(l, r) },
                        onMediaError = { err -> viewModel.onWebRtcMediaError(err) },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Rich Audio-Visual Interactive Stage Overlay when in AUDIO mode (or while waiting for remote video)
                    if (callState.mode == LiveCallMode.AUDIO) {
                        AudioVisualStageOverlay(
                            peerName = callState.peerName,
                            topic = callState.topic,
                            phaseLabel = callState.phase.label,
                            elapsedSeconds = callState.elapsedSeconds,
                            localLevel = callState.localAudioLevel,
                            remoteLevel = callState.remoteAudioLevel,
                            isMicMuted = callState.isMicMuted,
                            statusBanner = callState.statusBanner,
                            onSwitchToVideo = {
                                ensurePermissionsAndRun(requireCamera = true) {
                                    viewModel.switchLiveCallMode(LiveCallMode.VIDEO)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp, start = 16.dp, end = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = DeepEmerald.copy(alpha = 0.88f),
                                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.65f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = AccentGold, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = callState.statusBanner.ifBlank { "Live Video Camera & Interactive Desk Active" },
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                onClick = { showChatOverlay = !showChatOverlay },
                                shape = RoundedCornerShape(999.dp),
                                color = if (showChatOverlay) CyberCyan else EmeraldSurface.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.7f)),
                                modifier = Modifier.testTag("video_mode_chat_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = "Toggle Live Chat", tint = if (showChatOverlay) DeepEmerald else CyberCyan, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = if (showChatOverlay) "Hide Live Chat" else "Interactive Chat (${callState.chatMessages.size})",
                                        color = if (showChatOverlay) DeepEmerald else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Quick-Prompt Strip (Audio + Video live assistant prompts)
                    val interactivePrompts = remember {
                        listOf("Check Order Status", "Store Bank Accounts", "How to Pre-Order", "Catalog Highlights")
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 12.dp, end = 12.dp, bottom = 96.dp)
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("live_interactive_prompt_strip"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        interactivePrompts.forEach { prompt ->
                            Surface(
                                onClick = {
                                    viewModel.sendInteractiveAudioVisualPrompt(prompt)
                                    showChatOverlay = true
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = EmeraldSurface.copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.65f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = AccentGold, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = prompt,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Toggleable Live In-Call Chat & Order Reference Drawer
                    AnimatedVisibility(
                        visible = showChatOverlay,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 12.dp, end = 12.dp, bottom = 140.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = DeepEmerald.copy(alpha = 0.94f)),
                            border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .widthIn(max = 560.dp)
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Live Call Chat & Order Notes",
                                        color = AccentGold,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                    IconButton(
                                        onClick = { showChatOverlay = false },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Chat", tint = Color.White)
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .heightIn(min = 90.dp, max = 145.dp)
                                        .fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (callState.chatMessages.isEmpty()) {
                                        item {
                                            Text(
                                                text = "Send a quick message, order ID, or product note while talking.",
                                                color = Color.White.copy(alpha = 0.65f),
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    } else {
                                        items(callState.chatMessages, key = { it.id }) { m ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (m.isFromAdmin) AccentGold.copy(alpha = 0.2f) else EmeraldSurface,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(
                                                        text = m.senderName,
                                                        color = if (m.isFromAdmin) AccentGold else CyberCyan,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.5.sp
                                                    )
                                                    Text(text = m.text, color = Color.White, fontSize = 12.5.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = chatDraft,
                                        onValueChange = { chatDraft = it },
                                        placeholder = { Text("Type message...", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = AccentGold,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.35f)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    val latestOrder = orders.firstOrNull()
                                    if (latestOrder != null) {
                                        IconButton(
                                            onClick = {
                                                viewModel.sendLiveCallChatMessage(
                                                    text = "Checking Order #${latestOrder.orderId.take(8)} (${latestOrder.status.label})",
                                                    attachedTag = latestOrder.orderId
                                                )
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(EmeraldSurface)
                                        ) {
                                            Icon(Icons.Default.SupportAgent, contentDescription = "Share Latest Order", tint = AccentGold)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            if (chatDraft.isNotBlank()) {
                                                viewModel.sendLiveCallChatMessage(chatDraft)
                                                chatDraft = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SuccessGreen)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Live Control Dock
                    Surface(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = DeepEmerald.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, EmeraldHighlight.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mic Mute
                            CallControlButton(
                                icon = if (callState.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                label = if (callState.isMicMuted) "Muted" else "Mic",
                                containerColor = if (callState.isMicMuted) Color(0xFF991B1B) else EmeraldSurface,
                                contentColor = Color.White,
                                onClick = { viewModel.toggleLiveCallMicMute() },
                                testTag = "mute_mic_button"
                            )

                            // Live Switch Audio <-> Video
                            val switchTarget = if (callState.mode == LiveCallMode.AUDIO) LiveCallMode.VIDEO else LiveCallMode.AUDIO
                            CallControlButton(
                                icon = if (callState.mode == LiveCallMode.VIDEO) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                label = if (callState.mode == LiveCallMode.VIDEO) "To Audio" else "To Video",
                                containerColor = AccentGold,
                                contentColor = DeepEmerald,
                                onClick = {
                                    ensurePermissionsAndRun(requireCamera = switchTarget == LiveCallMode.VIDEO) {
                                        viewModel.switchLiveCallMode(switchTarget)
                                    }
                                },
                                testTag = "dock_switch_mode_button"
                            )

                            // Speakerphone
                            CallControlButton(
                                icon = if (callState.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                label = if (callState.isSpeakerOn) "Speaker" else "Ear",
                                containerColor = if (callState.isSpeakerOn) SuccessGreen else EmeraldSurface,
                                contentColor = Color.White,
                                onClick = { viewModel.toggleLiveCallSpeaker() },
                                testTag = "speaker_toggle_button"
                            )

                            // Flip Camera (or Live Chat when in Audio Mode)
                            if (callState.mode == LiveCallMode.VIDEO) {
                                CallControlButton(
                                    icon = Icons.Default.Cameraswitch,
                                    label = "Flip",
                                    containerColor = EmeraldSurface,
                                    contentColor = Color.White,
                                    onClick = { viewModel.flipLiveCallCamera() },
                                    testTag = "flip_camera_button"
                                )
                            } else {
                                CallControlButton(
                                    icon = Icons.Default.Chat,
                                    label = "Chat",
                                    containerColor = if (showChatOverlay) CyberCyan else EmeraldSurface,
                                    contentColor = if (showChatOverlay) DeepEmerald else Color.White,
                                    onClick = { showChatOverlay = !showChatOverlay },
                                    testTag = "toggle_call_chat_button"
                                )
                            }

                            // End Call
                            CallControlButton(
                                icon = Icons.Default.CallEnd,
                                label = "End",
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White,
                                onClick = {
                                    showChatOverlay = false
                                    viewModel.endLiveCall()
                                },
                                testTag = "end_call_button"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioVisualStageOverlay(
    peerName: String,
    topic: String,
    phaseLabel: String,
    elapsedSeconds: Int,
    localLevel: Int,
    remoteLevel: Int,
    isMicMuted: Boolean,
    statusBanner: String,
    onSwitchToVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioPulse")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val activeAudioEnergy by animateFloatAsState(
        targetValue = (maxOf(localLevel, remoteLevel) / 100f).coerceIn(0f, 1f),
        animationSpec = tween(140),
        label = "audioEnergy"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 24.dp, bottom = 108.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = EmeraldSurface.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, EmeraldHighlight.copy(alpha = 0.55f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "LIVE AUDIO TALK • $phaseLabel",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }
        }

        // Reactive Voice Visualizer Rings + Avatar
        Box(
            modifier = Modifier.size(210.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val baseR = size.minDimension * 0.28f
                val pulseBoost = (activeAudioEnergy * 28f) + (idlePulse * 6f)

                drawCircle(
                    color = EmeraldHighlight.copy(alpha = 0.16f + activeAudioEnergy * 0.25f),
                    radius = baseR + pulseBoost * 1.9f,
                    center = c
                )
                drawCircle(
                    color = AccentGold.copy(alpha = 0.28f + activeAudioEnergy * 0.35f),
                    radius = baseR + pulseBoost * 1.1f,
                    center = c,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Equalizer frequency bars at bottom arc
                val barCount = 14
                val barWidth = 5.dp.toPx()
                val gap = 4.dp.toPx()
                val totalW = barCount * barWidth + (barCount - 1) * gap
                val startX = c.x - totalW / 2f
                val bottomY = size.height - 6.dp.toPx()
                for (i in 0 until barCount) {
                    val wave = kotlin.math.abs(kotlin.math.sin((i + 1) * 0.7f + idlePulse * 3.1f))
                    val barH = (8f + (activeAudioEnergy * 34f + 8f) * wave).dp.toPx().coerceAtMost(40.dp.toPx())
                    drawRoundRect(
                        color = if (i % 2 == 0) AccentGold else EmeraldHighlight,
                        topLeft = Offset(startX + i * (barWidth + gap), bottomY - barH),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = EmeraldSurface,
                border = BorderStroke(2.5.dp, AccentGold),
                modifier = Modifier.size(104.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.SupportAgent,
                        contentDescription = peerName,
                        tint = AccentGold,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = peerName,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = topic,
                color = CyberCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = formatCallDuration(elapsedSeconds),
                color = AccentGold,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
            if (statusBanner.isNotBlank()) {
                Text(
                    text = statusBanner,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        OutlinedButton(
            onClick = onSwitchToVideo,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold),
            border = BorderStroke(1.2.dp, AccentGold),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Switch to Live Video Call", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(containerColor)
                .testTag(testTag)
        ) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatCallDuration(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
