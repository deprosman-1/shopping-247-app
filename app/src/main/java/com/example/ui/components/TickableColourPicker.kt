package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductColor
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan

/**
 * Standard preset colors available for product uploads.
 */
val PRESET_PRODUCT_COLORS = listOf(
    ProductColor("Black", "#000000"),
    ProductColor("White", "#FFFFFF"),
    ProductColor("Navy Blue", "#1E3A8A"),
    ProductColor("Cyber Cyan", "#06B6D4"),
    ProductColor("Crimson Red", "#DC2626"),
    ProductColor("Emerald Green", "#059669"),
    ProductColor("Accent Gold", "#F59E0B"),
    ProductColor("Royal Purple", "#7C3AED"),
    ProductColor("Rose Pink", "#EC4899"),
    ProductColor("Charcoal Grey", "#4B5563"),
    ProductColor("Sunset Orange", "#F97316"),
    ProductColor("Earth Brown", "#78350F"),
    ProductColor("Beige Cream", "#D4C5B9"),
    ProductColor("Olive Green", "#556B2F")
)

/**
 * Tickable colour display component for product upload and editing.
 * Allows tapping color chips to tick/untick available colors with instant visual feedback.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TickableColourPicker(
    selectedColors: List<ProductColor>,
    onColorsChanged: (List<ProductColor>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customHex by remember { mutableStateOf("#") }

    // Combined presets and any custom colors currently selected
    val allOptions = remember(selectedColors) {
        val list = PRESET_PRODUCT_COLORS.toMutableList()
        selectedColors.forEach { custom ->
            if (list.none { it.name.equals(custom.name, ignoreCase = true) || it.hex.equals(custom.hex, ignoreCase = true) }) {
                list.add(custom)
            }
        }
        list
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("tickable_colour_picker")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Available Product Colours",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedColors.isNotEmpty()) CyberCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "${selectedColors.size} Ticked",
                        color = if (selectedColors.isNotEmpty()) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap to tick/untick colors available for this product item:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tickable Swatches FlowRow
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allOptions.forEach { option ->
                    val isTicked = selectedColors.any {
                        it.name.equals(option.name, ignoreCase = true) || it.hex.equals(option.hex, ignoreCase = true)
                    }

                    TickableColorChip(
                        colorOption = option,
                        isTicked = isTicked,
                        onToggle = {
                            val newList = if (isTicked) {
                                selectedColors.filterNot {
                                    it.name.equals(option.name, ignoreCase = true) || it.hex.equals(option.hex, ignoreCase = true)
                                }
                            } else {
                                selectedColors + option
                            }
                            onColorsChanged(newList)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Custom Color Adder Row
            if (!showCustomInput) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCustomInput = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add custom color",
                        tint = AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ Add custom color or tone",
                        fontSize = 11.5.sp,
                        color = AccentGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        placeholder = { Text("Color Name (e.g. Teal)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )

                    OutlinedTextField(
                        value = customHex,
                        onValueChange = { customHex = it },
                        placeholder = { Text("#HEX code", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    AssistChip(
                        onClick = {
                            if (customName.isNotBlank() && customHex.isNotBlank() && customHex.startsWith("#")) {
                                val newColor = ProductColor(customName.trim(), customHex.trim())
                                if (selectedColors.none { it.name.equals(newColor.name, ignoreCase = true) }) {
                                    onColorsChanged(selectedColors + newColor)
                                }
                                customName = ""
                                customHex = "#"
                                showCustomInput = false
                            }
                        },
                        label = { Text("Tick", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

/**
 * Individual tickable color chip with round color circle and checkmark.
 */
@Composable
private fun TickableColorChip(
    colorOption: ProductColor,
    isTicked: Boolean,
    onToggle: () -> Unit
) {
    val parsedColor = remember(colorOption.hex) {
        try {
            Color(android.graphics.Color.parseColor(colorOption.hex))
        } catch (e: Exception) {
            Color.Gray
        }
    }

    val isDark = remember(parsedColor) {
        val luminance = (0.299 * parsedColor.red + 0.587 * parsedColor.green + 0.114 * parsedColor.blue)
        luminance < 0.55
    }

    val checkColor = if (isDark) Color.White else Color.Black

    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(10.dp),
        color = if (isTicked) CyberCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(
            width = if (isTicked) 1.5.dp else 1.dp,
            color = if (isTicked) CyberCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier.testTag("tickable_color_${colorOption.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Swatch circle with checkmark if ticked
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(parsedColor)
                    .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isTicked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Ticked",
                        tint = checkColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = colorOption.name,
                fontSize = 11.sp,
                fontWeight = if (isTicked) FontWeight.Bold else FontWeight.Medium,
                color = if (isTicked) CyberCyan else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
