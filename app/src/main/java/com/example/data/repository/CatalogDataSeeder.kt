package com.example.data.repository

import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductColor

object CatalogDataSeeder {

    fun generate100Products(): List<Product> {
        val products = mutableListOf<Product>()

        // 1. CLOTHES (15 items)
        val clothesList = listOf(
            Triple("CyberTech Tactical Hooded Anorak", "Nike Tech", "android.resource://com.example/drawable/img_leather_luxury_jacket"),
            Triple("Oversized Heavyweight Vintage Boxy Tee", "Balenciaga", "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=1200&q=95"),
            Triple("Urban Utility Cargo Jogger Pants", "Off-White", "https://images.unsplash.com/photo-1517445312882-bc9910d016b7?auto=format&fit=crop&w=1200&q=95"),
            Triple("Italian Tailored Slim-Fit Blazer", "Gucci", "https://images.unsplash.com/photo-1507679799987-c73779587ccf?auto=format&fit=crop&w=1200&q=95"),
            Triple("Vintage Denim Trucker Jacket", "Levi's", "https://images.unsplash.com/photo-1576995853123-5a10305d93c0?auto=format&fit=crop&w=1200&q=95"),
            Triple("Minimalist Cashmere Knit Sweater", "Uniqlo", "https://images.unsplash.com/photo-1576566588028-4147f3842f27?auto=format&fit=crop&w=1200&q=95"),
            Triple("Streetwear Graphic Oversized Hoodie", "Fear of God", "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?auto=format&fit=crop&w=1200&q=95"),
            Triple("Athletic Performance Compress Tech Shirt", "Under Armour", "https://images.unsplash.com/photo-1581655353564-df123a1eb820?auto=format&fit=crop&w=1200&q=95"),
            Triple("Luxury Silk Button-Up Cuban Collar Shirt", "Prada", "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?auto=format&fit=crop&w=1200&q=95"),
            Triple("All-Weather Waterproof Puffer Coat", "The North Face", "https://images.unsplash.com/photo-1544441893-675973e31985?auto=format&fit=crop&w=1200&q=95"),
            Triple("Casual Chino Trousers", "Dockers", "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?auto=format&fit=crop&w=1200&q=95"),
            Triple("Designer Pleated Midi Dress", "Zara", "https://images.unsplash.com/photo-1539109136881-3be0616acf4b?auto=format&fit=crop&w=1200&q=95"),
            Triple("Velvet Evening Dinner Jacket", "Tom Ford", "https://images.unsplash.com/photo-1507679799987-c73779587ccf?auto=format&fit=crop&w=1200&q=95"),
            Triple("Tech Fleece Tapered Sweatpants", "Nike Tech", "https://images.unsplash.com/photo-1552902865-b72c031ac5ea?auto=format&fit=crop&w=1200&q=95"),
            Triple("Organic Linen Summer Shorts", "Polo Ralph Lauren", "https://images.unsplash.com/photo-1591195853828-11db59a44f6b?auto=format&fit=crop&w=1200&q=95")
        )
        clothesList.forEachIndexed { i, (name, brand, img) ->
            products.add(
                Product(
                    id = "prod-clothes-${i + 1}",
                    title = name,
                    category = ProductCategory.CLOTHES.displayName,
                    brand = brand,
                    availableBrands = listOf(brand, "Zara", "Nike Tech"),
                    availableSizes = listOf("S", "M", "L", "XL", "XXL"),
                    availableColors = listOf(ProductColor("Midnight Black", "#111827"), ProductColor("Cyber Cyan", "#06B6D4")),
                    basePrice = (15000 + i * 2500).toDouble(),
                    discountPercent = 0,
                    stockQuantity = 20 + (i * 3),
                    imageUrl = img,
                    model3dType = "CLOTH",
                    description = "Premium $name crafted from durable, breathable materials with precision stitching for everyday comfort and high-end style.",
                    rating = 4.5 + (i % 5) * 0.1,
                    reviewCount = 45 + i * 12
                )
            )
        }

        // 2. SHOES (15 items)
        val shoesList = listOf(
            Triple("Quantum Zoom 3D Aerodynamic Sneaker", "Nike", "android.resource://com.example/drawable/img_sneaker_quantum_3d"),
            Triple("Strata Carbon Pro Running Spikes", "Adidas", "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=1200&q=95"),
            Triple("Air Jordan 1 Retro High OG", "Jordan", "https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?auto=format&fit=crop&w=1200&q=95"),
            Triple("Yeezy Boost 350 V2 Onyx", "Yeezy", "https://images.unsplash.com/photo-1588117305388-c2631a279f82?auto=format&fit=crop&w=1200&q=95"),
            Triple("Classic Leather Oxford Dress Shoes", "Clarks", "https://images.unsplash.com/photo-1614252235316-8c857d38b5f4?auto=format&fit=crop&w=1200&q=95"),
            Triple("UltraBoost Light Cushioning Trainers", "Adidas", "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=1200&q=95"),
            Triple("Retro Canvas High-Top Sneakers", "Converse", "https://images.unsplash.com/photo-1607522370275-f14206abe5d3?auto=format&fit=crop&w=1200&q=95"),
            Triple("Chunky Triple S Platform Trainers", "Balenciaga", "https://images.unsplash.com/photo-1560769629-975ec94e6a86?auto=format&fit=crop&w=1200&q=95"),
            Triple("Handcrafted Italian Leather Chelsea Boots", "Saint Laurent", "https://images.unsplash.com/photo-1638247025967-b4e38f787b76?auto=format&fit=crop&w=1200&q=95"),
            Triple("Gel-Kayano Stability Running Shoes", "Asics", "https://images.unsplash.com/photo-1539185441755-769473a23570?auto=format&fit=crop&w=1200&q=95"),
            Triple("Suede Classic Low-Top Kickers", "Puma", "https://images.unsplash.com/photo-1600185365926-3a2ce3cdb9eb?auto=format&fit=crop&w=1200&q=95"),
            Triple("Mountain Trail Waterproof Hiking Boots", "Timberland", "https://images.unsplash.com/photo-1520639888713-7851133b1ed0?auto=format&fit=crop&w=1200&q=95"),
            Triple("Slip-On Memory Foam Walking Loafers", "Skechers", "https://images.unsplash.com/photo-1562183241-b937e95585b6?auto=format&fit=crop&w=1200&q=95"),
            Triple("Speedcross Off-Road Trail Kicks", "Salomon", "https://images.unsplash.com/photo-1543163521-1bf539c55dd2?auto=format&fit=crop&w=1200&q=95"),
            Triple("Hand-Stitched Leather Monk Strap", "Berluti", "https://images.unsplash.com/photo-1614252235316-8c857d38b5f4?auto=format&fit=crop&w=1200&q=95")
        )
        shoesList.forEachIndexed { i, (name, brand, img) ->
            products.add(
                Product(
                    id = "prod-shoes-${i + 1}",
                    title = name,
                    category = ProductCategory.SHOES.displayName,
                    brand = brand,
                    availableBrands = listOf(brand, "Nike", "Adidas"),
                    availableSizes = listOf("EU 38", "EU 39", "EU 40", "EU 41", "EU 42", "EU 43", "EU 44", "EU 45", "EU 46"),
                    availableColors = listOf(ProductColor("Cyber Blue", "#2563EB"), ProductColor("Triple Black", "#000000")),
                    basePrice = (32000 + i * 3500).toDouble(),
                    discountPercent = 0,
                    stockQuantity = 15 + (i * 2),
                    imageUrl = img,
                    model3dType = "SNEAKER",
                    description = "Engineered $name designed for optimal grip, shock absorption, and elevated street performance.",
                    rating = 4.6 + (i % 4) * 0.1,
                    reviewCount = 80 + i * 15
                )
            )
        }

        // 3. BAGS (14 items)
        val bagsList = listOf(
            Triple("AeroSpace Carbon Fiber Executive Backpack", "Tumi", "android.resource://com.example/drawable/img_luxury_leather_bag"),
            Triple("Italian Calfskin Monogram Tote Bag", "Louis Vuitton", "https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=1200&q=95"),
            Triple("Waterproof Commuter Sling Bag", "Peak Design", "https://images.unsplash.com/photo-1622560480605-d83c853bc5c3?auto=format&fit=crop&w=1200&q=95"),
            Triple("Vintage Canvas Duffle Travel Bag", "Herschel", "https://images.unsplash.com/photo-1547949003-9792a18a2601?auto=format&fit=crop&w=1200&q=95"),
            Triple("Luxury Gold-Chain Crossbody Flap Bag", "Chanel", "https://images.unsplash.com/photo-1590874103328-eac38a683ce7?auto=format&fit=crop&w=1200&q=95"),
            Triple("Tactical Utility Chest Rig & Pouch", "Supreme", "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=1200&q=95"),
            Triple("Executive Genuine Leather Messenger Briefcase", "Samsonite", "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?auto=format&fit=crop&w=1200&q=95"),
            Triple("Designer Woven Leather Hobo Bag", "Bottega Veneta", "https://images.unsplash.com/photo-1591561954557-26941169b49e?auto=format&fit=crop&w=1200&q=95"),
            Triple("Anti-Theft TSA Laptop Travel Backpack", "Nomatic", "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=1200&q=95"),
            Triple("Classic Leather Belt Bag & Fanny Pack", "Gucci", "https://images.unsplash.com/photo-1566150905458-1bf1fc113f0d?auto=format&fit=crop&w=1200&q=95"),
            Triple("Minimalist Nylon Drawstring Gym Sack", "Nike", "https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=1200&q=95"),
            Triple("Hard Shell Carry-On Suitcase", "Rimowa", "https://images.unsplash.com/photo-1565026057447-b88e3f29042b?auto=format&fit=crop&w=1200&q=95"),
            Triple("Quilted Velvet Clutch Evening Bag", "YSL", "https://images.unsplash.com/photo-1566150905458-1bf1fc113f0d?auto=format&fit=crop&w=1200&q=95"),
            Triple("Foldable Travel Garment Bag", "Tumi", "https://images.unsplash.com/photo-1547949003-9792a18a2601?auto=format&fit=crop&w=1200&q=95")
        )
        bagsList.forEachIndexed { i, (name, brand, img) ->
            products.add(
                Product(
                    id = "prod-bags-${i + 1}",
                    title = name,
                    category = ProductCategory.BAGS.displayName,
                    brand = brand,
                    availableBrands = listOf(brand, "Tumi", "Gucci"),
                    availableSizes = listOf("Standard", "Medium", "Large"),
                    availableColors = listOf(ProductColor("Onyx Black", "#000000"), ProductColor("Cognac Brown", "#78350F")),
                    basePrice = (28000 + i * 4000).toDouble(),
                    discountPercent = 0,
                    stockQuantity = 12 + (i * 2),
                    imageUrl = img,
                    model3dType = "CYLINDER",
                    description = "Handcrafted $name featuring reinforced stitching, waterproof zippers, and ergonomic padding for effortless carry.",
                    rating = 4.7 + (i % 3) * 0.1,
                    reviewCount = 50 + i * 9
                )
            )
        }

        // 4. BEAUTY PRODUCT (14 items)
        val beautyList = listOf(
            Triple("Radiance Botanical Serum & Hydration Elixir", "Fenty Beauty", "android.resource://com.example/drawable/img_luxury_fragrance_bottle"),
            Triple("Velvet Matte Waterproof Liquid Lipstick Set", "MAC", "https://images.unsplash.com/photo-1586495777744-4413f21062fa?auto=format&fit=crop&w=1200&q=95"),
            Triple("Advanced Repair Night Serum Complex", "Estée Lauder", "https://images.unsplash.com/photo-1608248597263-000799965d13?auto=format&fit=crop&w=1200&q=95"),
            Triple("Eau De Parfum Luxury Signature Spray (100ml)", "Dior Sauvage", "https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?auto=format&fit=crop&w=1200&q=95"),
            Triple("Luminous Silk Flawless Foundation", "Armani Beauty", "https://images.unsplash.com/photo-1599733589046-9b8308b5b50d?auto=format&fit=crop&w=1200&q=95"),
            Triple("Deep Cleanse Exfoliating Facial Wash", "CeraVe", "https://images.unsplash.com/photo-1556228720-195a672e8a03?auto=format&fit=crop&w=1200&q=95"),
            Triple("Vitamin C Glow Boosting Face Oil", "The Ordinary", "https://images.unsplash.com/photo-1608248597263-000799965d13?auto=format&fit=crop&w=1200&q=95"),
            Triple("Nourishing Argan Oil Hair Treatment", "Moroccanoil", "https://images.unsplash.com/photo-1535585209827-a15fcdbc4c2d?auto=format&fit=crop&w=1200&q=95"),
            Triple("Pro Sculpt Eyeshadow Palette (18 Shades)", "Huda Beauty", "https://images.unsplash.com/photo-1512496015851-a90fb38ba796?auto=format&fit=crop&w=1200&q=95"),
            Triple("Hydrating Shea Butter Body Balm", "The Body Shop", "https://images.unsplash.com/photo-1608248597263-000799965d13?auto=format&fit=crop&w=1200&q=95"),
            Triple("Sunscreen SPF 50+ Invisible Fluid", "La Roche-Posay", "https://images.unsplash.com/photo-1556228720-195a672e8a03?auto=format&fit=crop&w=1200&q=95"),
            Triple("Volumizing Waterproof Lash Mascara", "Maybelline", "https://images.unsplash.com/photo-1586495777744-4413f21062fa?auto=format&fit=crop&w=1200&q=95"),
            Triple("Charcoal Pore Refining Clay Mask", "GLAMGLOW", "https://images.unsplash.com/photo-1556228720-195a672e8a03?auto=format&fit=crop&w=1200&q=95"),
            Triple("Rosewater Hydrating Face Mist", "Mario Badescu", "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?auto=format&fit=crop&w=1200&q=95")
        )
        beautyList.forEachIndexed { i, (name, brand, img) ->
            products.add(
                Product(
                    id = "prod-beauty-${i + 1}",
                    title = name,
                    category = ProductCategory.BEAUTY_PRODUCT.displayName,
                    brand = brand,
                    availableBrands = listOf(brand, "MAC", "Fenty"),
                    availableSizes = listOf("50ml", "100ml", "200ml"),
                    availableColors = listOf(ProductColor("Rose Gold", "#F43F5E"), ProductColor("Natural Nude", "#E5E7EB")),
                    basePrice = (12000 + i * 2200).toDouble(),
                    discountPercent = 0,
                    stockQuantity = when (i) { 0 -> 2; 1 -> 1; else -> 25 + (i * 3) },
                    imageUrl = img,
                    model3dType = "SPHERE",
                    description = "Dermatologist-tested $name formulated with organic active ingredients to nourish, illuminate, and enhance your natural glow.",
                    expiryMonthsRemaining = when (i) { 0 -> 0; 1 -> 2; 2 -> 3; else -> 16 + i },
                    rating = 4.8 + (i % 3) * 0.1,
                    reviewCount = 110 + i * 22
                )
            )
        }

        // 5. ELECTRONICS (14 items)
        val electronicsList = listOf(
            Triple("UltraWide Curved Gaming Monitor 4K 144Hz", "Samsung", "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=1200&q=95"),
            Triple("Pro Audio Active Noise-Canceling Headphones", "Sony WH-1000XM5", "android.resource://com.example/drawable/img_cyber_headset_audiophile"),
            Triple("MacBook Pro 16\" M3 Max (36GB RAM)", "Apple", "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=1200&q=95"),
            Triple("Galaxy S24 Ultra 5G (512GB)", "Samsung", "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=1200&q=95"),
            Triple("Smart OLED TV 65\" 120Hz Dolby Vision", "LG", "https://images.unsplash.com/photo-1593784991095-87728734ec08?auto=format&fit=crop&w=1200&q=95"),
            Triple("Wireless Mechanical RGB Keyboard", "Logitech", "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=1200&q=95"),
            Triple("High-Precision Optical Gaming Mouse", "Razer", "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=1200&q=95"),
            Triple("Portable Bluetooth Boombox Speaker", "JBL", "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?auto=format&fit=crop&w=1200&q=95"),
            Triple("4K Mirrorless Cinema Camera 24-70mm Lens", "Sony Alpha", "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=1200&q=95"),
            Triple("Smart Watch Ultra Fitness & Heart Monitor", "Apple Watch", "https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=1200&q=95"),
            Triple("High-Speed Wi-Fi 6E Mesh Router System", "TP-Link", "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?auto=format&fit=crop&w=1200&q=95"),
            Triple("Portable Fast Charging Power Bank 25,000mAh", "Anker", "https://images.unsplash.com/photo-1609592424109-dd9892f1b177?auto=format&fit=crop&w=1200&q=95"),
            Triple("Ergonomic Standing Desk Dual-Motor", "Fully", "https://images.unsplash.com/photo-1595515106969-1ce29566ff1c?auto=format&fit=crop&w=1200&q=95"),
            Triple("Studio Condenser USB Microphone", "Blue Yeti", "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?auto=format&fit=crop&w=1200&q=95")
        )
        electronicsList.forEachIndexed { i, (name, brand, img) ->
            products.add(
                Product(
                    id = "prod-electronics-${i + 1}",
                    title = name,
                    category = ProductCategory.ELECTRONICS.displayName,
                    brand = brand,
                    availableBrands = listOf(brand, "Apple", "Samsung"),
                    availableSizes = listOf("Standard", "128GB", "256GB", "512GB"),
                    availableColors = listOf(ProductColor("Space Gray", "#374151"), ProductColor("Silver White", "#F3F4F6")),
                    basePrice = (45000 + i * 15000).toDouble(),
                    discountPercent = 0,
                    stockQuantity = 10 + (i * 2),
                    imageUrl = img,
                    model3dType = "CYLINDER",
                    description = "Cutting-edge $name with ultra-low latency, energy-efficient performance, and official manufacturer warranty.",
                    rating = 4.9,
                    reviewCount = 140 + i * 30
                )
            )
        }

        // 6. FOOD (14 items)
        val foodList = listOf(
            Triple("Organic Jumbo Cashew Nuts (500g)", "Nutty Bites", "android.resource://com.example/drawable/img_gourmet_espresso_can"),
            Triple("Artisanal Sourdough Whole Grain Bread", "Bakery Fresh", "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=1200&q=95"),
            Triple("Premium Basmati Long-Grain Rice (5kg)", "Royal Harvest", "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=1200&q=95"),
            Triple("Single-Origin Dark Chocolate Bar 85%", "Lindt", "https://images.unsplash.com/photo-1549007994-cb92caebd54b?auto=format&fit=crop&w=1200&q=95"),
            Triple("Wildflower Honey Unfiltered Pure (1kg)", "Bee Pure", "https://images.unsplash.com/photo-1587049352846-4a222e784d38?auto=format&fit=crop&w=1200&q=95"),
            Triple("Extra Virgin Cold-Pressed Olive Oil (1L)", "Filippo Berio", "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=1200&q=95"),
            Triple("Organic Roasted Almond Butter (400g)", "Whole Foods", "https://images.unsplash.com/photo-1509358271058-acd05cc93898?auto=format&fit=crop&w=1200&q=95"),
            Triple("Gourmet Cheddar Cheese Wheel (500g)", "Kerrygold", "https://images.unsplash.com/photo-1452195100486-9cc805987862?auto=format&fit=crop&w=1200&q=95"),
            Triple("Smoked Norwegian Salmon Fillet Slices", "Ocean Fresh", "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?auto=format&fit=crop&w=1200&q=95"),
            Triple("Gluten-Free Oats & Berry Granola Cereal", "Quaker", "https://images.unsplash.com/photo-1517093725432-a9a754313169?auto=format&fit=crop&w=1200&q=95"),
            Triple("Traditional Italian Penne Rigate Pasta", "Barilla", "https://images.unsplash.com/photo-1621996346565-e3def616403c?auto=format&fit=crop&w=1200&q=95"),
            Triple("Roasted Pistachios Salted & Shelled (250g)", "Wonderful", "https://images.unsplash.com/photo-1509358271058-acd05cc93898?auto=format&fit=crop&w=1200&q=95"),
            Triple("Organic Quinoa Grain Superfood (1kg)", "Nature's Path", "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=1200&q=95"),
            Triple("Sun-Dried Tomato & Basil Pesto Spread", "Classico", "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=1200&q=95")
        )
        foodList.forEachIndexed { i, (name, brand, img) ->
            products.add(
                Product(
                    id = "prod-food-${i + 1}",
                    title = name,
                    category = ProductCategory.FOOD.displayName,
                    brand = brand,
                    availableBrands = listOf(brand, "Nestlé", "Organic"),
                    availableSizes = listOf("250g", "500g", "1kg"),
                    availableColors = listOf(ProductColor("Golden", "#F59E0B")),
                    basePrice = (3500 + i * 850).toDouble(),
                    discountPercent = 0,
                    stockQuantity = when (i) { 0 -> 2; 1 -> 1; 2 -> 2; else -> 40 + (i * 3) },
                    imageUrl = img,
                    model3dType = "CUBE",
                    description = "Freshly packed $name sourced from certified organic farms for rich taste and wholesome nutrition.",
                    expiryMonthsRemaining = when (i) { 0 -> 0; 1 -> 1; 2 -> 3; else -> 14 + (i % 6) },
                    rating = 4.7,
                    reviewCount = 60 + i * 10
                )
            )
        }

        // 7. BEVERAGES (14 items)
        val beveragesList = listOf(
            Triple("Arabica Dark Roast Ground Coffee (500g)", "Starbucks", "android.resource://com.example/drawable/img_gourmet_espresso_can"),
            Triple("Sparkling Natural Mineral Water (12 Pack)", "San Pellegrino", "https://images.unsplash.com/photo-1527661591475-527312dd65f5?auto=format&fit=crop&w=1200&q=95"),
            Triple("Matcha Green Tea Ceremonial Grade Powder", "Encha", "https://images.unsplash.com/photo-1576092768241-dec231879fc3?auto=format&fit=crop&w=1200&q=95"),
            Triple("Cold-Pressed Organic Orange Juice (1L)", "Simply Orange", "https://images.unsplash.com/photo-1613478223719-2ab802602423?auto=format&fit=crop&w=1200&q=95"),
            Triple("Crafted Ginger Ale Soda Cans (24 Pack)", "Fever-Tree", "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&w=1200&q=95"),
            Triple("Isotonic Electrolyte Sports Drink (6 Pack)", "Gatorade", "https://images.unsplash.com/photo-1543253687-c931c8e01820?auto=format&fit=crop&w=1200&q=95"),
            Triple("Organic Coconut Water Hydration (1L)", "Vita Coco", "https://images.unsplash.com/photo-1527661591475-527312dd65f5?auto=format&fit=crop&w=1200&q=95"),
            Triple("Chai Spiced Black Tea Bags (100 Count)", "Twinings", "https://images.unsplash.com/photo-1576092768241-dec231879fc3?auto=format&fit=crop&w=1200&q=95"),
            Triple("Zero Sugar Energy Boost Drink (12 Pack)", "Monster", "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&w=1200&q=95"),
            Triple("Unsweetened Almond Milk (1L)", "Alpro", "https://images.unsplash.com/photo-1550583724-b2692b85b150?auto=format&fit=crop&w=1200&q=95"),
            Triple("Hibiscus & Berry Iced Herbal Brew", "Teavana", "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=1200&q=95"),
            Triple("Sparkling Lemonade Soda (6 Pack)", "Sprite", "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&w=1200&q=95"),
            Triple("Probiotic Kombucha Ginger Lemon", "GT's", "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=1200&q=95"),
            Triple("Alkaline Ionized Spring Water (1L)", "Essentia", "https://images.unsplash.com/photo-1527661591475-527312dd65f5?auto=format&fit=crop&w=1200&q=95")
        )
        beveragesList.forEachIndexed { i, (name, brand, img) ->
            products.add(
                Product(
                    id = "prod-beverages-${i + 1}",
                    title = name,
                    category = ProductCategory.BEVERAGES.displayName,
                    brand = brand,
                    availableBrands = listOf(brand, "Starbucks", "Coca-Cola"),
                    availableSizes = listOf("330ml", "500ml", "1L"),
                    availableColors = listOf(ProductColor("Amber Gold", "#F59E0B")),
                    basePrice = (2500 + i * 750).toDouble(),
                    discountPercent = 0,
                    stockQuantity = 40 + (i * 4),
                    imageUrl = img,
                    model3dType = "CYLINDER",
                    description = "Refreshing $name crafted for instant refreshment, rich aroma, and premium taste.",
                    expiryMonthsRemaining = 18 - (i % 8),
                    rating = 4.8,
                    reviewCount = 95 + i * 14
                )
            )
        }

        // 8..23 NEW EXPANDED STOREFRONT CATEGORIES (48 items across 16 categories)
        data class ExpandedSeedSpec(
            val category: ProductCategory,
            val idPrefix: String,
            val model3d: String,
            val sizes: List<String>,
            val baseStart: Int,
            val items: List<Triple<String, String, String>>
        )

        val expandedSpecs = listOf(
            ExpandedSeedSpec(
                category = ProductCategory.MEN,
                idPrefix = "men",
                model3d = "CLOTH",
                sizes = listOf("M", "L", "XL", "XXL"),
                baseStart = 24000,
                items = listOf(
                    Triple("Men's Italian Wool Senator Kaftan Set", "Hugo Boss", "android.resource://com.example/drawable/img_leather_luxury_jacket"),
                    Triple("Men's Sovereign Automatic Dress Watch", "Rolex Homage", "android.resource://com.example/drawable/img_jewelry_gold_watch"),
                    Triple("Men's Executive Genuine Leather Loafers", "Clarks", "android.resource://com.example/drawable/img_sneaker_quantum_3d")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.WOMEN,
                idPrefix = "women",
                model3d = "COSMETIC",
                sizes = listOf("S", "M", "L", "XL"),
                baseStart = 22000,
                items = listOf(
                    Triple("Women's Silk Evening Maxi Gown", "Zara Woman", "android.resource://com.example/drawable/img_luxury_fragrance_bottle"),
                    Triple("Women's Designer Quilted Leather Handbag", "Coach", "android.resource://com.example/drawable/img_luxury_leather_bag"),
                    Triple("Women's Crystal Emerald Halo Ring Set", "Swarovski", "android.resource://com.example/drawable/img_jewelry_gold_watch")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.BABIES,
                idPrefix = "babies",
                model3d = "BOX",
                sizes = listOf("0-3M", "3-6M", "6-12M", "12-18M"),
                baseStart = 9500,
                items = listOf(
                    Triple("Organic Cotton Pastel Baby Romper & Plush Set", "Little Sprouts", "android.resource://com.example/drawable/img_babies_kids_set"),
                    Triple("Newborn Soft Fleece Swaddle Blanket 3-Pack", "Carter's", "android.resource://com.example/drawable/img_babies_kids_set"),
                    Triple("Anti-Colic Silicone Feeding Bottle Starter Kit", "Philips Avent", "android.resource://com.example/drawable/img_babies_kids_set")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.KIDS,
                idPrefix = "kids",
                model3d = "SNEAKER",
                sizes = listOf("EU 24", "EU 26", "EU 28", "EU 30", "EU 32"),
                baseStart = 12500,
                items = listOf(
                    Triple("Kids Pastel Breathable Canvas Sneakers", "TinyStep", "android.resource://com.example/drawable/img_babies_kids_set"),
                    Triple("Kids Ergonomic School Backpack & Lunch Box", "Nike Kids", "android.resource://com.example/drawable/img_luxury_leather_bag"),
                    Triple("Kids Cotton Playtime Tracksuit Set", "Adidas Kids", "android.resource://com.example/drawable/img_babies_kids_set")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.HOUSEHOLD,
                idPrefix = "household",
                model3d = "BOX",
                sizes = listOf("Compact", "Standard", "Family Size"),
                baseStart = 18500,
                items = listOf(
                    Triple("Heavy-Duty Stackable Home Organizer Bins (4-Pack)", "HomePro", "android.resource://com.example/drawable/img_household_upholstery"),
                    Triple("Stainless Steel Thermal Vacuum Flask Set (2L)", "Thermos", "android.resource://com.example/drawable/img_gourmet_espresso_can"),
                    Triple("Smart HEPA Room Air Purifier & Diffuser", "Philips Home", "android.resource://com.example/drawable/img_cyber_headset_audiophile")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.MUSICALS,
                idPrefix = "musicals",
                model3d = "CUBE",
                sizes = listOf("Studio", "Stage Pro"),
                baseStart = 65000,
                items = listOf(
                    Triple("Acoustic Dreadnought Rosewood Guitar + Stand", "Yamaha", "android.resource://com.example/drawable/img_household_upholstery"),
                    Triple("Studio Closed-Back Monitoring Headphones", "Audio-Technica", "android.resource://com.example/drawable/img_cyber_headset_audiophile"),
                    Triple("61-Key Touch-Sensitive Arranger Keyboard", "Casio", "android.resource://com.example/drawable/img_household_upholstery")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.EDUCATIONAL,
                idPrefix = "educational",
                model3d = "BOX",
                sizes = listOf("Primary", "Secondary", "Tertiary"),
                baseStart = 11000,
                items = listOf(
                    Triple("Executive Hardcover Academic Study Compendium & Pen", "Oxford Press", "android.resource://com.example/drawable/img_auto_tools_tech"),
                    Triple("STEM Robotics & Electronics Learning Kit", "Arduino Edu", "android.resource://com.example/drawable/img_cyber_headset_audiophile"),
                    Triple("Interactive Multilingual Learning Tablet 10-inch", "LeapFrog Pro", "android.resource://com.example/drawable/img_babies_kids_set")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.JEWELRY,
                idPrefix = "jewelry",
                model3d = "COSMETIC",
                sizes = listOf("18cm", "20cm", "Adjustable"),
                baseStart = 85000,
                items = listOf(
                    Triple("24K Gold-Plated Chronograph Watch & Emerald Ring", "Geneva Luxe", "android.resource://com.example/drawable/img_jewelry_gold_watch"),
                    Triple("Tennis Cut White Zirconia Link Bracelet", "Cartier Inspired", "android.resource://com.example/drawable/img_jewelry_gold_watch"),
                    Triple("Emerald Solitaire 18K Gold Engagement Ring", "Tiffany Style", "android.resource://com.example/drawable/img_jewelry_gold_watch")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.STATIONARIES,
                idPrefix = "stationaries",
                model3d = "BOX",
                sizes = listOf("A4 Pack", "A5 Executive"),
                baseStart = 7500,
                items = listOf(
                    Triple("Genuine Leather A5 Journal & Fountain Pen Set", "Parker Luxe", "android.resource://com.example/drawable/img_auto_tools_tech"),
                    Triple("80gsm Bright White A4 Copier Paper Ream (500 Sheets)", "Double A", "android.resource://com.example/drawable/img_auto_tools_tech"),
                    Triple("Architect Metal Drafting & Gel Pen Studio Box", "Staedtler", "android.resource://com.example/drawable/img_auto_tools_tech")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.PHONE_ACCESSORIES,
                idPrefix = "phone-acc",
                model3d = "PHONE",
                sizes = listOf("USB-C", "Lightning", "Universal"),
                baseStart = 14000,
                items = listOf(
                    Triple("20,000mAh 65W PD Matte Fast Power Bank", "Anker", "android.resource://com.example/drawable/img_auto_tools_tech"),
                    Triple("MagSafe Shockproof Kevlar Phone Case + Glass", "Spigen", "android.resource://com.example/drawable/img_cyber_headset_audiophile"),
                    Triple("3-in-1 Fast Wireless Charging Dock Station", "Belkin", "android.resource://com.example/drawable/img_auto_tools_tech")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.UPHOLSTERY,
                idPrefix = "upholstery",
                model3d = "BOX",
                sizes = listOf("Single Seater", "3-Seater Roll", "2 Yards"),
                baseStart = 48000,
                items = listOf(
                    Triple("Royal Emerald Velvet Tufted Accent Armchair", "Maison Decor", "android.resource://com.example/drawable/img_household_upholstery"),
                    Triple("Heavy-Duty Italian Boucle Sofa Upholstery Fabric", "Milano Textile", "android.resource://com.example/drawable/img_household_upholstery"),
                    Triple("High-Density Memory Foam Sofa Cushion Inserts (4x)", "OrthoComfort", "android.resource://com.example/drawable/img_household_upholstery")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.RUBBER_PLASTIC,
                idPrefix = "rubber-plastic",
                model3d = "CYLINDER",
                sizes = listOf("25 Litres", "45 Litres", "Heavy Pack"),
                baseStart = 8500,
                items = listOf(
                    Triple("Industrial Lock-Lid Storage Crates (4-Piece Set)", "DuraPlast", "android.resource://com.example/drawable/img_household_upholstery"),
                    Triple("Heavy-Duty Non-Slip Vulcanized Rubber Floor Mat", "FlexGrip", "android.resource://com.example/drawable/img_auto_tools_tech"),
                    Triple("Food-Grade Airtight Polypropylene Drum (50L)", "EcoPoly", "android.resource://com.example/drawable/img_household_upholstery")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.AUTOMOBILE_SPARE_PARTS,
                idPrefix = "auto-parts",
                model3d = "CUBE",
                sizes = listOf("OEM Standard", "Heavy Duty"),
                baseStart = 28500,
                items = listOf(
                    Triple("Cross-Drilled Vented Ceramic Front Brake Rotor Set", "Brembo OEM", "android.resource://com.example/drawable/img_auto_tools_tech"),
                    Triple("Laser Iridium Twin-Tip Spark Plugs (4-Pack)", "NGK Japan", "android.resource://com.example/drawable/img_auto_tools_tech"),
                    Triple("Synthetic 5W-30 Engine Oil Filter & Gasket Kit", "Bosch Auto", "android.resource://com.example/drawable/img_auto_tools_tech")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.CLOTHING_SEWING_MATERIALS,
                idPrefix = "sewing",
                model3d = "CUBE",
                sizes = listOf("6 Yards", "Master Kit"),
                baseStart = 12000,
                items = listOf(
                    Triple("Master Tailor Canvas Kit: Needles, Brass Tape & Shears", "Singer Pro", "android.resource://com.example/drawable/img_auto_tools_tech"),
                    Triple("Polished Swiss Voile Lace Fabric Bundle (5 Yards)", "Vlisco Heritage", "android.resource://com.example/drawable/img_leather_luxury_jacket"),
                    Triple("High-Tensile 24-Spool Embroidery Thread Pack", "Gutermann", "android.resource://com.example/drawable/img_auto_tools_tech")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.HEALTH_MEDICAL_EQUIPMENT,
                idPrefix = "medical-eq",
                model3d = "BOX",
                sizes = listOf("Clinical Adult", "Home Care"),
                baseStart = 32000,
                items = listOf(
                    Triple("Digital Upper-Arm Blood Pressure Monitor + LCD", "Omron Clinical", "android.resource://com.example/drawable/img_medical_pharma_kit"),
                    Triple("Dual-Head Acoustic Cardiology Stethoscope", "Littmann Class", "android.resource://com.example/drawable/img_medical_pharma_kit"),
                    Triple("Infrared Non-Contact Forehead Thermometer + Oximeter", "Beurer Med", "android.resource://com.example/drawable/img_medical_pharma_kit")
                )
            ),
            ExpandedSeedSpec(
                category = ProductCategory.PHARMACEUTICALS,
                idPrefix = "pharma",
                model3d = "BOTTLE",
                sizes = listOf("60 Capsules", "100 Tablets", "200ml Syrup"),
                baseStart = 6500,
                items = listOf(
                    Triple("Complete Multivitamin & Omega-3 Softgel Pharmacy Pack", "Emzor Pharma", "android.resource://com.example/drawable/img_medical_pharma_kit"),
                    Triple("Sterile Antiseptic First-Aid Amber Bottle & Dressing Kit", "May & Baker", "android.resource://com.example/drawable/img_medical_pharma_kit"),
                    Triple("Vitamin C 1000mg + Zinc Immune Effervescent Tablets", "Fidson Care", "android.resource://com.example/drawable/img_medical_pharma_kit")
                )
            )
        )

        expandedSpecs.forEach { spec ->
            spec.items.forEachIndexed { idx, (name, brand, img) ->
                products.add(
                    Product(
                        id = "prod-${spec.idPrefix}-${idx + 1}",
                        title = name,
                        category = spec.category.displayName,
                        brand = brand,
                        availableBrands = listOf(brand, "24/7 Select", "Premium OEM"),
                        availableSizes = spec.sizes,
                        availableColors = listOf(
                            ProductColor("Signature Finish", "#064E3B"),
                            ProductColor("Classic Onyx", "#111827")
                        ),
                        basePrice = (spec.baseStart + idx * 4500).toDouble(),
                        discountPercent = if (idx == 0) 5 else 0,
                        stockQuantity = 24 + idx * 8,
                        imageUrl = img,
                        model3dType = spec.model3d,
                        description = "Verified quality $name under ${spec.category.displayName}. Ready for instant dispatch from 24/7 SHOPPING.",
                        expiryMonthsRemaining = if (spec.category == ProductCategory.PHARMACEUTICALS) 18 else 24,
                        rating = 4.7 + (idx % 3) * 0.1,
                        reviewCount = 38 + idx * 11
                    )
                )
            }
        }

        return products
    }
}
