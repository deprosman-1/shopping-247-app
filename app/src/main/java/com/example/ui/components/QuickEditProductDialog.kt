package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductColor
import com.example.data.model.ProductVariantPrice
import com.example.ui.theme.AccentGold
import com.example.util.ProductImageHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEditProductDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(product.title) }
    var category by remember { mutableStateOf(product.category) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var brand by remember { mutableStateOf(product.brand) }
    var availableBrands by remember { mutableStateOf(product.availableBrands.joinToString(", ")) }
    var availableSizes by remember { mutableStateOf(product.availableSizes.joinToString(", ")) }
    var selectedColors by remember { mutableStateOf(product.availableColors) }
    var basePrice by remember { mutableStateOf(product.basePrice.toInt().toString()) }
    var costPrice by remember { mutableStateOf(product.costPrice.toInt().toString()) }
    var stockQuantity by remember { mutableStateOf(product.stockQuantity.toString()) }
    var expiryMonthsRemaining by remember { mutableStateOf(product.expiryMonthsRemaining.toString()) }
    var discountPercent by remember { mutableStateOf(product.discountPercent.toString()) }
    var imageUrl by remember { mutableStateOf(product.imageUrl) }
    var galleryUrls by remember { mutableStateOf(product.galleryUrls) }
    var variantPrices by remember { mutableStateOf(product.variantPrices) }
    var model3dType by remember { mutableStateOf(product.model3dType) }
    var model3dExpanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(product.description) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUrl = uri.toString()
        }
    }
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            galleryUrls = (galleryUrls + uris.map { it.toString() }).distinct().take(6)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit Catalog Features",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Product Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category Dropdown (High-Contrast Emerald & Gold with Large Bold Typography)
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    "Product Category",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFDE68A)
                                )
                            },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            ),
                            trailingIcon = {
                                Icon(
                                    imageVector = if (categoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand Category List",
                                    tint = AccentGold
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF064E3B),
                                unfocusedContainerColor = Color(0xFF052E16),
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = Color(0xFF10B981),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier
                                .background(Color(0xFF052E16))
                        ) {
                            ProductCategory.values().forEach { cat ->
                                val isCatSelected = cat.displayName.equals(category, ignoreCase = true)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = cat.displayName,
                                            fontSize = 17.sp,
                                            fontWeight = if (isCatSelected) FontWeight.Black else FontWeight.Bold,
                                            color = if (isCatSelected) AccentGold else Color(0xFFF8FAFC)
                                        )
                                    },
                                    trailingIcon = if (isCatSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected Category",
                                                tint = AccentGold
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier.background(
                                        if (isCatSelected) Color(0xFF065F46) else Color(0xFF052E16)
                                    ),
                                    onClick = {
                                        category = cat.displayName
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Price, Cost Price & Discount
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = basePrice,
                            onValueChange = { basePrice = it },
                            label = { Text("Base Price (₦)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = costPrice,
                            onValueChange = { costPrice = it },
                            label = { Text("Cost Price (₦)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = discountPercent,
                            onValueChange = { discountPercent = it },
                            label = { Text("Discount %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    // Stock Quantity, Expiry Months & 3D Model Type
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = stockQuantity,
                            onValueChange = { stockQuantity = it },
                            label = { Text("Stock Qty (<=2 Low)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = expiryMonthsRemaining,
                            onValueChange = { expiryMonthsRemaining = it },
                            label = { Text("Expiry (<=3mo Alert)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        // 3D Model Selector
                        val modelTypes = listOf("SNEAKER", "CLOTH", "BAG", "COSMETIC", "BOTTLE", "PHONE", "BOX")
                        ExposedDropdownMenuBox(
                            expanded = model3dExpanded,
                            onExpandedChange = { model3dExpanded = !model3dExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = model3dType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("3D Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = model3dExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = model3dExpanded,
                                onDismissRequest = { model3dExpanded = false }
                            ) {
                                modelTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            model3dType = type
                                            model3dExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Primary Brand & Available Brands
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Brand") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = availableBrands,
                            onValueChange = { availableBrands = it },
                            label = { Text("Brand Variants (csv)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Dedicated Size Selection Box
                    SizeSelectionBox(
                        category = category,
                        availableSizesCsv = availableSizes,
                        onSizesChanged = { availableSizes = it }
                    )

                    TickableColourPicker(
                        selectedColors = selectedColors,
                        onColorsChanged = { selectedColors = it }
                    )

                    VariantPriceMatrixEditor(
                        primaryBrand = brand,
                        availableBrandsCsv = availableBrands,
                        availableSizesCsv = availableSizes,
                        selectedColors = selectedColors,
                        basePriceText = basePrice,
                        variantPrices = variantPrices,
                        onVariantPricesChanged = { variantPrices = it },
                        onSyncVariantCatalogOptions = { addedBrand, addedSize, addedColor ->
                            if (brand.isBlank()) brand = addedBrand
                            val curBrands = availableBrands.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                            if (addedBrand.isNotBlank() && curBrands.none { it.equals(addedBrand, true) }) {
                                curBrands.add(addedBrand)
                                availableBrands = curBrands.joinToString(", ")
                            }
                            val curSizes = availableSizes.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                            if (addedSize.isNotBlank() && curSizes.none { it.equals(addedSize, true) }) {
                                curSizes.add(addedSize)
                                availableSizes = curSizes.joinToString(", ")
                            }
                            if (selectedColors.none { it.name.equals(addedColor.name, true) }) {
                                selectedColors = selectedColors + addedColor
                            }
                        }
                    )

                    // Image URL / File Selector
                    val isPhotoBrokenOrExpired = ProductImageHelper.isUnreadableImageUri(context, imageUrl)
                    val isNewLocalSelection = ProductImageHelper.isLocalOnlyUri(imageUrl) && !isPhotoBrokenOrExpired

                    if (isPhotoBrokenOrExpired) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDC2626).copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "⚠️ Photo Link Expired or Missing",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = Color(0xFFDC2626)
                                )
                                Text(
                                    text = "This item currently falls back to a default category image because its old phone link was temporary. Tap 'Pick Real Product Photo' below and Save to upload the real image to Supabase Cloud.",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pick Real Product Photo", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPhotoBrokenOrExpired) "Pick Real Product Photo from Gallery" else "Change Product Photo from Gallery",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { imageUrl = it },
                            label = { Text("Image URL or Uploaded Photo") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Select Image")
                        }
                    }

                    if (imageUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            AsyncImage(
                                model = ProductImageHelper.buildImageRequest(
                                    context = context,
                                    imageUrl = imageUrl,
                                    category = category,
                                    title = title
                                ),
                                contentDescription = "Uploaded Image Preview",
                                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = when {
                                        isPhotoBrokenOrExpired -> "Showing Fallback Stock Image (Old Link Expired)"
                                        isNewLocalSelection -> "New Gallery Photo Selected"
                                        else -> "Cloud Photo Linked"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPhotoBrokenOrExpired) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = when {
                                        isPhotoBrokenOrExpired -> "Pick a gallery photo above to replace this fallback image"
                                        isNewLocalSelection -> "Will upload permanently to Supabase Storage when you Save"
                                        else -> "Active across catalog cards and 3D detail view"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = {
                            galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Extra Gallery Angles (${galleryUrls.size}/6)", fontWeight = FontWeight.Bold)
                    }

                    if (galleryUrls.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            galleryUrls.forEach { gUrl ->
                                Box(modifier = Modifier.size(64.dp)) {
                                    AsyncImage(
                                        model = ProductImageHelper.buildImageRequest(
                                            context = context,
                                            imageUrl = gUrl,
                                            category = category,
                                            title = title
                                        ),
                                        contentDescription = "Extra gallery photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                    )
                                    IconButton(
                                        onClick = { galleryUrls = galleryUrls.filterNot { it == gUrl } },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(22.dp)
                                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (title.isBlank() || basePrice.isBlank()) {
                                Toast.makeText(context, "Please provide title and price", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val priceVal = basePrice.toDoubleOrNull() ?: product.basePrice
                            val costVal = costPrice.toDoubleOrNull() ?: product.costPrice
                            val qtyVal = stockQuantity.toIntOrNull() ?: product.stockQuantity
                            val discVal = discountPercent.toIntOrNull() ?: product.discountPercent
                            val expVal = expiryMonthsRemaining.toIntOrNull() ?: product.expiryMonthsRemaining

                            val mergedBrands = buildList {
                                if (availableBrands.isNotBlank()) {
                                    availableBrands.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { add(it) }
                                } else {
                                    add(brand.trim())
                                }
                                variantPrices.forEach { vp ->
                                    if (vp.brand.isNotBlank() && none { it.equals(vp.brand, true) }) add(vp.brand)
                                }
                            }
                            val mergedSizes = buildList {
                                if (availableSizes.isNotBlank()) {
                                    availableSizes.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { add(it) }
                                } else if (variantPrices.isEmpty()) {
                                    add("Standard")
                                }
                                variantPrices.forEach { vp ->
                                    if (vp.size.isNotBlank() && none { it.equals(vp.size, true) }) add(vp.size)
                                }
                            }.ifEmpty { listOf("Standard") }
                            val mergedColors = buildList {
                                val baseColors = if (selectedColors.isNotEmpty()) selectedColors else product.availableColors
                                baseColors.forEach { add(it) }
                                variantPrices.forEach { vp ->
                                    if (vp.colorName.isNotBlank() && none { it.name.equals(vp.colorName, true) }) {
                                        add(ProductColor(vp.colorName, vp.colorHex))
                                    }
                                }
                            }

                            val updatedProd = product.copy(
                                title = title.trim(),
                                category = category,
                                brand = brand.trim(),
                                availableBrands = mergedBrands,
                                availableSizes = mergedSizes,
                                availableColors = mergedColors,
                                basePrice = priceVal,
                                costPrice = costVal,
                                discountPercent = discVal,
                                stockQuantity = qtyVal,
                                expiryMonthsRemaining = expVal,
                                imageUrl = if (imageUrl.isNotBlank()) imageUrl.trim() else product.imageUrl,
                                galleryUrls = galleryUrls.map { it.trim() }.filter { it.isNotBlank() && it != imageUrl.trim() }.distinct(),
                                model3dType = model3dType,
                                description = description.ifBlank { product.description },
                                variantPrices = variantPrices
                            )

                            onSave(updatedProd)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Sync Features", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
