package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan

@Composable
fun UniversalSizeGuideDialog(
    initialCategory: String = "Clothes",
    onDismiss: () -> Unit
) {
    var selectedTab by remember {
        mutableIntStateOf(
            when {
                initialCategory.contains("Shoe", ignoreCase = true) -> 1
                initialCategory.contains("Elec", ignoreCase = true) -> 2
                initialCategory.contains("Bag", ignoreCase = true) -> 3
                initialCategory.contains("Beaut", ignoreCase = true) || initialCategory.contains("Bev", ignoreCase = true) || initialCategory.contains("Food", ignoreCase = true) -> 4
                else -> 0
            }
        )
    }

    val categories = listOf("Clothes (Apparel)", "Shoes (Footwear)", "Electronics (Storage)", "Bags (Capacity)", "Beauty & Liquids")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Universal Size Guide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    categories.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 11.5.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> ClothingSizeTable()
                        1 -> ShoesSizeTable()
                        2 -> ElectronicsSizeTable()
                        3 -> BagsSizeTable()
                        4 -> BeautyLiquidTable()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_size_guide_button")
            ) {
                Text("Got It, Close", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun TableHeaderRow(col1: String, col2: String, col3: String, col4: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(col1, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(col2, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(col3, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(col4, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun TableDataRow(val1: String, val2: String, val3: String, val4: String, isEven: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isEven) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(val1, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            Text(val2, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(val3, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(val4, fontSize = 11.sp, modifier = Modifier.weight(1f))
        }
    }
    Spacer(modifier = Modifier.height(2.dp))
}

@Composable
private fun ClothingSizeTable() {
    Column {
        Text("Apparel Sizing Chart (International)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CyberCyan)
        Spacer(modifier = Modifier.height(6.dp))
        TableHeaderRow("Size", "Chest (in)", "Waist (in)", "Height (cm)")
        TableDataRow("S", "34 - 36\"", "28 - 30\"", "160 - 170cm", true)
        TableDataRow("M", "38 - 40\"", "32 - 34\"", "170 - 178cm", false)
        TableDataRow("L", "42 - 44\"", "36 - 38\"", "178 - 185cm", true)
        TableDataRow("XL", "46 - 48\"", "40 - 42\"", "185 - 192cm", false)
        TableDataRow("XXL", "50 - 52\"", "44 - 46\"", "192 - 200cm", true)
    }
}

@Composable
private fun ShoesSizeTable() {
    var selectedAudience by remember { mutableIntStateOf(0) }
    val audienceLabels = listOf("Men's Shoes", "Women's Shoes", "Children / Kids")

    Column {
        Text(
            text = "Universal Footwear Sizing Chart",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = CyberCyan
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            audienceLabels.forEachIndexed { idx, label ->
                val isSelected = selectedAudience == idx
                Surface(
                    onClick = { selectedAudience = idx },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedAudience) {
            0 -> {
                TableHeaderRow("EU Size", "US Men", "UK Size", "Foot (cm)")
                TableDataRow("38", "6.0 M", "5.0", "24.0 cm", true)
                TableDataRow("39", "6.5 M", "5.5", "24.5 cm", false)
                TableDataRow("40", "7.0 M", "6.5", "25.0 cm", true)
                TableDataRow("41", "8.0 M", "7.5", "25.8 cm", false)
                TableDataRow("42", "8.5 M", "8.0", "26.5 cm", true)
                TableDataRow("43", "9.5 M", "9.0", "27.3 cm", false)
                TableDataRow("44", "10.5 M", "10.0", "28.0 cm", true)
                TableDataRow("45", "11.5 M", "11.0", "28.8 cm", false)
                TableDataRow("46", "12.5 M", "12.0", "29.5 cm", true)
            }
            1 -> {
                TableHeaderRow("EU Size", "US Women", "UK Size", "Foot (cm)")
                TableDataRow("35", "5.0 W", "2.5", "21.5 cm", true)
                TableDataRow("36", "6.0 W", "3.5", "22.5 cm", false)
                TableDataRow("37", "6.5 W", "4.0", "23.0 cm", true)
                TableDataRow("38", "7.5 W", "5.0", "24.0 cm", false)
                TableDataRow("39", "8.0 W", "5.5", "24.5 cm", true)
                TableDataRow("40", "9.0 W", "6.5", "25.0 cm", false)
                TableDataRow("41", "9.5 W", "7.0", "25.5 cm", true)
                TableDataRow("42", "10.0 W", "8.0", "26.0 cm", false)
            }
            2 -> {
                TableHeaderRow("EU Size", "US Kids", "UK Size", "Foot (cm)")
                TableDataRow("24", "8K Kids", "7.5K", "15.0 cm", true)
                TableDataRow("26", "9.5K Kids", "9K", "16.0 cm", false)
                TableDataRow("28", "11K Kids", "10.5K", "17.5 cm", true)
                TableDataRow("30", "13K Kids", "12.5K", "19.0 cm", false)
                TableDataRow("32", "2Y Kids", "1.0", "20.0 cm", true)
                TableDataRow("34", "3Y Kids", "2.0", "21.5 cm", false)
                TableDataRow("36", "4.5Y Kids", "3.5", "22.5 cm", true)
            }
        }
    }
}

@Composable
private fun ElectronicsSizeTable() {
    Column {
        Text("Electronics Storage & Specs Guide", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CyberCyan)
        Spacer(modifier = Modifier.height(6.dp))
        TableHeaderRow("Capacity", "Photos", "4K Video", "Recommended")
        TableDataRow("128GB", "~30,000", "~6 Hours", "Casual Users", true)
        TableDataRow("256GB", "~60,000", "~12 Hours", "Standard Work", false)
        TableDataRow("512GB", "~120,000", "~24 Hours", "Power Users", true)
        TableDataRow("1TB", "~240,000", "~48 Hours", "Pro Creators", false)
    }
}

@Composable
private fun BagsSizeTable() {
    Column {
        Text("Bags & Luggage Capacity Guide", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CyberCyan)
        Spacer(modifier = Modifier.height(6.dp))
        TableHeaderRow("Capacity", "Type", "Laptop Size", "Best Used For")
        TableDataRow("15L - 20L", "Daily Backpack", "Up to 14\"", "Work / School", true)
        TableDataRow("25L - 30L", "Commuter Pro", "Up to 16\"", "Weekend Trips", false)
        TableDataRow("40L - 60L", "Travel Duffle", "Up to 17\"", "Extended Travel", true)
        TableDataRow("Large", "Carry-on Suitcase", "N/A", "Flight Luggage", false)
    }
}

@Composable
private fun BeautyLiquidTable() {
    Column {
        Text("Cosmetics & Liquid Volume Guide", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CyberCyan)
        Spacer(modifier = Modifier.height(6.dp))
        TableHeaderRow("Volume", "Appx. Uses", "Fl. Oz.", "Usage Type")
        TableDataRow("30 ml", "~60 Sprays / Doses", "1.0 fl oz", "Serum / Foundation", true)
        TableDataRow("50 ml", "~100 Sprays", "1.7 fl oz", "Daily Moisturizer", false)
        TableDataRow("100 ml", "~200 Sprays", "3.4 fl oz", "Parfum / Cologne", true)
        TableDataRow("500 ml", "~50 Portions", "16.9 fl oz", "Shampoo / Beverage", false)
        TableDataRow("1 Litre", "~100 Portions", "33.8 fl oz", "Family Beverage", true)
    }
}
