package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductColor
import com.example.data.model.ProductVariantPrice
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Locale

private val DEFAULT_VARIANT_COLOR_PALETTE = listOf(
    ProductColor("Black", "#111827"),
    ProductColor("White", "#F8FAFC"),
    ProductColor("Red", "#DC2626"),
    ProductColor("Blue", "#2563EB"),
    ProductColor("Gold", "#F59E0B"),
    ProductColor("Emerald", "#059669"),
    ProductColor("Brown", "#78350F"),
    ProductColor("Silver", "#94A3B8")
)

fun resolveColorHexByName(colorName: String, availableColors: List<ProductColor>): String {
    val clean = colorName.trim()
    availableColors.firstOrNull { it.name.equals(clean, ignoreCase = true) }?.let { return it.hex }
    DEFAULT_VARIANT_COLOR_PALETTE.firstOrNull { it.name.equals(clean, ignoreCase = true) }?.let { return it.hex }
    return when {
        clean.contains("black", true) -> "#111827"
        clean.contains("white", true) -> "#F8FAFC"
        clean.contains("red", true) || clean.contains("crimson", true) -> "#DC2626"
        clean.contains("blue", true) || clean.contains("navy", true) -> "#2563EB"
        clean.contains("cyan", true) -> "#06B6D4"
        clean.contains("gold", true) || clean.contains("yellow", true) -> "#F59E0B"
        clean.contains("green", true) || clean.contains("emerald", true) -> "#059669"
        clean.contains("pink", true) || clean.contains("rose", true) -> "#EC4899"
        clean.contains("purple", true) -> "#7C3AED"
        clean.contains("brown", true) || clean.contains("cognac", true) -> "#78350F"
        else -> "#334155"
    }
}

@Composable
fun VariantPriceMatrixEditor(
    primaryBrand: String,
    availableBrandsCsv: String,
    availableSizesCsv: String,
    selectedColors: List<ProductColor>,
    basePriceText: String,
    variantPrices: List<ProductVariantPrice>,
    onVariantPricesChanged: (List<ProductVariantPrice>) -> Unit,
    onSyncVariantCatalogOptions: (addedBrand: String, addedSize: String, addedColor: ProductColor) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }

    val knownBrands = remember(primaryBrand, availableBrandsCsv) {
        (listOf(primaryBrand) + availableBrandsCsv.split(","))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }
    val knownSizes = remember(availableSizesCsv) {
        availableSizesCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }
    val knownColors = remember(selectedColors) {
        (selectedColors + DEFAULT_VARIANT_COLOR_PALETTE)
            .distinctBy { it.name.trim().lowercase() }
    }

    var entryBrand by remember { mutableStateOf(primaryBrand.ifBlank { knownBrands.firstOrNull().orEmpty() }) }
    var entrySize by remember { mutableStateOf(knownSizes.firstOrNull().orEmpty()) }
    var entryAmount by remember { mutableStateOf(basePriceText) }
    var entryColorName by remember { mutableStateOf(selectedColors.firstOrNull()?.name ?: "Black") }

    LaunchedEffect(primaryBrand) {
        if (entryBrand.isBlank() && primaryBrand.isNotBlank()) {
            entryBrand = primaryBrand.trim()
        }
    }
    LaunchedEffect(basePriceText) {
        if (entryAmount.isBlank() && basePriceText.isNotBlank()) {
            entryAmount = basePriceText.trim()
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF052E16).copy(alpha = 0.08f),
        border = BorderStroke(1.5.dp, Color(0xFF059669)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("variant_price_matrix_editor")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Sell,
                        contentDescription = null,
                        tint = Color(0xFF047857),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BRAND / SIZE / AMOUNT / COLOUR PRICING",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp,
                        color = Color(0xFF065F46)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (variantPrices.isNotEmpty()) SuccessGreen else AccentGold.copy(alpha = 0.25f)
                ) {
                    Text(
                        text = "${variantPrices.size} Priced",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (variantPrices.isNotEmpty()) Color.White else Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Assign exact prices per Brand / Size / Colour (e.g., Brand A / Size 42 / ₦50 / Black and Brand A / Size 43 / ₦60 / Black, or different prices per Colour).",
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Quick Tap Chips for Entered Brands & Sizes
            if (knownBrands.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Brands:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                    knownBrands.forEach { b ->
                        val active = entryBrand.equals(b, ignoreCase = true)
                        Surface(
                            onClick = { entryBrand = b },
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) Color(0xFF065F46) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (active) AccentGold else Color(0xFF10B981).copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = b,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (knownSizes.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sizes:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                    knownSizes.forEach { sz ->
                        val active = entrySize.equals(sz, ignoreCase = true)
                        Surface(
                            onClick = { entrySize = sz },
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) Color(0xFF065F46) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (active) AccentGold else Color(0xFF10B981).copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = sz,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 4-Field Entry Row: BRAND / SIZE / AMOUNT / COLOUR
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = entryBrand,
                    onValueChange = { entryBrand = it },
                    label = { Text("Brand") },
                    placeholder = { Text("e.g. Brand A") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("variant_entry_brand_input")
                )
                OutlinedTextField(
                    value = entrySize,
                    onValueChange = { entrySize = it },
                    label = { Text("Size") },
                    placeholder = { Text("e.g. 42, 43, XL") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("variant_entry_size_input")
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = entryAmount,
                    onValueChange = { entryAmount = it },
                    label = { Text("Amount (₦)") },
                    placeholder = { Text("e.g. 50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("variant_entry_amount_input")
                )
                OutlinedTextField(
                    value = entryColorName,
                    onValueChange = { entryColorName = it },
                    label = { Text("Colour") },
                    placeholder = { Text("e.g. Black, Red") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("variant_entry_color_input")
                )
            }

            // Quick Colour Swatch Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Colours:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                knownColors.forEach { pc ->
                    val active = entryColorName.equals(pc.name, ignoreCase = true)
                    val swatch = remember(pc.hex) {
                        try { Color(android.graphics.Color.parseColor(pc.hex)) } catch (_: Exception) { Color.Gray }
                    }
                    Surface(
                        onClick = { entryColorName = pc.name },
                        shape = RoundedCornerShape(999.dp),
                        color = if (active) Color(0xFF065F46) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (active) AccentGold else Color.Gray.copy(alpha = 0.35f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(0.8.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = pc.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Add / Update Variant Price Button
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        val b = entryBrand.trim().ifBlank { primaryBrand.trim() }
                        val s = entrySize.trim().ifBlank { "Standard" }
                        val c = entryColorName.trim().ifBlank { "Black" }
                        val amt = entryAmount.trim().toDoubleOrNull() ?: 0.0
                        if (b.isBlank() || amt <= 0.0) {
                            Toast.makeText(context, "Enter Brand, Size, Colour and a valid Amount (₦)", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val hex = resolveColorHexByName(c, knownColors)
                        val newItem = ProductVariantPrice(
                            brand = b,
                            size = s,
                            colorName = c,
                            amount = amt,
                            colorHex = hex
                        )
                        val updated = variantPrices.filterNot {
                            it.brand.equals(b, ignoreCase = true) &&
                                it.size.equals(s, ignoreCase = true) &&
                                it.colorName.equals(c, ignoreCase = true)
                        } + newItem
                        onVariantPricesChanged(updated)
                        onSyncVariantCatalogOptions(b, s, ProductColor(c, hex))
                        Toast.makeText(
                            context,
                            "Saved: $b / $s / ${nairaFormat.format(amt)} / $c",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF065F46)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_add_variant_price_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Brand / Size / ₦ / Colour", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }

                OutlinedButton(
                    onClick = {
                        val baseAmt = basePriceText.toDoubleOrNull()?.takeIf { it > 0.0 }
                            ?: entryAmount.toDoubleOrNull()?.takeIf { it > 0.0 }
                            ?: 0.0
                        if (baseAmt <= 0.0 || knownBrands.isEmpty() || knownSizes.isEmpty()) {
                            Toast.makeText(context, "Enter Base Price, Brand & Sizes first to auto-build rows", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }
                        val activeColors = if (selectedColors.isNotEmpty()) selectedColors else listOf(ProductColor("Black", "#111827"))
                        val merged = variantPrices.toMutableList()
                        for (b in knownBrands.take(4)) {
                            for (sz in knownSizes.take(8)) {
                                for (col in activeColors.take(4)) {
                                    val exists = merged.any {
                                        it.brand.equals(b, true) && it.size.equals(sz, true) && it.colorName.equals(col.name, true)
                                    }
                                    if (!exists) {
                                        merged.add(
                                            ProductVariantPrice(
                                                brand = b,
                                                size = sz,
                                                colorName = col.name,
                                                amount = baseAmt,
                                                colorHex = col.hex
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        onVariantPricesChanged(merged)
                        Toast.makeText(context, "Matrix ready (${merged.size} rows). Tap any row to customize its Amount.", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("admin_autogenerate_variant_matrix_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Auto-Fill", modifier = Modifier.size(16.dp), tint = Color(0xFF047857))
                }
            }

            if (variantPrices.isNotEmpty()) {
                HorizontalDivider(color = Color(0xFF10B981).copy(alpha = 0.25f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FORMAT: BRAND / SIZE / AMOUNT / COLOUR",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberCyan
                    )
                    TextButton(
                        onClick = { onVariantPricesChanged(emptyList()) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Clear All", fontSize = 10.5.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    variantPrices.forEachIndexed { idx, entry ->
                        val swatch = try {
                            Color(android.graphics.Color.parseColor(entry.colorHex))
                        } catch (_: Exception) {
                            Color.Gray
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    entryBrand = entry.brand
                                    entrySize = entry.size
                                    entryAmount = entry.amount.toInt().toString()
                                    entryColorName = entry.colorName
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(swatch)
                                        .border(1.dp, Color.Black.copy(alpha = 0.35f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${entry.brand} / ${entry.size} / ${nairaFormat.format(entry.amount)} / ${entry.colorName}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        entryBrand = entry.brand
                                        entrySize = entry.size
                                        entryAmount = entry.amount.toInt().toString()
                                        entryColorName = entry.colorName
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Variant Price",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onVariantPricesChanged(variantPrices.filterIndexed { i, _ -> i != idx })
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Variant Price",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
