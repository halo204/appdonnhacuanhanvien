package com.example.donvesinhcuanv.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donvesinhcuanv.data.WorkerStatus
import com.example.donvesinhcuanv.viewmodel.AuthState
import com.example.donvesinhcuanv.viewmodel.AuthViewModel

@Composable
fun PhoneRegisterScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity
    
    var currentStep by remember { mutableStateOf(RegistrationStep.PHONE) }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.OtpSent -> {
                currentStep = RegistrationStep.OTP
            }
            is AuthState.Success -> {
                val state = authState as AuthState.Success
                userId = state.userId
                if (state.isNewUser) {
                    currentStep = RegistrationStep.PROFILE
                } else {
                    currentStep = RegistrationStep.PENDING
                }
            }
            is AuthState.WorkerStatusChanged -> {
                val status = (authState as AuthState.WorkerStatusChanged).status
                when (status) {
                    WorkerStatus.PENDING -> currentStep = RegistrationStep.PENDING
                    WorkerStatus.APPROVED -> onNavigateToHome()
                    WorkerStatus.REJECTED -> currentStep = RegistrationStep.REJECTED
                }
            }
            else -> {}
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // Logo/Title
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Đăng ký nhân viên",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = when (currentStep) {
                RegistrationStep.PHONE -> "Nhập số điện thoại của bạn"
                RegistrationStep.OTP -> "Nhập mã OTP đã gửi đến $phoneNumber"
                RegistrationStep.PROFILE -> "Nhập họ tên của bạn"
                RegistrationStep.PENDING -> "Chờ admin phê duyệt"
                RegistrationStep.REJECTED -> "Tài khoản bị từ chối"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Content based on step
        when (currentStep) {
            RegistrationStep.PHONE -> PhoneInputStep(
                phoneNumber = phoneNumber,
                onPhoneChange = { phoneNumber = it },
                isLoading = authState is AuthState.Loading,
                onSendOtp = {
                    authViewModel.sendOtp(phoneNumber, activity)
                }
            )
            
            RegistrationStep.OTP -> OtpInputStep(
                otpCode = otpCode,
                onOtpChange = { otpCode = it },
                isLoading = authState is AuthState.Loading,
                onVerifyOtp = {
                    authViewModel.verifyOtp(otpCode)
                },
                onResendOtp = {
                    authViewModel.sendOtp(phoneNumber, activity)
                }
            )
            
            RegistrationStep.PROFILE -> ProfileInputStep(
                fullName = fullName,
                onNameChange = { fullName = it },
                isLoading = authState is AuthState.Loading,
                onSubmit = {
                    authViewModel.completeRegistration(
                        userId = userId,
                        name = fullName,
                        phone = phoneNumber
                    )
                }
            )
            
            RegistrationStep.PENDING -> PendingApprovalStep()
            
            RegistrationStep.REJECTED -> RejectedStep(
                onTryAgain = {
                    currentStep = RegistrationStep.PHONE
                    authViewModel.resetAuthState()
                }
            )
        }
        
        // Error message
        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Back to login
        TextButton(onClick = onNavigateToLogin) {
            Text("Đã có tài khoản? Đăng nhập")
        }
    }
}

enum class RegistrationStep {
    PHONE, OTP, PROFILE, PENDING, REJECTED
}

@Composable
fun PhoneInputStep(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    isLoading: Boolean,
    onSendOtp: () -> Unit
) {
    OutlinedTextField(
        value = phoneNumber,
        onValueChange = onPhoneChange,
        label = { Text("Số điện thoại") },
        placeholder = { Text("0901234567") },
        leadingIcon = { Icon(Icons.Default.Phone, null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Button(
        onClick = onSendOtp,
        modifier = Modifier.fillMaxWidth(),
        enabled = phoneNumber.length >= 10 && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Gửi mã OTP")
        }
    }
}

@Composable
fun OtpInputStep(
    otpCode: String,
    onOtpChange: (String) -> Unit,
    isLoading: Boolean,
    onVerifyOtp: () -> Unit,
    onResendOtp: () -> Unit
) {
    OutlinedTextField(
        value = otpCode,
        onValueChange = { if (it.length <= 6) onOtpChange(it) },
        label = { Text("Mã OTP") },
        placeholder = { Text("123456") },
        leadingIcon = { Icon(Icons.Default.Lock, null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onResendOtp) {
            Text("Gửi lại mã OTP")
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Button(
        onClick = onVerifyOtp,
        modifier = Modifier.fillMaxWidth(),
        enabled = otpCode.length == 6 && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Xác nhận OTP")
        }
    }
}

@Composable
fun ProfileInputStep(
    fullName: String,
    onNameChange: (String) -> Unit,
    isLoading: Boolean,
    onSubmit: () -> Unit
) {
    OutlinedTextField(
        value = fullName,
        onValueChange = onNameChange,
        label = { Text("Họ và tên") },
        leadingIcon = { Icon(Icons.Default.Person, null) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Button(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        enabled = fullName.isNotBlank() && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Hoàn tất đăng ký")
        }
    }
}

@Composable
fun PendingApprovalStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Đang chờ phê duyệt",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Tài khoản của bạn đang được admin xem xét. Bạn sẽ nhận được thông báo khi được phê duyệt.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        CircularProgressIndicator()
    }
}

@Composable
fun RejectedStep(onTryAgain: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Cancel,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Tài khoản bị từ chối",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Tài khoản của bạn không được phê duyệt. Vui lòng liên hệ admin để biết thêm chi tiết.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onTryAgain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Đăng ký lại")
        }
    }
}
