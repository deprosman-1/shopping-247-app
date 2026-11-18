package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ProductCategory

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
        // "All" chip
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

        ProductCategory.values().forEach { cat ->
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

private fun getCategoryIcon(category: ProductCategory): ImageVector {
    return when (category) {
        ProductCategory.CLOTHES -> Icons.Default.DryCleaning
        ProductCategory.SHOES -> Icons.Default.ShoppingBag
        ProductCategory.BAGS -> Icons.Default.LocalMall
        ProductCategory.BEAUTY_PRODUCT -> Icons.Default.FaceRetouchingNatural
        ProductCategory.ELECTRONICS -> Icons.Default.Devices
        ProductCategory.FOOD -> Icons.Default.Fastfood
        ProductCategory.BEVERAGES -> Icons.Default.LocalBar
    }
}
