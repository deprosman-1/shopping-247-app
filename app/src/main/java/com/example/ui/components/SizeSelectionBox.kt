package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan

/**
 * Dedicated Size Selection Box for Admin New Product Entry and Product Editing.
 * Provides category-adaptive presets, interactive toggles, quick bundle buttons,
 * and custom size additions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SizeSelectionBox(
    category: String,
    availableSizesCsv: String,
    onSizesChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var customSizeInput by remember { mutableStateOf("") }

    // Parse current selected sizes into a trimmed list
    val currentSelectedSizes = remember(availableSizesCsv) {
        availableSizesCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    fun toggleSize(size: String) {
        val list = currentSelectedSizes.toMutableList()
        if (list.any { it.equals(size, ignoreCase = true) }) {
            list.removeAll { it.equals(size, ignoreCase = true) }
        } else {
            list.add(size)
        }
        onSizesChanged(list.joinToString(", "))
    }

    fun addMultipleSizes(sizes: List<String>) {
        val list = currentSelectedSizes.toMutableList()
        sizes.forEach { s ->
            if (!list.any { it.equals(s, ignoreCase = true) }) {
                list.add(s)
            }
        }
        onSizesChanged(list.joinToString(", "))
    }

    fun clearAllSizes() {
        onSizesChanged("")
    }

    // Determine category-specific options
    val catLower = category.lowercase()
    val isShoes = catLower.contains("shoe") || catLower.contains("footwear")
    val isClothes = catLower.contains("cloth") || catLower.contains("apparel") || catLower.contains("wear")
    val isBags = catLower.contains("bag")
    val isBeauty = catLower.contains("beauty") || catLower.contains("cosmetic")
    val isBeveragesOrFood = catLower.contains("beverage") || catLower.contains("drink") || catLower.contains("food")
    val isElectronics = catLower.contains("electronic") || catLower.contains("gadget") || catLower.contains("phone")

    val standardPresets: List<String> = when {
        isShoes -> listOf("EU 36", "EU 37", "EU 38", "EU 39", "EU 40", "EU 41", "EU 42", "EU 43", "EU 44", "EU 45", "EU 46", "EU 47", "UK 6", "UK 7", "UK 8", "UK 9", "UK 10", "UK 11")
        isClothes -> listOf("XS", "S", "M", "L", "XL", "XXL", "3XL", "4XL", "Free Size", "One Size", "UK 8", "UK 10", "UK 12", "UK 14", "UK 16", "UK 18", "Slim Fit", "Regular Fit")
        isBags -> listOf("Mini", "Small", "Medium", "Large", "Extra Large", "One Size", "Standard", "Tote Size", "Crossbody", "Backpack (20L)", "Backpack (30L)")
        isBeauty -> listOf("15ml", "30ml", "50ml", "100ml", "150ml", "200ml", "250ml", "500ml", "Single Piece", "Travel Size", "Standard Bottle")
        isBeveragesOrFood -> listOf("330ml Can", "500ml Bottle", "750ml Bottle", "1 Litre", "1.5 Litres", "2 Litres", "Carton (6pcs)", "Carton (12pcs)", "Carton (24pcs)", "Single Pack", "500g", "1kg", "5kg", "10kg", "25kg", "50kg Bag")
        isElectronics -> listOf("32GB", "64GB", "128GB", "256GB", "512GB", "1TB", "Compact", "Standard", "Pro Size", "Universal Fit")
        else -> listOf("XS", "S", "M", "L", "XL", "XXL", "Small", "Medium", "Large", "Standard", "One Size")
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_size_selection_box")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "📐 SIZE SELECTION BOX",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = CyberCyan,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Tap sizes below to select for category '$category'",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (currentSelectedSizes.isNotEmpty()) {
                    TextButton(
                        onClick = { clearAllSizes() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Bundle Buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when {
                    isClothes -> {
                        OutlinedButton(
                            onClick = { addMultipleSizes(listOf("S", "M", "L", "XL", "XXL")) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("⚡ Add Standard Clothes (S-XXL)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { addMultipleSizes(listOf("XS", "S", "M", "L", "XL", "XXL", "3XL")) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("⚡ All Clothes (XS-3XL)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    isShoes -> {
                        OutlinedButton(
                            onClick = { addMultipleSizes(listOf("EU 40", "EU 41", "EU 42", "EU 43", "EU 44", "EU 45")) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("⚡ Popular Men's (EU 40-45)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { addMultipleSizes(listOf("EU 37", "EU 38", "EU 39", "EU 40", "EU 41")) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("⚡ Popular Women's (EU 37-41)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    isBags -> {
                        OutlinedButton(
                            onClick = { addMultipleSizes(listOf("Small", "Medium", "Large")) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("⚡ Small, Medium, Large", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    isBeauty -> {
                        OutlinedButton(
                            onClick = { addMultipleSizes(listOf("50ml", "100ml", "200ml")) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("⚡ 50ml, 100ml, 200ml", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    isBeveragesOrFood -> {
                        OutlinedButton(
                            onClick = { addMultipleSizes(listOf("500ml Bottle", "750ml Bottle", "1 Litre", "Carton (12pcs)")) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("⚡ 500ml, 750ml, 1L, Carton", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    isElectronics -> {
                        OutlinedButton(
                            onClick = { addMultipleSizes(listOf("128GB", "256GB", "512GB")) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("⚡ 128GB, 256GB, 512GB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selectable Size Chips (FlowRow)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                standardPresets.forEach { sizeLabel ->
                    val isSelected = currentSelectedSizes.any { it.equals(sizeLabel, ignoreCase = true) }
                    FilterChip(
                        selected = isSelected,
                        onClick = { toggleSize(sizeLabel) },
                        label = {
                            Text(
                                text = sizeLabel,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                fontSize = 11.5.sp
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            selectedLabelColor = Color.Black,
                            selectedLeadingIconColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Size Entry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customSizeInput,
                    onValueChange = { customSizeInput = it },
                    label = { Text("Custom Size") },
                    placeholder = { Text("e.g. 75cl, UK 14 Long, 42.5") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("admin_custom_size_input")
                )

                Button(
                    onClick = {
                        val trimmed = customSizeInput.trim()
                        if (trimmed.isNotBlank()) {
                            toggleSize(trimmed)
                            customSizeInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Size", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Currently Selected Sizes summary badge row
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Selected Sizes (${currentSelectedSizes.size}):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (currentSelectedSizes.isEmpty()) {
                        Text(
                            text = "No sizes selected yet. Tap any size chip above to add.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = availableSizesCsv,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
