package com.example.ui.dialogs

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FamilyMemberEntity
import com.example.ui.theme.CardBorderLight
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.Navy900
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.familyOutlinedTextFieldColors
import com.example.ui.theme.familySwitchColors
import com.example.ui.util.FormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditProfileDialog(
    existingProfile: FamilyMemberEntity? = null,
    onDismiss: () -> Unit,
    onSaveProfile: (
        id: String?,
        name: String,
        role: String,
        avatarColorHex: String,
        notes: String,
        isPrimary: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf(existingProfile?.name ?: "") }
    var role by remember { mutableStateOf(existingProfile?.role ?: "Parent") }
    var avatarColorHex by remember { mutableStateOf(existingProfile?.avatarColorHex ?: "#0A2540") }
    var notes by remember { mutableStateOf(existingProfile?.notes ?: "") }
    var isPrimary by remember { mutableStateOf(existingProfile?.isPrimary ?: false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val roleOptions = listOf("Self", "Spouse", "Parent", "Child", "Sibling", "Household", "Other")
    val colorOptions = listOf(
        "#0A2540", "#00A88D", "#1E5B94", "#8E24AA",
        "#D81B60", "#FB8C00", "#00897B", "#5C6BC0"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .clip(RoundedCornerShape(16.dp))
                .testTag("add_edit_profile_dialog"),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (existingProfile != null) "Edit Family Profile" else "Add Family Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = "Track expenses, income, shares & liabilities by person",
                            fontSize = 12.sp,
                            color = TextSecondaryLight
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryLight)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Member Name") },
                    placeholder = { Text("e.g. Mom, Dad, Alex, Emma") },
                    singleLine = true,
                    colors = familyOutlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = ExpenseRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Family Role / Relationship",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    roleOptions.forEach { option ->
                        val selected = role.equals(option, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) Navy900 else Color(0xFFF0F4F8),
                            border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, CardBorderLight),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { role = option }
                        ) {
                            Text(
                                text = option,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else Navy900,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Avatar Badge Color",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colorOptions.forEach { hex ->
                        val selected = avatarColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(FormatUtils.parseAvatarColor(hex, name))
                                .border(
                                    width = if (selected) 2.5.dp else 0.dp,
                                    color = if (selected) TealAccent else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { avatarColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected color",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Manages household groceries & utilities") },
                    singleLine = true,
                    colors = familyOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Set as Default Active Profile",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy900
                        )
                        Text(
                            text = "Pre-select this profile when logging new entries",
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                    }
                    Switch(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it },
                        colors = familySwitchColors()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondaryLight, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Profile name cannot be empty"
                                return@Button
                            }
                            onSaveProfile(
                                existingProfile?.id,
                                name.trim(),
                                role.trim(),
                                avatarColorHex,
                                notes.trim(),
                                isPrimary
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy900,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        Text("Save Profile", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
