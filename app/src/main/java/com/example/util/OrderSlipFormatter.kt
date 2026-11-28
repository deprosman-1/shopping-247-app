package com.example.util

import com.example.data.model.Order
import java.text.NumberFormat
import java.util.Locale

data class SlipLineItem(
    val titleAndSpecs: String,
    val unitPriceText: String,
    val lineTotalText: String
)

data class ParsedOrderSlipDetails(
    val items: List<SlipLineItem>,
    val cleanDeliveryAddress: String,
    val deliveryConditionAndFee: String
)

object OrderSlipFormatter {

    private val soldRegex = Regex("""\s*\[Sold:[^\]]+\]""")
    private val deliveryBracketRegex = Regex("""\s*\[Delivery:\s*([^\]]+)\]\s*""")
    private val richLineRegex = Regex("""^(.*?)\s*@\s*(₦[\d,]+(?:\.\d+)?)\s*each\s*=\s*(₦[\d,]+(?:\.\d+)?)$""")
    private val legacyLineRegex = Regex("""^(.*?)\s*[-–]\s*₦\s*([\d,]+(?:\.\d+)?)$""")

    fun parseOrderSlipDetails(order: Order): ParsedOrderSlipDetails {
        val naira = NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }

        val cleanSummary = order.itemsSummary.replace(soldRegex, "").trim()
        val rawLines = cleanSummary
            .split(";", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val items = rawLines.map { line ->
            val richMatch = richLineRegex.matchEntire(line)
            if (richMatch != null) {
                SlipLineItem(
                    titleAndSpecs = richMatch.groupValues[1].trim(),
                    unitPriceText = richMatch.groupValues[2].trim(),
                    lineTotalText = richMatch.groupValues[3].trim()
                )
            } else {
                val legacyMatch = legacyLineRegex.matchEntire(line)
                if (legacyMatch != null) {
                    val rawNum = legacyMatch.groupValues[2].replace(",", "").toDoubleOrNull()
                    val formattedPrice = if (rawNum != null) naira.format(rawNum) else "₦${legacyMatch.groupValues[2]}"
                    SlipLineItem(
                        titleAndSpecs = legacyMatch.groupValues[1].trim(),
                        unitPriceText = "",
                        lineTotalText = formattedPrice
                    )
                } else {
                    SlipLineItem(
                        titleAndSpecs = line,
                        unitPriceText = "",
                        lineTotalText = ""
                    )
                }
            }
        }

        val delivMatch = deliveryBracketRegex.find(order.deliveryAddress)
        val deliveryCondition = delivMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val cleanAddress = order.deliveryAddress.replace(deliveryBracketRegex, "").trim()
            .ifBlank { order.deliveryAddress }

        return ParsedOrderSlipDetails(
            items = items,
            cleanDeliveryAddress = cleanAddress,
            deliveryConditionAndFee = deliveryCondition
        )
    }

    fun formatCleanItemsBulletList(order: Order): String {
        val parsed = parseOrderSlipDetails(order)
        if (parsed.items.isEmpty()) return order.itemsSummary.ifBlank { "Registered in order archive." }
        return parsed.items.joinToString("\n") { item ->
            buildString {
                append("• ").append(item.titleAndSpecs)
                if (item.unitPriceText.isNotBlank() && item.lineTotalText.isNotBlank()) {
                    append(" — Unit: ").append(item.unitPriceText).append(" | Total: ").append(item.lineTotalText)
                } else if (item.lineTotalText.isNotBlank()) {
                    append(" — Price: ").append(item.lineTotalText)
                }
            }
        }
    }
}
