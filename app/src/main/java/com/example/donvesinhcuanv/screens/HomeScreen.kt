package com.example.donvesinhcuanv.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.donvesinhcuanv.data.Job
import com.example.donvesinhcuanv.viewmodel.JobViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: JobViewModel,
    onNavigateToJobList: () -> Unit
) {
    val worker by viewModel.currentWorker.collectAsState()
    val pendingJobs by viewModel.pendingJobs.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    
    // Refresh worker data when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshWorkerData()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header với trạng thái online/offline
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Xin chào 👋",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = worker?.name ?: "Nhân viên",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Thông báo",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Online/Offline Toggle
                OnlineToggle(
                    isOnline = isOnline,
                    onToggle = { viewModel.toggleOnlineStatus() }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Thu nhập hôm nay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    EarningsCard(
                        title = "Hôm nay",
                        amount = worker?.todayEarnings ?: 0,
                        icon = Icons.Default.DateRange
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    EarningsCard(
                        title = "Tổng thu nhập",
                        amount = worker?.totalEarnings ?: 0,
                        icon = Icons.Default.AccountBalanceWallet
                    )
                }
            }
        }
        
        // Danh sách đơn hàng
        if (isOnline) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đơn hàng mới",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (pendingJobs.isNotEmpty()) {
                            Surface(
                                color = Color(0xFFFF5722),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "${pendingJobs.size}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                if (pendingJobs.isEmpty()) {
                    item {
                        EmptyJobsState()
                    }
                } else {
                    items(pendingJobs) { job ->
                        NewJobCard(
                            job = job,
                            onAccept = { viewModel.acceptJob(job) }
                        )
                    }
                }
            }
        } else {
            // Offline state
            OfflineState()
        }
    }
}

@Composable
fun OnlineToggle(isOnline: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isOnline) Color(0xFF4CAF50) else Color(0xFFE0E0E0)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (isOnline) Color.White else Color.Gray,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isOnline) "Đang online" else "Đang offline",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOnline) Color.White else Color.Gray
                    )
                    Text(
                        text = if (isOnline) "Sẵn sàng nhận đơn" else "Không nhận đơn mới",
                        fontSize = 12.sp,
                        color = if (isOnline) Color.White.copy(alpha = 0.9f) else Color.Gray
                    )
                }
            }
            
            Switch(
                checked = isOnline,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF2E7D32),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray
                )
            )
        }
    }
}

@Composable
fun RowScope.EarningsCard(title: String, amount: Int, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF1F8F4)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${String.format("%,d", amount)}đ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
fun NewJobCard(job: Job, onAccept: () -> Unit) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (job.isNew) 1.02f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (job.isNew) Color(0xFFFFF3E0) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = job.icon, fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = job.serviceName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = job.customerName,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                if (job.isNew) {
                    Surface(
                        color = Color(0xFFFF5722),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "MỚI",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Địa chỉ và khoảng cách
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = job.address,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${job.distance} km",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = Color(0xFF2196F3),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Thời gian
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Thời gian: ${dateFormat.format(job.scheduledDate)}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Divider(color = Color.LightGray.copy(alpha = 0.3f))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Giá và nút nhận đơn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Thu nhập",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${String.format("%,d", job.price)}đ",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NHẬN ĐƠN",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyJobsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Đang tìm đơn hàng...",
            fontSize = 16.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Chúng tôi sẽ thông báo khi có đơn mới",
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun OfflineState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.WifiOff,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Bạn đang offline",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Bật trạng thái online để nhận đơn hàng mới",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}
