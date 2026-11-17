package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyMemberEntity
import com.example.ui.theme.Navy900
import com.example.ui.theme.TealAccent
import com.example.ui.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarHeader(
    activeProfile: FamilyMemberEntity?,
    profileCount: Int,
    onProfileIconClick: () -> Unit
) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
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
                    text = if (activeProfile != null) {
                        "Offline Family Ledger • ${activeProfile.name}"
                    } else if (profileCount > 0) {
                        "Offline Family Ledger • $profileCount Profiles"
                    } else {
                        "Offline Individual Family Ledger"
                    },
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Navy900,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        actions = {
            // Fixed circular profile avatar button (48dp touch target, never wraps or breaks layout)
            IconButton(
                onClick = onProfileIconClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("top_profile_icon_button")
            ) {
                if (activeProfile != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                FormatUtils.parseAvatarColor(
                                    activeProfile.avatarColorHex,
                                    activeProfile.name
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeProfile.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TealAccent.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Family Profile",
                            tint = TealAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    )
}
