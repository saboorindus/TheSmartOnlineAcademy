package com.echologics.thesmartonlineacademy.ui.teacher.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.data.model.ApprovalStatus
import com.echologics.thesmartonlineacademy.ui.common.theme.Amber
import com.echologics.thesmartonlineacademy.ui.common.theme.AmberLight

/**
 * Drop this composable at the top of TeacherBookingsScreen or any
 * teacher home screen. It shows nothing when approved.
 */
@Composable
fun ApprovalStatusBanner(
    viewModel: ApprovalBannerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    if (state.isLoading || state.approvalStatus == ApprovalStatus.APPROVED) return

    val (bgColor, borderColor, iconColor, icon, title, subtitle) = when (state.approvalStatus) {
        ApprovalStatus.PENDING -> BannerData(
            bg = AmberLight,
            border = Amber,
            iconColor = Amber,
            icon = Icons.Default.HourglassTop,
            title = "Profile under review",
            subtitle = "Our team is reviewing your profile. This usually takes 24–48 hours. You'll be notified once approved."
        )
        ApprovalStatus.REJECTED -> BannerData(
            bg = Color(0xFFFCEBEB),
            border = Color(0xFFA32D2D),
            iconColor = Color(0xFFA32D2D),
            icon = Icons.Default.Warning,
            title = "Profile not approved",
            subtitle = state.rejectionReason.ifBlank {
                "Your profile was not approved. Please review your details and update your profile."
            }
        )
        else -> return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = iconColor)
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = iconColor.copy(alpha = 0.8f)
                )
                if (state.approvalStatus == ApprovalStatus.REJECTED) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Go to Profile tab to update and resubmit.",
                        fontSize = 12.sp,
                        color = iconColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Simple data holder to avoid destructuring mismatches
data class BannerData(
    val bg: Color,
    val border: Color,
    val iconColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String
)

operator fun BannerData.component1() = bg
operator fun BannerData.component2() = border
operator fun BannerData.component3() = iconColor
operator fun BannerData.component4() = icon
operator fun BannerData.component5() = title
operator fun BannerData.component6() = subtitle