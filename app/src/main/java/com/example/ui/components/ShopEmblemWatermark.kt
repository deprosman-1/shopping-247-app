package com.example.ui.components

import com.example.R
import coil.compose.AsyncImage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@Composable
fun ShopEmblemWatermark(
    onOpenQrShare: () -> Unit,
    modifier: Modifier = Modifier,
    storeName: String = "ODIYA",
    tagline: String = "Shopping with ease and comfort",
    storePhone: String = "",
    secondaryPhone: String = "",
    storeAddress: String = ""
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("smartshop_embedded_emblem")
            .shadow(4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.95f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = 0.5f),
                            AccentGold.copy(alpha = 0.6f),
                            CyberCyan.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Tag: Official Installed Crest
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentGold.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, AccentGold.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "OFFICIAL INSTALLED ${storeName.uppercase()}",
                            color = AccentGold,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Row with ODIYA Logo Box & Full Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF052E16),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, AccentGold)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            AsyncImage(
                                model = R.drawable.shopping_logo_icon_1794752250458,
                                contentDescription = "ODIYA Logo",
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "ODIYA",
                                color = AccentGold,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = storeName.uppercase(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.2.sp
                        )
                        Text(
                            text = "• $tagline •",
                            color = CyberCyan,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val phonesCombined = listOf(storePhone, secondaryPhone).filter { it.isNotBlank() }.joinToString(" / ")
                        if (phonesCombined.isNotBlank() || storeAddress.isNotBlank()) {
                            val contactLine = listOfNotNull(
                                phonesCombined.takeIf { it.isNotBlank() }?.let { "📞 $it" },
                                storeAddress.takeIf { it.isNotBlank() }?.let { "📍 $it" }
                            ).joinToString("  •  ")
                            Text(
                                text = contactLine,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Trust Badges Pill Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrustPill(label = "100% Quality", icon = Icons.Default.Star)
                    TrustPill(label = "Affordable", icon = Icons.Default.Verified)
                    TrustPill(label = "Fast Delivery", icon = Icons.Default.Security)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Embedded QR Code Download & Share Trigger Button
                Button(
                    onClick = onOpenQrShare,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(34.dp)
                        .testTag("emblem_qr_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "Get QR Code",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "QR Code, Download Links & Share App",
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TrustPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
