package com.echologics.thesmartonlineacademy.ui.terms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple

@Composable
fun TermsDialog(
    role: String,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,  // allows full width
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 12.dp, vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Dialog header ─────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Terms & Conditions",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )

                // ── Scrollable content ────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Text(
                        "The Smart Online Academy",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "For ${if (role == "teacher") "Teachers" else "Students"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )

                    TermsSectionItem("1. Acceptance of terms",
                        "By creating an account on The Smart Online Academy, you confirm that you have read, understood, and agree to be bound by these Terms and Conditions. If you do not agree, you may not use this platform."
                    )

                    TermsSectionItem("2. Eligibility",
                        if (role == "teacher")
                            "You must be at least 18 years old to register as a teacher. All information provided during onboarding must be accurate and truthful. Misrepresenting qualifications will result in immediate account suspension."
                        else
                            "You must be at least 13 years old to register as a student. Users under 18 should have parental or guardian consent. By registering, you confirm that all information provided is accurate."
                    )

                    TermsSectionItem("3. Platform conduct",
                        "All users agree to:\n\n• Treat all other users with respect and professionalism\n• Not engage in harassment, discrimination, or abusive behaviour\n• Not share session recordings without consent of all participants\n• Not use the platform for any illegal or unauthorised purpose\n• Not attempt to contact other users outside the platform to avoid fees"
                    )

                    if (role == "teacher") {
                        TermsSectionItem("4. Teacher responsibilities",
                            "As a registered teacher, you agree to:\n\n• Provide sessions as advertised and on time\n• Maintain accuracy of your listed subjects and qualifications\n• Not cancel sessions without at least 24 hours notice\n• Accept that your profile is subject to admin review and approval\n• Understand that the platform retains a service fee from each session\n• Issue refunds or reschedule in cases of technical failure on your end"
                        )
                        TermsSectionItem("5. Payment terms",
                            "Payments are processed through EasyPaisa / JazzCash QR. Teachers must confirm payment receipt within 48 hours. Disputes must be reported within 7 days of the session."
                        )
                    } else {
                        TermsSectionItem("4. Student responsibilities",
                            "As a registered student, you agree to:\n\n• Make payments honestly and only submit genuine transaction IDs\n• Attend booked sessions on time, or notify the teacher at least 24 hours in advance\n• Not share session materials without the teacher's permission\n• Leave honest reviews based on your actual experience\n• Not request refunds for sessions you have attended"
                        )
                        TermsSectionItem("5. Payment terms",
                            "All session fees are paid upfront via QR-based payment. Payments are held pending teacher confirmation. Refund requests must be submitted within 48 hours of a missed or cancelled session."
                        )
                    }

                    TermsSectionItem("6. Session recording",
                        "Sessions may be recorded for quality assurance. Participants may save recordings for personal study. Sharing recordings publicly without written consent is strictly prohibited."
                    )

                    TermsSectionItem("7. Intellectual property",
                        "Content created by teachers remains their intellectual property. Students may use session materials for personal learning only and may not redistribute or publish them without permission."
                    )

                    TermsSectionItem("8. Privacy",
                        "Your personal information is collected in accordance with our Privacy Policy. We do not sell your data to third parties. Contact details are not shared beyond what is necessary to facilitate sessions."
                    )

                    TermsSectionItem("9. Account termination",
                        "The Smart Online Academy reserves the right to suspend or terminate accounts that violate these terms, engage in fraudulent activity, or cause harm to other users. Terminated accounts forfeit any pending payments."
                    )

                    TermsSectionItem("10. Limitation of liability",
                        "We are not liable for the quality of teaching, learning outcomes, or disputes beyond our refund policy. We are not responsible for technical failures caused by third-party services including Agora, Firebase, or payment providers."
                    )

                    TermsSectionItem("11. Changes to terms",
                        "We reserve the right to update these terms at any time. Continued use of the platform constitutes acceptance. Users will be notified of material changes via email or in-app notification."
                    )

                    TermsSectionItem("12. Governing law",
                        "These terms are governed by the laws of Pakistan. Any disputes shall be resolved through good-faith negotiation or, if necessary, through the appropriate legal jurisdiction."
                    )

                    Spacer(Modifier.height(8.dp))
                }

                // ── Close button at bottom ────────────────────────────────────
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Text("Close", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun TermsSectionItem(title: String, body: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 14.dp, bottom = 5.dp)
    )
    Text(
        text = body,
        fontSize = 12.sp,
        lineHeight = 19.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
    )
    HorizontalDivider(
        modifier = Modifier.padding(top = 12.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}