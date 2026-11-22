package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import com.example.data.model.Order
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CancelOrderConfirmDialog(
    order: Order,
    onDismiss: () -> Unit,
    onConfirmCancel: (reason: String) -> Unit
) {
    val quickReasons = listOf(
        "Ordered by mistake",
        "Changed size / color",
        "Delivery address needs update",
        "Found another item",
        "Payment issue"
    )
    var selectedReason by remember { mutableStateOf(quickReasons.first()) }
    var customNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Cancel,
                contentDescription = null,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Cancel Order ${order.orderId}?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pre-dispatch orders can be cancelled immediately. Reserved stock is restored automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickReasons.forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            label = { Text(reason, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = customNote,
                    onValueChange = { customNote = it },
                    label = { Text("Additional note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = listOf(selectedReason, customNote.trim())
                        .filter { it.isNotBlank() }
                        .joinToString(" — ")
                    onConfirmCancel(finalReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                modifier = Modifier.testTag("confirm_cancel_order_button")
            ) {
                Text("Cancel Order", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Order")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReturnRefundRequestDialog(
    order: Order,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String, proofUri: String) -> Unit
) {
    val naira = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply { maximumFractionDigits = 0 }
    }
    val reasons = listOf(
        "Wrong item / size delivered",
        "Damaged or defective item",
        "Missing item from parcel",
        "Item does not match description",
        "Quality / Freshness issue"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var details by remember { mutableStateOf("") }
    var proofUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) proofUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF062E22),
        titleContentColor = Color(0xFFFDE047),
        icon = {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFDC2626).copy(alpha = 0.22f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.AssignmentReturn,
                    contentDescription = "Request Return or Refund",
                    tint = Color(0xFFFDE047),
                    modifier = Modifier.padding(10.dp).size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Request Return / Refund",
                fontWeight = FontWeight.Black,
                fontSize = 19.sp,
                color = Color(0xFFFDE047)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .testTag("return_refund_dialog"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF064E3B),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF34D399).copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Order ${order.orderId} • ${naira.format(order.totalAmount)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color(0xFF34D399)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = order.itemsSummary.replace("\n", " "),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White,
                            maxLines = 3
                        )
                    }
                }
                Text(
                    text = "Select Return / Refund Reason:",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Color(0xFF38BDF8)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reasons.forEach { reason ->
                        val isSelected = selectedReason == reason
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedReason = reason },
                            label = {
                                Text(
                                    text = reason,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) Color(0xFF052E16) else Color.White
                                )
                            },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFDE047),
                                containerColor = Color(0xFF065F46),
                                selectedLabelColor = Color(0xFF052E16),
                                labelColor = Color.White
                            ),
                            border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0xFF34D399).copy(alpha = 0.6f),
                                selectedBorderColor = Color(0xFFFDE047)
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = {
                        Text(
                            "Describe Issue & Refund Preference",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    },
                    placeholder = {
                        Text(
                            "Explain defect, wrong size, or preferred bank refund",
                            color = Color.White.copy(alpha = 0.65f),
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFDE047),
                        unfocusedBorderColor = Color(0xFF34D399)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (proofUri != null) Color(0xFF34D399) else Color(0xFFFDE047)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (proofUri != null) Color(0xFF065F46) else Color(0xFF064E3B),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(
                        if (proofUri != null) Icons.Default.CheckCircle else Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach proof photo",
                        modifier = Modifier.size(18.dp),
                        tint = if (proofUri != null) Color(0xFF34D399) else Color(0xFFFDE047)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (proofUri != null) "Photo Proof Attached (Tap to change)" else "Attach Photo Proof (Optional)",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp,
                        color = Color.White
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        selectedReason,
                        details.trim().ifBlank { selectedReason },
                        proofUri?.toString().orEmpty()
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color(0xFF052E16)
                ),
                modifier = Modifier.height(48.dp).testTag("submit_return_refund_button")
            ) {
                Text("Submit Request", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text("Close", fontWeight = FontWeight.Black, color = Color(0xFFF87171))
            }
        }
    )
}
