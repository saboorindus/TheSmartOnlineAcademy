package com.echologics.thesmartonlineacademy.ui.teacher.dashboard

import com.echologics.thesmartonlineacademy.data.model.ApprovalStatus
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.data.repository.AdminRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ApprovalBannerState(
    val approvalStatus: ApprovalStatus? = null,
    val rejectionReason: String = "",
    val notificationMessage: String = "",
    val isLoading: Boolean = true
)

class ApprovalBannerViewModel(
    private val adminRepo: AdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ApprovalBannerState())
    val state: StateFlow<ApprovalBannerState> = _state.asStateFlow()

    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("teachers").document(uid).get().await()
                val profile = doc.toObject(TeacherProfile::class.java)

                val notif = adminRepo.getApprovalNotification(uid)
                val message = notif?.get("message") as? String ?: ""

                _state.value = _state.value.copy(
                    approvalStatus = profile?.approvalStatus,
                    rejectionReason = profile?.let {
                        doc.getString("rejectionReason") ?: ""
                    } ?: "",
                    notificationMessage = message,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun refresh() = load()
}
