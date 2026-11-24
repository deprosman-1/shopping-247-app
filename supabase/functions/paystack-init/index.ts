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

    const amountKobo = Math.round(Number(body.amount ?? 0));
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

    const requestedSplitMode = String(body.split_mode ?? metadata.split_mode ?? "STANDARD").trim().toUpperCase();
    const purpose = String(body.purpose ?? metadata.purpose ?? "ORDER_CHECKOUT").trim().toUpperCase();
    const rawBearer = String(body.bearer ?? "account").trim().toLowerCase();
    const bearer = rawBearer === "subaccount" ? "subaccount" : "account";

    const rawAllocations = Array.isArray(body.split_allocations) ? body.split_allocations : [];
    const validAllocations = rawAllocations
      .map((a: Record<string, unknown>) => ({
        partner_id: String(a?.partner_id ?? "").trim(),
        partner_name: String(a?.partner_name ?? "").trim(),
        subaccount: String(a?.subaccount ?? a?.subaccount_code ?? "").trim(),
        partner_net_kobo: Math.round(Number(a?.partner_net_kobo ?? 0)),
        commission_kobo: Math.round(Number(a?.commission_kobo ?? 0)),
        hosting_offset_kobo: Math.round(Number(a?.hosting_offset_kobo ?? 0)),
      }))
      .filter((a: { subaccount: string; partner_net_kobo: number }) =>
        a.subaccount.startsWith("ACCT_") && a.partner_net_kobo >= 100
      );

    const directSubaccount = String(body.subaccount ?? "").trim();
    const odiyaCommissionKobo = Math.max(0, Math.round(Number(body.odiya_commission_kobo ?? metadata.odiya_commission_kobo ?? 0)));
    const hostingFeeKobo = Math.max(0, Math.round(Number(body.hosting_fee_kobo ?? metadata.hosting_fee_kobo ?? 0)));

    let resolvedSplitMode = "STANDARD";
    let resolvedSubaccount = "";
    let resolvedSplitCode = "";
    let resolvedOdiyaShareKobo = amountKobo;
    let resolvedPartnerNetKobo = 0;

    const initPayload: Record<string, unknown> = {
      amount: amountKobo,
      email,
      currency: "NGN",
      ...(reference ? { reference } : {}),
      callback_url: callbackUrl,
    };

    if (purpose === "PARTNER_HOSTING_FEE" || requestedSplitMode === "HOSTING_FEE_100_ODIYA") {
      // Monthly Hosting Fee settles 100% directly into ODIYA App's main Paystack settlement account
      resolvedSplitMode = "HOSTING_FEE_100_ODIYA";
      resolvedOdiyaShareKobo = amountKobo;
      resolvedPartnerNetKobo = 0;
    } else if (validAllocations.length >= 2) {
      // Multi-Partner Dynamic Flat Split via Paystack /split API
      const totalPartnersNet = validAllocations.reduce((sum: number, a: { partner_net_kobo: number }) => sum + a.partner_net_kobo, 0);
      const safePartnerTotal = Math.min(totalPartnersNet, Math.max(0, amountKobo - 100));
      const ratio = totalPartnersNet > safePartnerTotal && totalPartnersNet > 0 ? safePartnerTotal / totalPartnersNet : 1;
      const splitSubaccounts = validAllocations.map((a: { subaccount: string; partner_net_kobo: number }) => ({
        subaccount: a.subaccount,
        share: Math.max(100, Math.floor(a.partner_net_kobo * ratio)),
      }));
      const splitCreateRes = await fetch("https://api.paystack.co/split", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${paystackSecret}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          name: `ODIYA-${orderId || reference || Date.now()}`.slice(0, 60),
          type: "flat",
          currency: "NGN",
          subaccounts: splitSubaccounts,
          bearer_type: "account",
        }),
      });
      const splitCreateJson = await splitCreateRes.json();
      const createdSplitCode = String(splitCreateJson?.data?.split_code ?? "").trim();
      if (splitCreateRes.ok && createdSplitCode) {
        initPayload.split_code = createdSplitCode;
        resolvedSplitCode = createdSplitCode;
        resolvedSplitMode = "PARTNER_MULTI_SPLIT";
        resolvedPartnerNetKobo = splitSubaccounts.reduce((s: number, x: { share: number }) => s + x.share, 0);
        resolvedOdiyaShareKobo = Math.max(0, amountKobo - resolvedPartnerNetKobo);
      } else {
        // Fallback to first partner subaccount if /split failed
        const first = validAllocations[0];
        const charge = Math.max(0, amountKobo - first.partner_net_kobo);
        if (charge < amountKobo) {
          initPayload.subaccount = first.subaccount;
          if (charge > 0) initPayload.transaction_charge = charge;
          initPayload.bearer = bearer;
          resolvedSubaccount = first.subaccount;
          resolvedSplitMode = "PARTNER_SUBACCOUNT";
          resolvedOdiyaShareKobo = charge;
          resolvedPartnerNetKobo = amountKobo - charge;
        }
      }
    } else if (validAllocations.length === 1 || directSubaccount.startsWith("ACCT_")) {
      const targetSub = validAllocations.length === 1 ? validAllocations[0].subaccount : directSubaccount;
      const partnerNetFromBody = validAllocations.length === 1
        ? validAllocations[0].partner_net_kobo
        : Math.max(0, amountKobo - Math.round(Number(body.transaction_charge ?? body.odiya_share_kobo ?? 0)));
      const safePartnerNet = Math.min(Math.max(0, partnerNetFromBody), amountKobo);
      const odiyaChargeKobo = Math.max(0, amountKobo - safePartnerNet);

      if (safePartnerNet >= 100 && odiyaChargeKobo < amountKobo) {
        initPayload.subaccount = targetSub;
        if (odiyaChargeKobo > 0) {
          initPayload.transaction_charge = odiyaChargeKobo;
        }
        initPayload.bearer = bearer;
        resolvedSubaccount = targetSub;
        resolvedSplitMode = "PARTNER_SUBACCOUNT";
        resolvedOdiyaShareKobo = odiyaChargeKobo;
        resolvedPartnerNetKobo = safePartnerNet;
      } else {
        // 100% kept by ODIYA (e.g., due hosting fee equals full cart)
        resolvedSplitMode = "HOSTING_OFFSET_100_ODIYA";
        resolvedOdiyaShareKobo = amountKobo;
        resolvedPartnerNetKobo = 0;
      }
    }

    const partnerId = String(body.partner_id ?? metadata.partner_id ?? validAllocations[0]?.partner_id ?? "").trim();
    const partnerName = String(body.partner_name ?? metadata.partner_name ?? validAllocations[0]?.partner_name ?? "").trim();
    const splitBreakdownJson = validAllocations.length > 0 ? JSON.stringify(validAllocations) : "";

    initPayload.metadata = {
      ...metadata,
      order_id: orderId,
      purpose,
      split_mode: resolvedSplitMode,
      partner_id: partnerId,
      partner_name: partnerName,
      subaccount_code: resolvedSubaccount,
      split_code: resolvedSplitCode,
      odiya_share_kobo: resolvedOdiyaShareKobo,
      odiya_commission_kobo: odiyaCommissionKobo,
      hosting_fee_kobo: hostingFeeKobo,
      partner_net_kobo: resolvedPartnerNetKobo,
      store_brand: "Odiya",
    };

    const paystackRes = await fetch("https://api.paystack.co/transaction/initialize", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${paystackSecret}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(initPayload),
    });

    const paystackJson = await paystackRes.json();

    // Record initialized payment session + split breakdown in public.paystack_transactions using service_role
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
              amount_kobo: amountKobo,
              amount_naira: amountKobo / 100,
              currency: "NGN",
              status: "initialized",
              authorization_url: String(paystackJson.data?.authorization_url ?? ""),
              split_mode: resolvedSplitMode,
              partner_id: partnerId,
              partner_name: partnerName,
              subaccount_code: resolvedSubaccount,
              split_code: resolvedSplitCode,
              odiya_share_kobo: resolvedOdiyaShareKobo,
              odiya_commission_kobo: odiyaCommissionKobo,
              hosting_fee_kobo: hostingFeeKobo,
              partner_net_kobo: resolvedPartnerNetKobo,
              split_breakdown_json: splitBreakdownJson,
              verified_by: "paystack_init_edge",
            },
            { onConflict: "reference" },
          );
        }
      } catch (dbErr) {
        console.warn("Non-fatal paystack_transactions pre-log error:", dbErr);
      }
    }

    return new Response(
      JSON.stringify({
        ...paystackJson,
        split_summary: {
          split_mode: resolvedSplitMode,
          subaccount_code: resolvedSubaccount,
          split_code: resolvedSplitCode,
          odiya_share_kobo: resolvedOdiyaShareKobo,
          odiya_commission_kobo: odiyaCommissionKobo,
          hosting_fee_kobo: hostingFeeKobo,
          partner_net_kobo: resolvedPartnerNetKobo,
        },
      }),
      {
        status: paystackRes.status,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      },
    );
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
