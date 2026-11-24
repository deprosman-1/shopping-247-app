// Supabase Edge Function: paystack-verify
// Deploy: supabase functions deploy paystack-verify --no-verify-jwt
// Secret required: supabase secrets set PAYSTACK_SECRET_KEY=sk_live_your_secret_key

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({ status: false, message: "Method not allowed. Use POST." }),
      { status: 405, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }

  const paystackSecret = Deno.env.get("PAYSTACK_SECRET_KEY")?.trim();
  if (!paystackSecret) {
    return new Response(
      JSON.stringify({
        status: false,
        message: "PAYSTACK_SECRET_KEY is not configured in Supabase Edge Function Secrets.",
      }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }

  try {
    const body = await req.json();
    const reference = String(body.reference ?? "").trim();
    if (!reference) {
      return new Response(
        JSON.stringify({ status: false, message: "Missing transaction reference." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const verifyRes = await fetch(
      `https://api.paystack.co/transaction/verify/${encodeURIComponent(reference)}`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${paystackSecret}`,
          "Content-Type": "application/json",
        },
      },
    );

    const verifyJson = await verifyRes.json();
    const txData = verifyJson?.data;
    const txStatus = String(txData?.status ?? "").toLowerCase();

    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

    if (verifyRes.ok && txData && supabaseUrl && serviceRoleKey) {
      const supabase = createClient(supabaseUrl, serviceRoleKey);
      const amountKobo = Number(txData.amount ?? 0);
      const orderId = String(txData.metadata?.order_id ?? "");
      const channel = String(txData.channel ?? "paystack");

      await supabase.from("paystack_transactions").upsert(
        {
          reference,
          order_id: orderId || reference,
          customer_phone: String(txData.metadata?.customer_phone ?? ""),
          customer_name: String(txData.metadata?.customer_name ?? ""),
          amount_kobo: Math.round(amountKobo),
          amount_naira: Math.round(amountKobo) / 100,
          currency: String(txData.currency ?? "NGN"),
          channel,
          status: txStatus || "pending",
          paid_at: txData.paid_at ?? (txStatus === "success" ? new Date().toISOString() : null),
          verified_by: "paystack_verify_edge",
        },
        { onConflict: "reference" },
      );

      if (txStatus === "success" && orderId) {
        await supabase
          .from("orders")
          .update({
            status: "CONFIRMED",
            payment_reference: reference,
            verification_note: `Paystack verified (${channel}) • Ref ${reference}`,
          })
          .eq("order_id", orderId);
      }
    }

    return new Response(JSON.stringify(verifyJson), {
      status: verifyRes.status,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    return new Response(
      JSON.stringify({
        status: false,
        message: err instanceof Error ? err.message : "Failed to verify Paystack transaction",
      }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }
});
