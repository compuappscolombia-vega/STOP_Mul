package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AuthUser
import com.example.model.UserProfile
import com.example.ui.theme.StopColors

@Composable
fun AuthDialog(
    currentUser: AuthUser?,
    currentProfile: UserProfile,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(StopColors.Slate900)
                    .border(1.dp, StopColors.Slate800, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Google Cloud Account",
                        style = MaterialTheme.typography.titleLarge,
                        color = StopColors.White
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("auth_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close account dialog",
                            tint = StopColors.Slate400
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StopColors.Slate950)
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(StopColors.Slate900)
                            .border(1.dp, StopColors.Slate800, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = currentProfile.avatar, fontSize = 26.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.name ?: currentProfile.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = StopColors.White
                        )
                        Text(
                            text = currentUser?.email ?: currentProfile.email ?: "Signed in with Google",
                            fontSize = 12.sp,
                            color = StopColors.Slate400
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = "Google verified",
                        tint = StopColors.Emerald400
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StopColors.Slate950.copy(alpha = 0.6f))
                        .border(1.dp, StopColors.Slate800, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "CLOUD SYNC ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StopColors.Emerald400
                    )
                    Text(
                        text = "Sync Code: ${currentProfile.syncCode}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = StopColors.Slate200
                    )
                    Text(
                        text = "Your multiplayer stats, rankings, and custom profile stay synced across all devices signed into this Google account.",
                        fontSize = 11.sp,
                        color = StopColors.Slate400
                    )
                }

                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = StopColors.Slate800),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auth_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign out",
                        tint = StopColors.Rose300,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Sign Out",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StopColors.Rose300
                    )
                }
            }
        }
    }
}
