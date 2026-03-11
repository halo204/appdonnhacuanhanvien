package com.example.donvesinhcuanv.viewmodel

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.donvesinhcuanv.data.AppConfig
import com.example.donvesinhcuanv.data.FirebaseRepository
import com.example.donvesinhcuanv.data.WorkerStatus
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object OtpSent : AuthState()
    data class Success(val userId: String, val isNewUser: Boolean) : AuthState()
    data class Error(val message: String) : AuthState()
    data class WorkerStatusChanged(val status: WorkerStatus) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val firebaseRepository = if (AppConfig.USE_FIREBASE) FirebaseRepository() else null
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private var verificationId: String = ""
    
    init {
        checkAuthStatus()
    }
    
    private fun checkAuthStatus() {
        if (AppConfig.USE_FIREBASE) {
            val currentUser = firebaseRepository?.getCurrentUser()
            _isLoggedIn.value = currentUser != null
            if (currentUser != null) {
                // Observe worker status
                observeWorkerStatus(currentUser.uid)
            }
        }
    }
    
    // Step 1: Send OTP to phone number
    fun sendOtp(phoneNumber: String, activity: Activity) {
        if (!AppConfig.USE_FIREBASE) {
            _authState.value = AuthState.Error("Firebase chưa được bật")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            firebaseRepository?.sendOtp(
                phoneNumber = phoneNumber,
                activity = activity,
                onCodeSent = { verId ->
                    verificationId = verId
                    _authState.value = AuthState.OtpSent
                },
                onVerificationCompleted = { credential ->
                    // Auto verification (rare)
                    signInWithCredential(credential)
                },
                onVerificationFailed = { exception ->
                    _authState.value = AuthState.Error(
                        exception.message ?: "Gửi OTP thất bại"
                    )
                }
            )
        }
    }
    
    // Step 2: Verify OTP code
    fun verifyOtp(otpCode: String) {
        if (!AppConfig.USE_FIREBASE) {
            _authState.value = AuthState.Error("Firebase chưa được bật")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = firebaseRepository?.verifyOtp(verificationId, otpCode)
            
            if (result?.isSuccess == true) {
                val user = result.getOrThrow()
                checkUserProfile(user.uid)
            } else {
                _authState.value = AuthState.Error(
                    result?.exceptionOrNull()?.message ?: "OTP không đúng"
                )
            }
        }
    }
    
    // Sign in with credential (for auto-verification)
    private fun signInWithCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = firebaseRepository?.signInWithCredential(credential)
            
            if (result?.isSuccess == true) {
                val user = result.getOrThrow()
                checkUserProfile(user.uid)
            } else {
                _authState.value = AuthState.Error(
                    result?.exceptionOrNull()?.message ?: "Đăng nhập thất bại"
                )
            }
        }
    }
    
    // Step 3: Check if user profile exists
    private fun checkUserProfile(userId: String) {
        viewModelScope.launch {
            val result = firebaseRepository?.getWorker(userId)
            
            if (result?.isSuccess == true) {
                // Existing user
                _isLoggedIn.value = true
                val worker = result.getOrThrow()
                _authState.value = AuthState.WorkerStatusChanged(worker.status)
                observeWorkerStatus(userId)
            } else {
                // New user - need to complete registration
                _authState.value = AuthState.Success(userId, isNewUser = true)
            }
        }
    }
    
    // Step 4: Complete registration (no images needed)
    fun completeRegistration(
        userId: String,
        name: String,
        phone: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            // Create worker profile
            val createResult = firebaseRepository?.createWorkerProfile(
                userId = userId,
                name = name,
                phone = phone
            )
            
            if (createResult?.isSuccess == true) {
                _isLoggedIn.value = true
                _authState.value = AuthState.WorkerStatusChanged(WorkerStatus.PENDING)
                observeWorkerStatus(userId)
            } else {
                _authState.value = AuthState.Error("Tạo tài khoản thất bại")
            }
        }
    }
    
    // Observe worker status changes (for admin approval)
    private fun observeWorkerStatus(userId: String) {
        viewModelScope.launch {
            firebaseRepository?.observeWorkerStatus(userId)?.collect { status ->
                _authState.value = AuthState.WorkerStatusChanged(status)
            }
        }
    }
    
    // Legacy email/password sign in (keep for backward compatibility)
    fun signUp(email: String, password: String, fullName: String, phone: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = firebaseRepository?.signUp(email, password, fullName, phone)
            
            _authState.value = if (result?.isSuccess == true) {
                _isLoggedIn.value = true
                val user = result.getOrThrow()
                AuthState.Success(user.uid, isNewUser = false)
            } else {
                AuthState.Error(result?.exceptionOrNull()?.message ?: "Đăng ký thất bại")
            }
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val result = firebaseRepository?.signIn(email, password)
            
            _authState.value = if (result?.isSuccess == true) {
                _isLoggedIn.value = true
                val user = result.getOrThrow()
                checkUserProfile(user.uid)
                AuthState.Success(user.uid, isNewUser = false)
            } else {
                AuthState.Error(result?.exceptionOrNull()?.message ?: "Đăng nhập thất bại")
            }
        }
    }
    
    fun signOut() {
        firebaseRepository?.signOut()
        _isLoggedIn.value = false
        _authState.value = AuthState.Idle
    }
    
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}
