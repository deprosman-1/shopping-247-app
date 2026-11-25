package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Man
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Woman
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductCategory
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan

/**
 * Compact pinned Category Dropdown Button designed to sit beside the scrollable
 * catalog action/filter strip so the front view saves vertical space for products.
 */
@Composable
fun CategorySelectorButton(
    selectedCategory: String?,
    categoryCounts: Map<String, Int>,
    totalCount: Int,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val activeCat = remember(selectedCategory) {
        ProductCategory.entries.firstOrNull { it.displayName.equals(selectedCategory, ignoreCase = true) }
    }
    val activeIcon = activeCat?.let { getCategoryIcon(it) } ?: Icons.Default.Category
    val isFiltered = selectedCategory != null

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = if (isFiltered) AccentGold else Color(0xFF064E3B),
            border = BorderStroke(
                width = 1.2.dp,
                color = if (isFiltered) AccentGold else AccentGold.copy(alpha = 0.7f)
            ),
            shadowElevation = 2.dp,
            modifier = Modifier
                .height(34.dp)
                .testTag("category_dropdown_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                Icon(
                    imageVector = activeIcon,
                    contentDescription = "Category Filter",
                    tint = if (isFiltered) Color.Black else AccentGold,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = selectedCategory ?: "Category",
                    color = if (isFiltered) Color.Black else AccentGold,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Choose Category",
                    tint = if (isFiltered) Color.Black else AccentGold,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color.White
        ) {
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocalMall,
                        contentDescription = null,
                        tint = if (selectedCategory == null) Color(0xFFDC2626) else Color(0xFF000000),
                        modifier = Modifier.size(18.dp)
                    )
                },
                text = {
                    Text(
                        text = "All 3D Items ($totalCount)",
                        fontWeight = if (selectedCategory == null) FontWeight.Black else FontWeight.ExtraBold,
                        color = if (selectedCategory == null) Color(0xFFDC2626) else Color(0xFF000000)
                    )
                },
                trailingIcon = if (selectedCategory == null) {
                    { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp)) }
                } else null,
                colors = androidx.compose.material3.MenuDefaults.itemColors(
                    textColor = Color(0xFF000000),
                    leadingIconColor = Color(0xFF000000),
                    trailingIconColor = Color(0xFFDC2626)
                ),
                onClick = {
                    onSelectCategory(null)
                    expanded = false
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("category_chip_all")
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = Color(0xFFE5E7EB)
            )

            ProductCategory.entries.forEach { cat ->
                val isSelected = selectedCategory == cat.displayName
                val count = categoryCounts[cat.displayName] ?: 0
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = getCategoryIcon(cat),
                            contentDescription = cat.displayName,
                            tint = if (isSelected) Color(0xFFDC2626) else Color(0xFF000000),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            text = if (count > 0) "${cat.displayName} ($count)" else cat.displayName,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.ExtraBold,
                            color = if (isSelected) Color(0xFFDC2626) else Color(0xFF000000)
                        )
                    },
                    trailingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = androidx.compose.material3.MenuDefaults.itemColors(
                        textColor = Color(0xFF000000),
                        leadingIconColor = Color(0xFF000000),
                        trailingIconColor = Color(0xFFDC2626)
                    ),
                    onClick = {
                        onSelectCategory(if (isSelected) null else cat.displayName)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("category_chip_${cat.name.lowercase()}")
                )
            }
        }
    }
}

/**
 * Horizontally scrollable LazyRow arrangement of ALL 23 product categories
 * for effortless sideways browsing and 1-tap category switching on the home storefront.
 */
@Composable
fun HorizontalCategoryStrip(
    selectedCategory: String?,
    categoryCounts: Map<String, Int>,
    totalCount: Int,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = remember { ProductCategory.entries }
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("horizontal_category_strip"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item(key = "cat_all", contentType = "category_pill") {
            val isAll = selectedCategory == null
            Surface(
                onClick = { onSelectCategory(null) },
                shape = RoundedCornerShape(12.dp),
                color = if (isAll) AccentGold else Color(0xFF064E3B),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isAll) AccentGold else CyberCyan.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("horizontal_cat_all")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 11.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalMall,
                        contentDescription = "All Products",
                        tint = if (isAll) Color.Black else AccentGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "All ($totalCount)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isAll) Color.Black else Color.White,
                        maxLines = 1
                    )
                }
            }
        }

        items(
            items = entries,
            key = { it.name },
            contentType = { "category_pill" }
        ) { cat ->
            val isSelected = selectedCategory.equals(cat.displayName, ignoreCase = true)
            val count = categoryCounts[cat.displayName] ?: 0
            Surface(
                onClick = { onSelectCategory(if (isSelected) null else cat.displayName) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) AccentGold else Color(0xFF064E3B),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) AccentGold else CyberCyan.copy(alpha = 0.38f)
                ),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("horizontal_cat_${cat.name.lowercase()}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 11.dp)
                ) {
                    Icon(
                        imageVector = getCategoryIcon(cat),
                        contentDescription = cat.displayName,
                        tint = if (isSelected) Color.Black else CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (count > 0) "${cat.displayName} ($count)" else cat.displayName,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryFilterBar(
    selectedCategory: String?, // null = "All"
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onSelectCategory(null) },
            label = { Text("All 3D Items", fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal) },
            leadingIcon = if (selectedCategory == null) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            shape = RoundedCornerShape(14.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.testTag("category_chip_all")
        )

        ProductCategory.entries.forEach { cat ->
            val isSelected = selectedCategory == cat.displayName
            val icon = getCategoryIcon(cat)

            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(if (isSelected) null else cat.displayName) },
                label = { Text(cat.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = cat.displayName,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("category_chip_${cat.name.lowercase()}")
            )
        }
    }
}

fun getCategoryIcon(category: ProductCategory): ImageVector {
    return when (category) {
        ProductCategory.CLOTHES -> Icons.Default.DryCleaning
        ProductCategory.SHOES -> Icons.Default.ShoppingBag
        ProductCategory.BAGS -> Icons.Default.LocalMall
        ProductCategory.BEAUTY_PRODUCT -> Icons.Default.FaceRetouchingNatural
        ProductCategory.ELECTRONICS -> Icons.Default.Devices
        ProductCategory.FOOD -> Icons.Default.Fastfood
        ProductCategory.BEVERAGES -> Icons.Default.LocalBar
        ProductCategory.MEN -> Icons.Default.Man
        ProductCategory.WOMEN -> Icons.Default.Woman
        ProductCategory.BABIES -> Icons.Default.ChildCare
        ProductCategory.FRUITS_VEGETABLES -> Icons.Default.Eco
        ProductCategory.HOUSEHOLD -> Icons.Default.Home
        ProductCategory.MUSICALS -> Icons.Default.MusicNote
        ProductCategory.EDUCATIONAL -> Icons.Default.School
        ProductCategory.JEWELRY -> Icons.Default.Diamond
        ProductCategory.STATIONARIES -> Icons.Default.EditNote
        ProductCategory.PHONE_ACCESSORIES -> Icons.Default.PhoneAndroid
        ProductCategory.UPHOLSTERY -> Icons.Default.Weekend
        ProductCategory.RUBBER_PLASTIC -> Icons.Default.Build
        ProductCategory.AUTOMOBILE_SPARE_PARTS -> Icons.Default.DirectionsCar
        ProductCategory.CLOTHING_SEWING_MATERIALS -> Icons.Default.ContentCut
        ProductCategory.HEALTH_MEDICAL_EQUIPMENT -> Icons.Default.MedicalServices
        ProductCategory.PROCESSED_FOODS_PASTRIES -> Icons.Default.BakeryDining
        ProductCategory.FROZEN_FOODS_MEAT -> Icons.Default.AcUnit
    }
}
