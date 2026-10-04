package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.PeerDevice
import com.example.data.model.PeerSyncStatus
import com.example.data.model.SyncLogEntity
import com.example.sync.SyncRunSummary
import com.example.ui.theme.CardBorderLight
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenBg
import com.example.ui.theme.Navy900
import com.example.ui.theme.PendingOrange
import com.example.ui.theme.PendingOrangeBg
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealLight
import com.example.ui.util.FormatUtils

@Composable
fun SyncHubScreen(
    currentMember: String,
    currentDeviceName: String = "This Device",
    localIp: String,
    localPort: Int,
    isSyncing: Boolean,
    statusMessage: String,
    lastSyncedTime: Long?,
    members: List<FamilyMemberEntity>,
    peers: List<PeerDevice>,
    syncLogs: List<SyncLogEntity>,
    lastSyncSummary: SyncRunSummary? = null,
    onSyncAllClick: () -> Unit,
    onSyncPeerClick: (PeerDevice) -> Unit,
    onSyncMemberClick: (FamilyMemberEntity) -> Unit,
    onRemoveMemberClick: (FamilyMemberEntity) -> Unit = {},
    onDirectIpClick: () -> Unit,
    onPairSimulatedClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onDismissSyncSummary: () -> Unit = {},
    onExportImportClick: () -> Unit = {}
) {
    var memberToRemove by remember { mutableStateOf<FamilyMemberEntity?>(null) }
    val otherMembers = members.filter { !it.isSelf }
    val selfMember = members.firstOrNull { it.isSelf }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("sync_hub_screen")
    ) {
        item {
            // Local Device Node Card
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = Navy900),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(TealLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentMember.take(1).uppercase(),
                                    color = Navy900,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentMember,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = TealAccent.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "This Device",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TealAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = currentDeviceName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TealAccent
                                )
                            }
                        }

                        IconButton(onClick = onEditProfileClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = TealAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "OFFLINE SYNC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Local Wi-Fi P2P",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "DISCOVERY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Automatic",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealAccent
                            )
                        }
                    }

                    if (lastSyncedTime != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Last Synced: ${FormatUtils.formatDateTime(lastSyncedTime)}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSyncAllClick,
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy900,
                        contentColor = Color.White,
                        disabledContainerColor = Navy900.copy(alpha = 0.6f),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("sync_all_peers_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing...", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync All Members", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }

                OutlinedButton(
                    onClick = onPairSimulatedClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Navy900
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Navy900),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pair_device_action_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Navy900, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pair Device", color = Navy900, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Sync Execution Summary Banner (Handling Sync Failure & Partial Availability)
            if (lastSyncSummary != null) {
                Spacer(modifier = Modifier.height(14.dp))
                val isAllSuccess = lastSyncSummary.failedCount == 0 && lastSyncSummary.total > 0
                val isTotalFailure = lastSyncSummary.successCount == 0 && lastSyncSummary.total > 0
                val bannerBg = when {
                    isAllSuccess -> Color(0xFFF0FDF4)
                    isTotalFailure -> Color(0xFFFFF4F2)
                    else -> Color(0xFFFFFBEB)
                }
                val bannerBorder = when {
                    isAllSuccess -> Color(0xFFBBF7D0)
                    isTotalFailure -> Color(0xFFFFD4CF)
                    else -> Color(0xFFFFE0B2)
                }
                val bannerIcon = when {
                    isAllSuccess -> Icons.Default.CheckCircle
                    isTotalFailure -> Icons.Default.ErrorOutline
                    else -> Icons.Default.WarningAmber
                }
                val bannerTint = when {
                    isAllSuccess -> IncomeGreen
                    isTotalFailure -> ExpenseRed
                    else -> PendingOrange
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = bannerBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, bannerBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = bannerIcon,
                                    contentDescription = null,
                                    tint = bannerTint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isAllSuccess -> "All ${lastSyncSummary.total} Devices Synced Successfully"
                                        isTotalFailure -> "Sync Failed: Devices Unavailable (0/${lastSyncSummary.total})"
                                        else -> "Partial Sync: ${lastSyncSummary.successCount} of ${lastSyncSummary.total} Devices Synced"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                            }
                            IconButton(
                                onClick = onDismissSyncSummary,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Both devices must have Family Books open on the same Wi-Fi network. Either all devices will be available, some, or none if apps are closed.",
                            fontSize = 11.sp,
                            color = Color(0xFF4A5568),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        // Per-device results list
                        lastSyncSummary.deviceResults.forEach { res ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (res.isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (res.isSuccess) IncomeGreen else ExpenseRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${res.memberName} (${res.deviceName}): ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Navy900
                                )
                                Text(
                                    text = res.statusMessage,
                                    fontSize = 11.sp,
                                    color = if (res.isSuccess) IncomeGreen else ExpenseRed
                                )
                            }
                        }

                        if (!isAllSuccess) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = onSyncAllClick,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy900),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy900),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Navy900,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retry Sync All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Export / Import Ledger Backup Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExportImportClick() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Navy900),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = TealAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Export / Import Ledger",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Navy900
                            )
                            Text(
                                text = "Backup or restore full year or specific months (JSON / CSV)",
                                fontSize = 11.sp,
                                color = Color(0xFF5A6E85)
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onExportImportClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy900),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Navy900),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Manage", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy900)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Explanatory Banner: P2P & Family Members are the same
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F5FA)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = Navy900,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Family Members = Connected Devices",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Navy900
                        )
                        Text(
                            text = "When another family device connects over Wi-Fi, their profile is automatically stored as a verified Family Member with shared ledger access.",
                            fontSize = 11.sp,
                            color = Color(0xFF4A5568)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Connected Family Members Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Paired Family Members (${members.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
                OutlinedButton(
                    onClick = onPairSimulatedClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy900),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Navy900),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Navy900, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Pair Device", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy900)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Family Members List (Stored Devices)
        items(members, key = { it.id }) { member ->
            FamilyMemberDeviceCard(
                member = member,
                onSync = { onSyncMemberClick(member) },
                onRemove = { memberToRemove = member }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Discovered Devices on Wi-Fi Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Wi-Fi Devices Discovered (${peers.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
                Text(
                    text = "mDNS Scanning",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (peers.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = Navy900,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Listening for nearby FamilyBooks devices...",
                            fontWeight = FontWeight.SemiBold,
                            color = Navy900,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Open FamilyBooks on another phone or tap 'Connect IP' / '+ Pair Device' above.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        } else {
            items(peers, key = { it.deviceId }) { peer ->
                PeerCardItem(
                    peer = peer,
                    onSync = { onSyncPeerClick(peer) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Real-Time P2P Sync Event Logs Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Real-Time Sync Event Log",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Navy900
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Sync Logs List
        if (syncLogs.isEmpty()) {
            item {
                Text(
                    text = "No sync events recorded yet.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(syncLogs.take(15), key = { it.id }) { log ->
                SyncLogItem(log = log)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }

    if (memberToRemove != null) {
        val target = memberToRemove!!
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = {
                Text(
                    text = "Remove Paired Device?",
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to unpair ${target.name} (${target.deviceName.ifBlank { target.role }})?\n\nThis device will no longer sync with your family ledger.",
                    fontSize = 13.sp,
                    color = Color(0xFF4A5568)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveMemberClick(target)
                        memberToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseRed,
                        contentColor = Color.White
                    )
                ) {
                    Text("Remove Device", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun FamilyMemberDeviceCard(
    member: FamilyMemberEntity,
    onSync: () -> Unit,
    onRemove: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(FormatUtils.getMemberAvatarColor(member.name)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        val (statusText, statusBg, statusColor) = when {
                            member.isSelf -> Triple("This Device", TealLight, Navy900)
                            member.connectionStatus == "ONLINE" -> Triple("Online", IncomeGreenBg, IncomeGreen)
                            member.connectionStatus == "SYNCED" -> Triple("Synced", IncomeGreenBg, IncomeGreen)
                            else -> Triple("Offline / Paired", Color(0xFFF0F4F8), Color.Gray)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusBg
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = member.deviceName.ifBlank { member.role },
                        fontSize = 12.sp,
                        color = Color(0xFF5A6E85)
                    )

                    if (member.lastConnectedAt != null && !member.isSelf) {
                        Text(
                            text = "Last connected: ${FormatUtils.formatTime(member.lastConnectedAt)}",
                            fontSize = 10.sp,
                            color = Color(0xFF8C9BAE)
                        )
                    }
                }
            }

            if (!member.isSelf) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onRemove,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove Paired Device",
                            tint = ExpenseRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Remove", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = onSync,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy900,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PeerCardItem(
    peer: PeerDevice,
    onSync: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(FormatUtils.getMemberAvatarColor(peer.memberName)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = peer.memberName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = peer.memberName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        val (statusText, statusBg, statusColor) = when (peer.syncStatus) {
                            PeerSyncStatus.SYNCED -> Triple("Synced", IncomeGreenBg, IncomeGreen)
                            PeerSyncStatus.SYNCING -> Triple("Syncing...", TealLight, Navy900)
                            PeerSyncStatus.FAILED -> Triple("Failed", Color(0xFFFFEBEE), ExpenseRed)
                            else -> Triple("Discovered", Color(0xFFF0F4F8), Navy900)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusBg
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = peer.deviceName.ifBlank { "Family Device" },
                        fontSize = 12.sp,
                        color = Color(0xFF5A6E85)
                    )

                    if (peer.lastSyncedAt != null) {
                        Text(
                            text = "Last sync: ${FormatUtils.formatTime(peer.lastSyncedAt)}",
                            fontSize = 10.sp,
                            color = Color(0xFF8C9BAE)
                        )
                    }
                }
            }

            Button(
                onClick = onSync,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Navy900,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Sync & Pair", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun SyncLogItem(log: SyncLogEntity) {
    val isSuccess = log.status == "SUCCESS"
    val isIncoming = log.direction == "INCOMING"

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isSuccess) IncomeGreen else ExpenseRed)
                    .align(Alignment.CenterVertically)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${if (isIncoming) "Incoming ←" else "Outgoing →"} ${log.peerName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Navy900
                    )
                    Text(
                        text = FormatUtils.formatTime(log.timestamp),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = log.message,
                    fontSize = 11.sp,
                    color = Color(0xFF4A5568)
                )
            }
        }
    }
}
