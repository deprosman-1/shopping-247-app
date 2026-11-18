package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.ProductColor
import com.example.ui.theme.SuccessGreen
import java.text.NumberFormat
import java.util.Locale

enum class ShoeAudience(val label: String) {
    MEN("Men"),
    WOMEN("Women"),
    CHILDREN("Children / Kids")
}

enum class ShoeStandard(val label: String) {
    EU("EU"),
    US("US"),
    UK("UK"),
    CM("CM")
}

fun formatShoeSizeWithAudienceAndStandard(rawSize: String, audience: ShoeAudience, standard: ShoeStandard): String {
    val clean = rawSize.replace("EU", "").replace("US", "").replace("UK", "").replace("cm", "").replace("Size", "").trim()
    val num = clean.toIntOrNull() ?: return rawSize
    // Corresponding universal measurements: e.g. 8.5, 9.5, 10.0, 10.5, 11.11...
    val universalMeasure = when (num) {
        34 -> "8.5"
        35 -> "9.0"
        36 -> "9.5"
        37 -> "10.0"
        38 -> "10.5"
        39 -> "11.11"
        40 -> "11.60"
        41 -> "12.20"
        42 -> "12.75"
        43 -> "13.30"
        44 -> "13.90"
        45 -> "14.50"
        46 -> "15.10"
        else -> String.format(Locale.US, "%.2f", 7.5 + (num * 0.45))
    }

    return when (standard) {
        ShoeStandard.EU -> "Size $num (EU)"
        ShoeStandard.US -> "Size $num (US $universalMeasure)"
        ShoeStandard.UK -> "Size $num (UK $universalMeasure)"
        ShoeStandard.CM -> "Size $num (${universalMeasure}cm)"
    }
}

@Composable
fun VariantSelector(
    product: Product,
    selectedBrand: String,
    onBrandSelected: (String) -> Unit,
    selectedSize: String,
    onSizeSelected: (String) -> Unit,
    selectedColor: ProductColor,
    onColorSelected: (ProductColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSizeGuide by remember { mutableStateOf(false) }

    val nairaFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }
    }

    val dynamicPrice = product.calculatePrice(selectedBrand, selectedSize)

    val isShoeProduct = product.category.contains("Shoe", ignoreCase = true) ||
            product.category.contains("Footwear", ignoreCase = true) ||
            product.model3dType.equals("SNEAKER", ignoreCase = true) ||
            product.title.contains("Shoe", ignoreCase = true) ||
            product.title.contains("Sneaker", ignoreCase = true) ||
            product.title.contains("Boot", ignoreCase = true) ||
            product.title.contains("Heel", ignoreCase = true) ||
            product.title.contains("Loafer", ignoreCase = true) ||
            product.title.contains("Slipper", ignoreCase = true) ||
            product.title.contains("Sandal", ignoreCase = true) ||
            product.title.contains("Slide", ignoreCase = true) ||
            product.title.contains("Kicks", ignoreCase = true) ||
            product.availableSizes.any { it.startsWith("EU") || (it.toIntOrNull() != null && it.toInt() in 20..50) }

    var shoeStandard by remember { mutableStateOf(ShoeStandard.EU) }
    var shoeAudience by remember { mutableStateOf(ShoeAudience.MEN) }

    if (showSizeGuide) {
        UniversalSizeGuideDialog(
            initialCategory = product.category,
            onDismiss = { showSizeGuide = false }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("variant_selector_panel"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Live Price & Synchronized Preview Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Calculated Checkout Price",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = nairaFormat.format(dynamicPrice),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SuccessGreen.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${product.stockQuantity} Available in $selectedBrand",
                            color = SuccessGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. SELECTABLE BRAND
            Text(
                text = "Brand (${product.availableBrands.size} Available)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                product.availableBrands.forEach { brandName ->
                    val isSelected = brandName == selectedBrand
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onBrandSelected(brandName)
                            val updatedPrice = product.calculatePrice(brandName, selectedSize)
                            Toast.makeText(context, "Brand '$brandName' selected • Price: ${nairaFormat.format(updatedPrice)}", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text(brandName) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("brand_chip_$brandName")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. SELECTABLE SIZE & UNIVERSAL SIZE GUIDE BUTTON
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isShoeProduct) "Shoe Size: $selectedSize" else "Size / Capacity: $selectedSize",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    onClick = { showSizeGuide = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.testTag("open_size_guide_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📏 Universal Size Guide",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isShoeProduct) {
                Spacer(modifier = Modifier.height(6.dp))
                // Universal Shoe Audience Switcher (Men, Women, Children)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("For:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ShoeAudience.values().forEach { aud ->
                        val isAudSelected = shoeAudience == aud
                        Surface(
                            onClick = { shoeAudience = aud },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAudSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (isAudSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        ) {
                            Text(
                                text = aud.label,
                                fontSize = 10.5.sp,
                                fontWeight = if (isAudSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAudSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                // Universal Shoe Standard Switcher (EU, US, UK, CM)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Standard:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ShoeStandard.values().forEach { std ->
                        val isStdSelected = shoeStandard == std
                        Surface(
                            onClick = { shoeStandard = std },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isStdSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (isStdSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        ) {
                            Text(
                                text = std.label,
                                fontSize = 10.5.sp,
                                fontWeight = if (isStdSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isStdSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val effectiveSizes = if (isShoeProduct) {
                    when (shoeAudience) {
                        ShoeAudience.MEN -> listOf("38", "39", "40", "41", "42", "43", "44", "45", "46")
                        ShoeAudience.WOMEN -> listOf("35", "36", "37", "38", "39", "40", "41", "42")
                        ShoeAudience.CHILDREN -> listOf("24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37")
                    }
                } else product.availableSizes

                effectiveSizes.forEach { rawSizeName ->
                    val displayLabel = if (isShoeProduct) formatShoeSizeWithAudienceAndStandard(rawSizeName, shoeAudience, shoeStandard) else rawSizeName
                    val fullUniversalLabel = if (isShoeProduct) {
                        val eu = formatShoeSizeWithAudienceAndStandard(rawSizeName, shoeAudience, ShoeStandard.EU)
                        val us = formatShoeSizeWithAudienceAndStandard(rawSizeName, shoeAudience, ShoeStandard.US)
                        val uk = formatShoeSizeWithAudienceAndStandard(rawSizeName, shoeAudience, ShoeStandard.UK)
                        val cm = formatShoeSizeWithAudienceAndStandard(rawSizeName, shoeAudience, ShoeStandard.CM)
                        "$eu ($us / $uk / $cm)"
                    } else rawSizeName

                    val isSelected = selectedSize == fullUniversalLabel || selectedSize == rawSizeName || selectedSize == displayLabel

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val targetSelected = if (isShoeProduct) fullUniversalLabel else rawSizeName
                            onSizeSelected(targetSelected)
                            val updatedPrice = product.calculatePrice(selectedBrand, rawSizeName)
                            Toast.makeText(context, "Shoe Size '$displayLabel' selected • Price: ${nairaFormat.format(updatedPrice)}", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text(displayLabel, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("size_chip_$rawSizeName")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. SELECTABLE COLOUR
            Text(
                text = "Colour: ${selectedColor.name}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                product.availableColors.forEach { colorItem ->
                    val isSelected = colorItem.name == selectedColor.name
                    val parsedColor = remember(colorItem.hex) {
                        try {
                            Color(android.graphics.Color.parseColor(colorItem.hex))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onColorSelected(colorItem)
                                Toast.makeText(context, "Colour '${colorItem.name}' selected", Toast.LENGTH_SHORT).show()
                            }
                            .padding(4.dp)
                            .testTag("color_chip_${colorItem.name}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (colorItem.hex.equals("#F8FAFC", ignoreCase = true) || colorItem.hex.equals("#FFFFFF", ignoreCase = true)) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = colorItem.name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
