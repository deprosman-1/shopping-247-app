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
      const amountKobo = Math.round(Number(txData.amount ?? 0));
      const orderId = String(txData.metadata?.order_id ?? "");
      const purpose = String(txData.metadata?.purpose ?? "ORDER_CHECKOUT").toUpperCase();
      const channel = String(txData.channel ?? "paystack");
      const splitMode = String(txData.metadata?.split_mode ?? "STANDARD");
      const partnerId = String(txData.metadata?.partner_id ?? "");
      const partnerName = String(txData.metadata?.partner_name ?? "");
      const subaccountCode = String(txData.subaccount?.subaccount_code ?? txData.metadata?.subaccount_code ?? "");
      const splitCode = String(txData.split?.split_code ?? txData.metadata?.split_code ?? "");
      const odiyaShareKobo = Math.round(Number(txData.metadata?.odiya_share_kobo ?? amountKobo));
      const odiyaCommissionKobo = Math.round(Number(txData.metadata?.odiya_commission_kobo ?? 0));
      const hostingFeeKobo = Math.round(Number(txData.metadata?.hosting_fee_kobo ?? 0));
      const partnerNetKobo = Math.round(Number(txData.metadata?.partner_net_kobo ?? 0));

      await supabase.from("paystack_transactions").upsert(
        {
          reference,
          order_id: orderId || reference,
          customer_phone: String(txData.metadata?.customer_phone ?? ""),
          customer_name: String(txData.metadata?.customer_name ?? ""),
          amount_kobo: amountKobo,
          amount_naira: amountKobo / 100,
          currency: String(txData.currency ?? "NGN"),
          channel,
          status: txStatus || "pending",
          split_mode: splitMode,
          partner_id: partnerId,
          partner_name: partnerName,
          subaccount_code: subaccountCode,
          split_code: splitCode,
          odiya_share_kobo: odiyaShareKobo,
          odiya_commission_kobo: odiyaCommissionKobo,
          hosting_fee_kobo: hostingFeeKobo,
          partner_net_kobo: partnerNetKobo,
          paid_at: txData.paid_at ?? (txStatus === "success" ? new Date().toISOString() : null),
          verified_by: "paystack_verify_edge",
        },
        { onConflict: "reference" },
      );

      if (txStatus === "success" && orderId && purpose !== "PARTNER_HOSTING_FEE") {
        const splitTag = partnerNetKobo > 0
          ? ` • Split: ODIYA ₦${Math.round(odiyaShareKobo / 100).toLocaleString("en-NG")} / Partner ₦${Math.round(partnerNetKobo / 100).toLocaleString("en-NG")}`
          : "";
        await supabase
          .from("orders")
          .update({
            status: "CONFIRMED",
            payment_reference: reference,
            verification_note: `Paystack verified (${channel}) • Ref ${reference}${splitTag}`,
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
