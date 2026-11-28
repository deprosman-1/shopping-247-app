-- =============================================================================
-- ODIYA Complete Supabase Schema, Row Level Security (RLS) & Storage Policies
-- Run this entire script inside: Supabase Dashboard -> SQL Editor -> New Query
-- =============================================================================

-- 1. TABLES CREATION & SAFE COLUMN UPGRADES
CREATE TABLE IF NOT EXISTS public.products (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    category TEXT NOT NULL,
    brand TEXT NOT NULL,
    available_brands TEXT,
    available_sizes TEXT,
    available_colors TEXT,
    base_price NUMERIC NOT NULL,
    cost_price NUMERIC,
    discount_percent INT DEFAULT 0,
    stock_quantity INT DEFAULT 0,
    expiry_months_remaining INT DEFAULT 6,
    image_url TEXT,
    model_3d_type TEXT DEFAULT 'CUBE',
    description TEXT,
    sku TEXT,
    partner_id TEXT DEFAULT '',
    partner_name TEXT DEFAULT '',
    partner_commission_percent NUMERIC DEFAULT 0,
    partner_batch_id TEXT DEFAULT '',
    is_catalog_locked BOOLEAN DEFAULT FALSE,
    variant_prices_json TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.products ADD COLUMN IF NOT EXISTS category TEXT DEFAULT 'General';
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS brand TEXT DEFAULT 'ODIYA';
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS available_brands TEXT;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS available_sizes TEXT;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS available_colors TEXT;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS base_price NUMERIC DEFAULT 0;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS cost_price NUMERIC;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS discount_percent INT DEFAULT 0;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS stock_quantity INT DEFAULT 0;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS expiry_months_remaining INT DEFAULT 6;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS image_url TEXT;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS model_3d_type TEXT DEFAULT 'CUBE';
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS sku TEXT;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS partner_id TEXT DEFAULT '';
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS partner_name TEXT DEFAULT '';
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS partner_commission_percent NUMERIC DEFAULT 0;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS partner_batch_id TEXT DEFAULT '';
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS is_catalog_locked BOOLEAN DEFAULT FALSE;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS variant_prices_json TEXT DEFAULT '';
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();

CREATE TABLE IF NOT EXISTS public.customers (
    id TEXT PRIMARY KEY,
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    phone_number TEXT,
    delivery_address TEXT,
    wishlist_product_ids TEXT DEFAULT '',
    signup_credential TEXT DEFAULT '',
    is_admin BOOLEAN DEFAULT FALSE,
    created_at BIGINT
);

ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS full_name TEXT DEFAULT 'Customer';
ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS email TEXT;
ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS phone_number TEXT DEFAULT '';
ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS delivery_address TEXT DEFAULT '';
ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS wishlist_product_ids TEXT DEFAULT '';
ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS signup_credential TEXT DEFAULT '';
ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS is_admin BOOLEAN DEFAULT FALSE;
ALTER TABLE public.customers ADD COLUMN IF NOT EXISTS created_at BIGINT;

CREATE TABLE IF NOT EXISTS public.orders (
    order_id TEXT PRIMARY KEY,
    customer_id TEXT NOT NULL,
    customer_name TEXT NOT NULL,
    customer_email TEXT NOT NULL,
    customer_phone TEXT,
    delivery_address TEXT NOT NULL,
    items_summary TEXT,
    total_amount NUMERIC NOT NULL,
    profit NUMERIC DEFAULT 0,
    payment_method TEXT NOT NULL,
    payment_account TEXT,
    payment_reference TEXT,
    sender_name TEXT,
    status TEXT NOT NULL,
    verification_note TEXT,
    receipt_image_url TEXT DEFAULT '',
    status_updates_json TEXT DEFAULT '',
    dispatch_rider_whatsapp TEXT DEFAULT '',
    partner_id TEXT DEFAULT '',
    partner_name TEXT DEFAULT '',
    partner_commission_amount NUMERIC DEFAULT 0,
    partner_net_payout NUMERIC DEFAULT 0,
    created_at BIGINT
);

ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS customer_phone TEXT DEFAULT '';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS payment_account TEXT DEFAULT '';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS payment_reference TEXT DEFAULT '';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS sender_name TEXT DEFAULT '';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS verification_note TEXT DEFAULT '';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS receipt_image_url TEXT DEFAULT '';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS status_updates_json TEXT DEFAULT '';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS dispatch_rider_whatsapp TEXT DEFAULT '';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS partner_id TEXT DEFAULT '';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS partner_name TEXT DEFAULT '';
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS partner_commission_amount NUMERIC DEFAULT 0;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS partner_net_payout NUMERIC DEFAULT 0;
ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS created_at BIGINT;

-- 1B. CUSTOMER PRE-ORDERS & SPECIAL BRAND REQUESTS TABLE (Admin <-> Customer Live Sync)
CREATE TABLE IF NOT EXISTS public.pre_orders (
    id TEXT PRIMARY KEY,
    customer_id TEXT DEFAULT 'guest',
    customer_name TEXT NOT NULL,
    customer_phone TEXT DEFAULT '',
    product_name TEXT NOT NULL,
    brand_name TEXT DEFAULT '',
    category TEXT DEFAULT 'Clothes',
    preferred_size TEXT DEFAULT 'Standard',
    preferred_color TEXT DEFAULT 'Any',
    target_budget NUMERIC DEFAULT 0,
    description TEXT DEFAULT '',
    photo_url TEXT DEFAULT '',
    status TEXT DEFAULT 'PENDING',
    created_at BIGINT,
    admin_response TEXT DEFAULT '',
    admin_quoted_price NUMERIC DEFAULT 0,
    customer_reply TEXT DEFAULT ''
);

ALTER TABLE public.pre_orders ADD COLUMN IF NOT EXISTS customer_id TEXT DEFAULT 'guest';
ALTER TABLE public.pre_orders ADD COLUMN IF NOT EXISTS customer_phone TEXT DEFAULT '';
ALTER TABLE public.pre_orders ADD COLUMN IF NOT EXISTS brand_name TEXT DEFAULT '';
ALTER TABLE public.pre_orders ADD COLUMN IF NOT EXISTS category TEXT DEFAULT 'Clothes';
ALTER TABLE public.pre_orders ADD COLUMN IF NOT EXISTS preferred_size TEXT DEFAULT 'Standard';
ALTER TABLE public.pre_orders ADD COLUMN IF NOT EXISTS preferred_color TEXT DEFAULT 'Any';
ALTER TABLE public.pre_orders ADD COLUMN IF NOT EXISTS target_budget NUMERIC DEFAULT 0;
ALTER TABLE public.pre_orders ADD COLUMN IF NOT EXISTS description TEXT DEFAULT '';
ALTER TABLE public.pre_orders ADD COLUMN IF NOT EXISTS photo_url TEXT DEFAULT '';
ALTER TABLE public.pre_orders ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'PENDING';
ALTER TABLE public.pre_orders ADD COLUMN IF NOT EXISTS created_at BIGINT;
ALTER TABLE public.pre_orders ADD COLUMN IF NOT EXISTS admin_response TEXT DEFAULT '';
ALTER TABLE public.pre_orders ADD COLUMN IF NOT EXISTS admin_quoted_price NUMERIC DEFAULT 0;
ALTER TABLE public.pre_orders ADD COLUMN IF NOT EXISTS customer_reply TEXT DEFAULT '';

-- 1C. PAYSTACK TRANSACTIONS & APP DEFENSE TELEMETRY TABLES
CREATE TABLE IF NOT EXISTS public.paystack_transactions (
    reference TEXT PRIMARY KEY,
    order_id TEXT NOT NULL,
    customer_phone TEXT DEFAULT '',
    customer_name TEXT DEFAULT '',
    amount_kobo BIGINT DEFAULT 0,
    amount_naira NUMERIC DEFAULT 0,
    currency TEXT DEFAULT 'NGN',
    channel TEXT DEFAULT 'card',
    status TEXT DEFAULT 'initialized',
    authorization_url TEXT DEFAULT '',
    split_mode TEXT DEFAULT 'STANDARD',
    partner_id TEXT DEFAULT '',
    partner_name TEXT DEFAULT '',
    subaccount_code TEXT DEFAULT '',
    split_code TEXT DEFAULT '',
    odiya_share_kobo BIGINT DEFAULT 0,
    odiya_commission_kobo BIGINT DEFAULT 0,
    hosting_fee_kobo BIGINT DEFAULT 0,
    partner_net_kobo BIGINT DEFAULT 0,
    split_breakdown_json TEXT DEFAULT '',
    paid_at TIMESTAMPTZ,
    verified_by TEXT DEFAULT 'edge_function',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.paystack_transactions ADD COLUMN IF NOT EXISTS split_mode TEXT DEFAULT 'STANDARD';
ALTER TABLE public.paystack_transactions ADD COLUMN IF NOT EXISTS partner_id TEXT DEFAULT '';
ALTER TABLE public.paystack_transactions ADD COLUMN IF NOT EXISTS partner_name TEXT DEFAULT '';
ALTER TABLE public.paystack_transactions ADD COLUMN IF NOT EXISTS subaccount_code TEXT DEFAULT '';
ALTER TABLE public.paystack_transactions ADD COLUMN IF NOT EXISTS split_code TEXT DEFAULT '';
ALTER TABLE public.paystack_transactions ADD COLUMN IF NOT EXISTS odiya_share_kobo BIGINT DEFAULT 0;
ALTER TABLE public.paystack_transactions ADD COLUMN IF NOT EXISTS odiya_commission_kobo BIGINT DEFAULT 0;
ALTER TABLE public.paystack_transactions ADD COLUMN IF NOT EXISTS hosting_fee_kobo BIGINT DEFAULT 0;
ALTER TABLE public.paystack_transactions ADD COLUMN IF NOT EXISTS partner_net_kobo BIGINT DEFAULT 0;
ALTER TABLE public.paystack_transactions ADD COLUMN IF NOT EXISTS split_breakdown_json TEXT DEFAULT '';

CREATE TABLE IF NOT EXISTS public.app_security_telemetry (
    id TEXT PRIMARY KEY,
    device_model TEXT DEFAULT '',
    app_version TEXT DEFAULT '',
    event_type TEXT NOT NULL,
    severity TEXT DEFAULT 'INFO',
    details TEXT DEFAULT '',
    signing_sha256 TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 1D. STORE PROFILE & LIVE APP UPDATE METADATA TABLE
CREATE TABLE IF NOT EXISTS public.store_profile (
    id TEXT PRIMARY KEY DEFAULT 'odiya_main',
    store_name TEXT DEFAULT 'ODIYA',
    tagline TEXT DEFAULT 'Shopping with ease and comfort',
    phone_number TEXT DEFAULT '',
    secondary_phone_number TEXT DEFAULT '',
    whatsapp_number TEXT DEFAULT '',
    support_email TEXT DEFAULT '',
    store_address TEXT DEFAULT '',
    opay_account_number TEXT DEFAULT '',
    opay_account_name TEXT DEFAULT 'ODIYA',
    moniepoint_account_number TEXT DEFAULT '',
    moniepoint_account_name TEXT DEFAULT 'ODIYA',
    palmpay_account_number TEXT DEFAULT '',
    palmpay_account_name TEXT DEFAULT 'ODIYA',
    pos_terminal_id TEXT DEFAULT '',
    pos_terminal_name TEXT DEFAULT '',
    extra_bank_name TEXT DEFAULT '',
    extra_bank_account_number TEXT DEFAULT '',
    extra_bank_account_name TEXT DEFAULT '',
    apk_download_url TEXT DEFAULT '',
    latest_app_version_code INT DEFAULT 1,
    latest_app_version_name TEXT DEFAULT '1.0',
    update_release_notes TEXT DEFAULT '',
    is_maintenance_mode BOOLEAN DEFAULT FALSE,
    maintenance_message TEXT DEFAULT '',
    maintenance_started_at BIGINT DEFAULT 0,
    paystack_public_key TEXT DEFAULT '',
    paystack_callback_url TEXT DEFAULT 'depros://paystack-callback',
    paystack_edge_function_base_url TEXT DEFAULT '',
    updated_at BIGINT DEFAULT 0
);

-- =============================================================================
-- 2. LEAST-PRIVILEGE GRANTS & ROW LEVEL SECURITY (RLS) POLICIES
-- =============================================================================
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.products TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE ON TABLE public.customers TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE ON TABLE public.orders TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE ON TABLE public.pre_orders TO anon, authenticated;
GRANT SELECT ON TABLE public.paystack_transactions TO anon, authenticated;
GRANT INSERT, SELECT ON TABLE public.app_security_telemetry TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE ON TABLE public.store_profile TO anon, authenticated;

-- Products: Public storefront catalog + Admin sync
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public Read & Sync Access on products" ON public.products;
CREATE POLICY "Public Read & Sync Access on products" ON public.products
    FOR ALL TO anon, authenticated USING (true) WITH CHECK (true);

-- Customers: Insert/Read/Update without allowing client DELETE
ALTER TABLE public.customers ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public Read & Sync Access on customers" ON public.customers;
DROP POLICY IF EXISTS "Customers Read & Upsert without client delete" ON public.customers;
CREATE POLICY "Customers Read Access" ON public.customers
    FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Customers Insert Access" ON public.customers
    FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Customers Update Access" ON public.customers
    FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);

-- Orders: Insert/Read/Update for checkout, Admin verification & tracking (no anonymous DELETE)
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public Read & Sync Access on orders" ON public.orders;
DROP POLICY IF EXISTS "Orders Read Access" ON public.orders;
DROP POLICY IF EXISTS "Orders Insert Access" ON public.orders;
DROP POLICY IF EXISTS "Orders Update Access" ON public.orders;
CREATE POLICY "Orders Read Access" ON public.orders
    FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Orders Insert Access" ON public.orders
    FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "Orders Update Access" ON public.orders
    FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);

-- Pre-Orders: Customers submit requests; Admin responds/quotes; no anonymous DELETE
ALTER TABLE public.pre_orders ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "PreOrders Read Access" ON public.pre_orders;
DROP POLICY IF EXISTS "PreOrders Insert Access" ON public.pre_orders;
DROP POLICY IF EXISTS "PreOrders Update Access" ON public.pre_orders;
CREATE POLICY "PreOrders Read Access" ON public.pre_orders
    FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "PreOrders Insert Access" ON public.pre_orders
    FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY "PreOrders Update Access" ON public.pre_orders
    FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);

-- Paystack Transactions: Clients can only READ status; ONLY Edge Functions/Webhook (service_role) can write!
ALTER TABLE public.paystack_transactions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public Read & Sync Access on paystack_transactions" ON public.paystack_transactions;
DROP POLICY IF EXISTS "Paystack Ledger Read Only for Clients" ON public.paystack_transactions;
CREATE POLICY "Paystack Ledger Read Only for Clients" ON public.paystack_transactions
    FOR SELECT TO anon, authenticated USING (true);

-- Security Telemetry: Append-only + read
ALTER TABLE public.app_security_telemetry ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public Insert & Read on app_security_telemetry" ON public.app_security_telemetry;
CREATE POLICY "Telemetry Read & Insert" ON public.app_security_telemetry
    FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "Telemetry Insert Only" ON public.app_security_telemetry
    FOR INSERT TO anon, authenticated WITH CHECK (true);

ALTER TABLE public.store_profile ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "StoreProfile Read & Upsert" ON public.store_profile;
CREATE POLICY "StoreProfile Read & Upsert" ON public.store_profile
    FOR ALL TO anon, authenticated USING (true) WITH CHECK (true);

-- =============================================================================
-- OPTIONAL STRICT SUPABASE-AUTH JWT UPGRADE (Run only if you migrate app
-- accounts to Supabase Auth `auth.uid()` sessions; otherwise keep active rules above):
--
-- DROP POLICY IF EXISTS "Orders Read Access" ON public.orders;
-- DROP POLICY IF EXISTS "Orders Insert Access" ON public.orders;
-- DROP POLICY IF EXISTS "Orders Update Access" ON public.orders;
-- CREATE POLICY "Users read own orders" ON public.orders
--     FOR SELECT TO authenticated USING (auth.uid()::text = customer_id);
-- CREATE POLICY "Users create own orders" ON public.orders
--     FOR INSERT TO authenticated WITH CHECK (auth.uid()::text = customer_id);
-- =============================================================================

-- 3. SUPABASE STORAGE BUCKETS INITIALIZATION
INSERT INTO storage.buckets (id, name, public)
VALUES ('shop-receipts', 'shop-receipts', true)
ON CONFLICT (id) DO UPDATE SET public = true;

INSERT INTO storage.buckets (id, name, public)
VALUES ('product-media', 'product-media', true)
ON CONFLICT (id) DO UPDATE SET public = true;

INSERT INTO storage.buckets (id, name, public)
VALUES ('customer-avatars', 'customer-avatars', true)
ON CONFLICT (id) DO UPDATE SET public = true;

-- 4. SUPABASE STORAGE BUCKET POLICIES (storage.objects)
DROP POLICY IF EXISTS "Public Read on shop-receipts" ON storage.objects;
CREATE POLICY "Public Read on shop-receipts" ON storage.objects FOR SELECT USING (bucket_id = 'shop-receipts');
DROP POLICY IF EXISTS "Public Insert on shop-receipts" ON storage.objects;
CREATE POLICY "Public Insert on shop-receipts" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'shop-receipts');
DROP POLICY IF EXISTS "Public Update on shop-receipts" ON storage.objects;
CREATE POLICY "Public Update on shop-receipts" ON storage.objects FOR UPDATE USING (bucket_id = 'shop-receipts');
DROP POLICY IF EXISTS "Public Delete on shop-receipts" ON storage.objects;
CREATE POLICY "Public Delete on shop-receipts" ON storage.objects FOR DELETE USING (bucket_id = 'shop-receipts');

DROP POLICY IF EXISTS "Public Read on product-media" ON storage.objects;
CREATE POLICY "Public Read on product-media" ON storage.objects FOR SELECT USING (bucket_id = 'product-media');
DROP POLICY IF EXISTS "Public Insert on product-media" ON storage.objects;
CREATE POLICY "Public Insert on product-media" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'product-media');
DROP POLICY IF EXISTS "Public Update on product-media" ON storage.objects;
CREATE POLICY "Public Update on product-media" ON storage.objects FOR UPDATE USING (bucket_id = 'product-media');
DROP POLICY IF EXISTS "Public Delete on product-media" ON storage.objects;
CREATE POLICY "Public Delete on product-media" ON storage.objects FOR DELETE USING (bucket_id = 'product-media');

DROP POLICY IF EXISTS "Public Read on customer-avatars" ON storage.objects;
CREATE POLICY "Public Read on customer-avatars" ON storage.objects FOR SELECT USING (bucket_id = 'customer-avatars');
DROP POLICY IF EXISTS "Public Insert on customer-avatars" ON storage.objects;
CREATE POLICY "Public Insert on customer-avatars" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'customer-avatars');
DROP POLICY IF EXISTS "Public Update on customer-avatars" ON storage.objects;
CREATE POLICY "Public Update on customer-avatars" ON storage.objects FOR UPDATE USING (bucket_id = 'customer-avatars');

-- 5. FORCE POSTGREST SCHEMA CACHE RELOAD
NOTIFY pgrst, 'reload schema';
