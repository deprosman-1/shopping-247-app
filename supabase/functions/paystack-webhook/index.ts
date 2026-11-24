// Supabase Edge Function: paystack-webhook
// Deploy: supabase functions deploy paystack-webhook --no-verify-jwt
// Secret required: supabase secrets set PAYSTACK_SECRET_KEY=sk_live_your_secret_key
// Set Webhook URL in Paystack Dashboard -> Settings -> API Keys & Webhooks:
// https://<YOUR_PROJECT_REF>.supabase.co/functions/v1/paystack-webhook

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

function toHex(buffer: ArrayBuffer): string {
  return Array.from(new Uint8Array(buffer))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

function timingSafeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let mismatch = 0;
  for (let i = 0; i < a.length; i++) {
    mismatch |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return mismatch === 0;
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  const paystackSecret = Deno.env.get("PAYSTACK_SECRET_KEY")?.trim();
  if (!paystackSecret) {
    return new Response("PAYSTACK_SECRET_KEY missing", { status: 500 });
  }

  const signature = req.headers.get("x-paystack-signature")?.trim().toLowerCase() ?? "";
  if (!signature) {
    return new Response("Missing x-paystack-signature header", { status: 401 });
  }

  const rawBody = await req.text();

  // Verify HMAC-SHA512 signature using PAYSTACK_SECRET_KEY
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(paystackSecret),
    { name: "HMAC", hash: "SHA-512" },
    false,
    ["sign"],
  );
  const digest = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(rawBody));
  const expectedSig = toHex(digest).toLowerCase();

  if (!timingSafeEqual(expectedSig, signature)) {
    return new Response("Invalid Paystack webhook signature", { status: 401 });
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) {
    return new Response("Supabase service credentials unavailable", { status: 500 });
  }

  try {
    const event = JSON.parse(rawBody);
    const eventName = String(event?.event ?? "");
    const data = event?.data ?? {};
    const reference = String(data?.reference ?? "").trim();

    if (!reference) {
      return new Response(JSON.stringify({ received: true, skipped: "no reference" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }

    const supabase = createClient(supabaseUrl, serviceRoleKey);
    const amountKobo = Number(data?.amount ?? 0);
    const amountNaira = Math.round(amountKobo) / 100;
    const channel = String(data?.channel ?? "paystack");
    const status = String(data?.status ?? (eventName === "charge.success" ? "success" : "pending")).toLowerCase();

    let orderId = String(data?.metadata?.order_id ?? "").trim();
    if (!orderId) {
      const { data: existingTx } = await supabase
        .from("paystack_transactions")
        .select("order_id")
        .eq("reference", reference)
        .maybeSingle();
      orderId = String(existingTx?.order_id ?? "");
    }

    await supabase.from("paystack_transactions").upsert(
      {
        reference,
        order_id: orderId || reference,
        customer_phone: String(data?.metadata?.customer_phone ?? data?.customer?.phone ?? ""),
        customer_name: String(data?.metadata?.customer_name ?? ""),
        amount_kobo: Math.round(amountKobo),
        amount_naira: amountNaira,
        currency: String(data?.currency ?? "NGN"),
        channel,
        status: eventName === "charge.success" ? "success" : status,
        paid_at: data?.paid_at ?? new Date().toISOString(),
        verified_by: "paystack_webhook",
      },
      { onConflict: "reference" },
    );

    if (eventName === "charge.success" && orderId) {
      const nowMs = Date.now();
      const { data: existingOrder } = await supabase
        .from("orders")
        .select("status_updates_json")
        .eq("order_id", orderId)
        .maybeSingle();

      const existingUpdates = String(existingOrder?.status_updates_json ?? "").trim();
      const note = `Paystack webhook verified (${channel}) • ₦${amountNaira.toLocaleString("en-NG")} • Ref ${reference}`;
      const newEntry = `CONFIRMED|${nowMs}|${note}`;
      const nextUpdates = existingUpdates
        ? (existingUpdates.includes(reference) ? existingUpdates : `${existingUpdates};${newEntry}`)
        : newEntry;

      await supabase
        .from("orders")
        .update({
          status: "CONFIRMED",
          payment_reference: reference,
          verification_note: note,
          status_updates_json: nextUpdates,
        })
        .eq("order_id", orderId);
    }

    return new Response(JSON.stringify({ received: true, event: eventName, reference, order_id: orderId }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  } catch (err) {
    return new Response(
      JSON.stringify({ error: err instanceof Error ? err.message : "Webhook processing error" }),
      { status: 500, headers: { "Content-Type": "application/json" } },
    );
  }
});
