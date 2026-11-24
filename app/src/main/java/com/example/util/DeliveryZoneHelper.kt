package com.example.util

import com.example.data.model.DeliveryZoneOption

object DeliveryZoneHelper {
    val zones: List<DeliveryZoneOption> = listOf(
        DeliveryZoneOption(
            id = "zone_pickup",
            zoneName = "In-Store Pickup (Odiya Hub)",
            statesSummary = "Walk-in / Rider self-pickup",
            fee = 0.0,
            etaLabel = "Ready in 1–2 Hours"
        ),
        DeliveryZoneOption(
            id = "zone_lagos_mainland",
            zoneName = "Lagos Mainland Same-Day",
            statesSummary = "Ikeja, Yaba, Surulere, Festac, Oshodi, Agege",
            fee = 2500.0,
            etaLabel = "Same-Day (2–6 Hours)"
        ),
        DeliveryZoneOption(
            id = "zone_lagos_island",
            zoneName = "Lagos Island & Lekki Corridor",
            statesSummary = "VI, Ikoyi, Lekki, Ajah, Chevron",
            fee = 3000.0,
            etaLabel = "Same-Day (3–8 Hours)"
        ),
        DeliveryZoneOption(
            id = "zone_southwest",
            zoneName = "South-West Express",
            statesSummary = "Ogun, Oyo (Ibadan), Osun, Ondo, Ekiti, Kwara",
            fee = 3500.0,
            etaLabel = "24–48 Hours"
        ),
        DeliveryZoneOption(
            id = "zone_abuja",
            zoneName = "Abuja FCT Express",
            statesSummary = "Wuse, Maitama, Garki, Gwarinpa, Kubwa, Lugbe",
            fee = 4500.0,
            etaLabel = "24 Hours Next-Day Air/Van"
        ),
        DeliveryZoneOption(
            id = "zone_south_east_south",
            zoneName = "South-South & South-East Hub",
            statesSummary = "Rivers (PH), Delta, Edo, Anambra, Enugu, Abia, Imo, Akwa Ibom",
            fee = 5000.0,
            etaLabel = "24–48 Hours"
        ),
        DeliveryZoneOption(
            id = "zone_north",
            zoneName = "Northern Region Express",
            statesSummary = "Kano, Kaduna, Plateau (Jos), Bauchi, Borno, Niger, Benue",
            fee = 6000.0,
            etaLabel = "48–72 Hours"
        ),
        DeliveryZoneOption(
            id = "zone_intl",
            zoneName = "International DHL / FedEx Air",
            statesSummary = "UK, US, Canada, Europe & Global Dispatch",
            fee = 25000.0,
            etaLabel = "3–5 Business Days"
        )
    )

    fun findByName(name: String?): DeliveryZoneOption {
        if (name.isNullOrBlank()) return zones[1]
        return zones.find {
            it.zoneName.equals(name, ignoreCase = true) ||
                it.id.equals(name, ignoreCase = true) ||
                it.statesSummary.contains(name, ignoreCase = true)
        } ?: zones[1]
    }
}
