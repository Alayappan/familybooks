package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.Navy900
import com.example.ui.theme.PendingOrange
import com.example.ui.theme.PendingOrangeBg
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealLight
import com.example.ui.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarAndSyncBanner(
    currentMember: String,
    currentDeviceName: String = "This Device",
    statusMessage: String,
    isSyncing: Boolean,
    discoveredPeerCount: Int,
    onEditProfileClick: () -> Unit,
    onSyncClick: () -> Unit,
    onBannerClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Family",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Books",
                            fontWeight = FontWeight.Light,
                            fontSize = 20.sp,
                            color = TealAccent
                        )
                    }
                    Text(
                        text = "P2P Offline Family Accounting",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Navy900,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            ),
            actions = {
                // Fixed Profile Icon Button (fixed size avatar, prevents UI breakage)
                IconButton(
                    onClick = onEditProfileClick,
                    modifier = Modifier.testTag("device_owner_chip")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FormatUtils.getMemberAvatarColor(currentMember)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentMember.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Sync Now Action Icon
                IconButton(
                    onClick = onSyncClick,
                    modifier = Modifier.testTag("sync_action_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync Now with Wi-Fi Peers",
                        tint = if (isSyncing) TealAccent else Color.White,
                        modifier = if (isSyncing) Modifier.rotate(rotation) else Modifier
                    )
                }
            }
        )

        // Live P2P Sync Status Ribbon
        val (bannerBg, contentColor) = when {
            isSyncing -> TealLight to Navy900
            discoveredPeerCount > 0 -> IncomeGreenBg to IncomeGreen
            statusMessage.contains("failed", ignoreCase = true) -> PendingOrangeBg to PendingOrange
            else -> Color(0xFFEDF2F7) to Navy900
        }

        Surface(
            color = bannerBg,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBannerClick() }
                .testTag("sync_status_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = contentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    } else if (discoveredPeerCount > 0) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Connected",
                            tint = IncomeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Offline / Scanning",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (isSyncing) "Synchronizing with family..." else statusMessage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1
                    )
                }

                // Peer pill indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = contentColor.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = if (discoveredPeerCount > 0) "$discoveredPeerCount Peers" else "P2P Ready",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
