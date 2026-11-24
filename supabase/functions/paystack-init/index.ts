// Supabase Edge Function: paystack-init
// Deploy: supabase functions deploy paystack-init --no-verify-jwt
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
    const orderId = String(body.order_id ?? body.metadata?.order_id ?? "").trim();
    const reference = String(body.reference ?? "").trim();
    const email = String(body.email ?? "").trim();
    const callbackUrl = String(body.callback_url ?? "depros://paystack-callback").trim();
    const metadata = typeof body.metadata === "object" && body.metadata !== null ? body.metadata : {};

    const amountKobo = Number(body.amount ?? 0);
    if (!Number.isFinite(amountKobo) || amountKobo < 100) {
      return new Response(
        JSON.stringify({ status: false, message: "Invalid payment amount (minimum 100 kobo / ₦1)." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }
    if (!email || !email.includes("@")) {
      return new Response(
        JSON.stringify({ status: false, message: "Valid customer receipt email is required." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const paystackRes = await fetch("https://api.paystack.co/transaction/initialize", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${paystackSecret}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        amount: Math.round(amountKobo),
        email,
        currency: "NGN",
        ...(reference ? { reference } : {}),
        callback_url: callbackUrl,
        metadata: {
          ...metadata,
          order_id: orderId,
          store_brand: "Odiya",
        },
      }),
    });

    const paystackJson = await paystackRes.json();

    // Record initialized payment session in public.paystack_transactions using service_role
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (paystackRes.ok && paystackJson?.status && supabaseUrl && serviceRoleKey) {
      try {
        const supabase = createClient(supabaseUrl, serviceRoleKey);
        const resolvedRef = paystackJson.data?.reference || reference;
        if (resolvedRef) {
          await supabase.from("paystack_transactions").upsert(
            {
              reference: resolvedRef,
              order_id: orderId || resolvedRef,
              customer_phone: String(metadata.customer_phone ?? ""),
              customer_name: String(metadata.customer_name ?? ""),
              amount_kobo: Math.round(amountKobo),
              amount_naira: Math.round(amountKobo) / 100,
              currency: "NGN",
              status: "initialized",
              authorization_url: String(paystackJson.data?.authorization_url ?? ""),
              verified_by: "paystack_init_edge",
            },
            { onConflict: "reference" },
          );
        }
      } catch (dbErr) {
        console.warn("Non-fatal paystack_transactions pre-log error:", dbErr);
      }
    }

    return new Response(JSON.stringify(paystackJson), {
      status: paystackRes.status,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    return new Response(
      JSON.stringify({
        status: false,
        message: err instanceof Error ? err.message : "Failed to initialize Paystack transaction",
      }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }
});
