// Supabase Edge Function: paystack-subaccount
// Deploy: supabase functions deploy paystack-subaccount --no-verify-jwt
// Secret required: supabase secrets set PAYSTACK_SECRET_KEY=sk_live_your_secret_key

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
    const action = String(body.action ?? "create").trim().toLowerCase();

    if (action === "list_banks") {
      const bankRes = await fetch("https://api.paystack.co/bank?country=nigeria&perPage=100", {
        method: "GET",
        headers: {
          Authorization: `Bearer ${paystackSecret}`,
          "Content-Type": "application/json",
        },
      });
      const bankJson = await bankRes.json();
      return new Response(JSON.stringify(bankJson), {
        status: bankRes.status,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const businessName = String(body.business_name ?? "").trim();
    const bankCode = String(body.bank_code ?? body.settlement_bank ?? "").trim();
    const accountNumber = String(body.account_number ?? "").replace(/\D/g, "");
    const existingCode = String(body.subaccount_code ?? "").trim();
    const rawPct = Number(body.percentage_charge ?? 10);
    const percentageCharge = Number.isFinite(rawPct) ? Math.min(Math.max(rawPct, 0.5), 95) : 10;
    const primaryEmail = String(body.primary_contact_email ?? "").trim();
    const primaryPhone = String(body.primary_contact_phone ?? "").trim();
    const partnerId = String(body.partner_id ?? "").trim();

    if (!businessName || !bankCode || accountNumber.length < 10) {
      return new Response(
        JSON.stringify({
          status: false,
          message: "business_name, settlement bank_code, and 10-digit account_number are required.",
        }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const endpoint = existingCode.startsWith("ACCT_")
      ? `https://api.paystack.co/subaccount/${encodeURIComponent(existingCode)}`
      : "https://api.paystack.co/subaccount";
    const method = existingCode.startsWith("ACCT_") ? "PUT" : "POST";

    const paystackRes = await fetch(endpoint, {
      method,
      headers: {
        Authorization: `Bearer ${paystackSecret}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        business_name: businessName,
        settlement_bank: bankCode,
        account_number: accountNumber,
        percentage_charge: percentageCharge,
        description: `Odiya Partner Subaccount${partnerId ? ` (${partnerId})` : ""}`,
        ...(primaryEmail && primaryEmail.includes("@") ? { primary_contact_email: primaryEmail } : {}),
        ...(primaryPhone ? { primary_contact_phone: primaryPhone } : {}),
      }),
    });

    const paystackJson = await paystackRes.json();
    return new Response(JSON.stringify(paystackJson), {
      status: paystackRes.status,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err) {
    return new Response(
      JSON.stringify({
        status: false,
        message: err instanceof Error ? err.message : "Failed to register Paystack subaccount",
      }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }
});
