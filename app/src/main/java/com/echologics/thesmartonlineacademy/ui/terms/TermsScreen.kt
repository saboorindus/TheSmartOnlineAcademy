package com.echologics.thesmartonlineacademy.ui.terms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.ui.common.components.PrimaryButton
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
    role: String,
    onAccepted: () -> Unit,   // navigate to actual signup form
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    var accepted by remember { mutableStateOf(false) }

    // Only enable Accept button once user has scrolled near the bottom
    val hasScrolledToBottom by remember {
        derivedStateOf {
            scrollState.value >= (scrollState.maxValue - 200).coerceAtLeast(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & conditions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // Scrollable terms body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    "The Smart Online Academy",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Terms & Conditions for ${if (role == "teacher") "Teachers" else "Students"}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    textAlign = TextAlign.Center
                )

                TermsSection("1. Acceptance of terms") {
                    "By creating an account on The Smart Online Academy, you confirm that you have read, understood, and agree to be bound by these Terms and Conditions. If you do not agree to these terms, you may not use this platform."
                }

                TermsSection("2. Eligibility") {
                    if (role == "teacher") {
                        "You must be at least 18 years old to register as a teacher. By registering, you confirm that all information provided during onboarding is accurate, complete, and truthful. Providing false credentials or misrepresenting your qualifications will result in immediate account suspension."
                    } else {
                        "You must be at least 13 years old to register as a student. Users under 18 should have parental or guardian consent before creating an account. By registering, you confirm that the information provided is accurate."
                    }
                }

                TermsSection("3. Platform conduct") {
                    "All users agree to:\n\n• Treat all other users with respect and professionalism\n• Not engage in harassment, discrimination, or abusive behaviour\n• Not share session recordings without the explicit consent of all participants\n• Not use the platform for any illegal or unauthorised purpose\n• Not attempt to contact other users outside the platform to avoid fees"
                }

                if (role == "teacher") {
                    TermsSection("4. Teacher responsibilities") {
                        "As a registered teacher, you agree to:\n\n• Provide sessions as advertised and on time\n• Maintain the quality and accuracy of your listed subjects and qualifications\n• Not cancel sessions without reasonable notice of at least 24 hours\n• Accept that your profile is subject to review and approval by platform administrators\n• Understand that the platform retains a service fee from each session payment\n• Issue refunds or reschedule sessions in cases of technical failure on your end"
                    }

                    TermsSection("5. Payment terms") {
                        "Payments are processed through the platform's manual QR-based payment system (EasyPaisa / JazzCash). Teachers must confirm payment receipt within 48 hours of submission. Disputes regarding payments must be reported within 7 days of the session."
                    }
                } else {
                    TermsSection("4. Student responsibilities") {
                        "As a registered student, you agree to:\n\n• Make payments honestly and only submit genuine transaction IDs\n• Attend booked sessions on time or notify the teacher at least 24 hours in advance if unable to attend\n• Not share session materials without the teacher's permission\n• Leave honest and fair reviews based on your actual experience\n• Not request refunds for sessions you have attended"
                    }

                    TermsSection("5. Payment terms") {
                        "All session fees are paid upfront via the platform's QR-based payment system. Payments are held pending teacher confirmation. Refund requests must be submitted within 48 hours of a missed or cancelled session and are subject to review."
                    }
                }

                TermsSection("6. Session recording") {
                    "Sessions may be recorded by the platform for quality assurance purposes only. Participants may save session recordings for personal study use. Sharing recordings publicly or commercially without written consent from all parties is strictly prohibited."
                }

                TermsSection("7. Intellectual property") {
                    "All content created by teachers on this platform remains the intellectual property of the teacher. Students may use session materials for personal learning only and may not redistribute, sell, or publish them without permission."
                }

                TermsSection("8. Privacy") {
                    "Your personal information is collected and processed in accordance with our Privacy Policy. We do not sell your data to third parties. Your contact details will not be shared with other users outside of what is necessary to facilitate sessions."
                }

                TermsSection("9. Account termination") {
                    "The Smart Online Academy reserves the right to suspend or terminate any account that violates these terms, engages in fraudulent activity, or causes harm to other users. Terminated accounts forfeit any pending payments or credits."
                }

                TermsSection("10. Limitation of liability") {
                    "The Smart Online Academy is a platform facilitating connections between teachers and students. We are not liable for the quality of teaching, learning outcomes, or disputes between users beyond what is stated in our refund policy. We are not responsible for technical failures caused by third-party services including Agora, Firebase, or payment providers."
                }

                TermsSection("11. Changes to terms") {
                    "We reserve the right to update these terms at any time. Continued use of the platform after changes are published constitutes acceptance of the new terms. Users will be notified of material changes via email or in-app notification."
                }

                TermsSection("12. Governing law") {
                    "These terms are governed by the laws of Pakistan. Any disputes shall be resolved through good-faith negotiation or, if necessary, through the appropriate legal jurisdiction."
                }

                Spacer(Modifier.height(8.dp))

                // Scroll hint
                if (!hasScrolledToBottom) {
                    Text(
                        "Scroll to the bottom to accept",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            // Fixed bottom acceptance area
            Surface(
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Checkbox row
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Checkbox(
                            checked = accepted,
                            onCheckedChange = { if (hasScrolledToBottom) accepted = it },
                            enabled = hasScrolledToBottom,
                            colors = CheckboxDefaults.colors(
                                checkedColor = Purple,
                                uncheckedColor = if (hasScrolledToBottom)
                                    MaterialTheme.colorScheme.outline
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "I have read and agree to the Terms and Conditions of The Smart Online Academy",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = if (hasScrolledToBottom)
                                MaterialTheme.colorScheme.onBackground
                            else
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    PrimaryButton(
                        text = "Continue to sign up",
                        onClick = onAccepted,
                        enabled = accepted && hasScrolledToBottom
                    )
                }
            }
        }
    }
}

@Composable
private fun TermsSection(title: String, body: () -> String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
    )
    Text(
        text = body(),
        fontSize = 13.sp,
        lineHeight = 21.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
    )
    HorizontalDivider(
        modifier = Modifier.padding(top = 14.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline
    )
}