package com.example.werun.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.werun.ui.components.NavigationDrawerContent
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.WbSunny  // Icon nắng, thay bằng weather icon lib nếu cần
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
private fun getWeatherIcon(code: Int): ImageVector {
    return when (code) {
        0 -> Icons.Default.WbSunny  // Nắng
        1, 2, 3 -> Icons.Default.Cloud  // Mây
        45, 48 -> Icons.Default.Cloud  // Sương mù (nếu có Foggy, hoặc dùng CloudOutlined)
        51, 53, 55 -> Icons.Default.WaterDrop  // Mưa phùn
        61, 63, 65 -> Icons.Default.WaterDrop  // Mưa
        71, 73, 75 -> Icons.Default.AcUnit  // Tuyết
        95, 96, 99 -> Icons.Default.FlashOn  // Bão
        else -> Icons.Default.WaterDrop  // Không rõ
    }
}
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(factory = provideHomeViewModelFactory())
) {
    val tag = "HomeScreen"
    Log.d(tag, "HomeScreen recomposed")

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    // Log trạng thái để debug
    Log.d(tag, "uiState: isLoading=${uiState.isLoading}, user=${uiState.user}, stats=${uiState.stats}, error=${uiState.error}")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                navController = navController,
                drawerState = drawerState
            )
        }
    ) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                HomeTopBar(
                    userName = uiState.user?.fullName ?: "WeRun",
                    onMenuClick = {
                        scope.launch {
                            Log.d(tag, "Opening navigation drawer")
                            drawerState.open()
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Main content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))

                        // Goal Circle and Map
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val goal = remember(uiState.stats.todayGoal) {
                                if (uiState.stats.todayGoal.isFinite())
                                    (uiState.stats.todayGoal / 1000).toFloat()
                                else 0f
                            }
                            val progress = remember(uiState.stats.goalProgress) {
                                if (uiState.stats.goalProgress.isFinite())
                                    uiState.stats.goalProgress.coerceIn(0f, 1f)
                                else 0f
                            }
                            GoalCircleProgress(goal = goal, progress = progress)
                            MapPlaceholder(
                                lastLat = uiState.user?.lastRunLat ?: 0.0,
                                lastLng = uiState.user?.lastRunLng ?: 0.0
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.weatherState.isLoading) {
                                CircularProgressIndicator(color = Color.Gray, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = getWeatherIcon(uiState.weatherState.weatherCode),  // Thay đổi dòng này
                                    contentDescription = "Weather",
                                    tint = Color(0xFFFFD700),  // Giữ tint, hoặc đổi theo temp nếu muốn
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = uiState.weatherState.temperature,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = uiState.weatherState.condition,
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            uiState.weatherState.error?.let {
                                Text(text = it, color = Color.Red, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Today's Goal
                        Text(
                            text = "Today's Goal",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        Text(
                            text = "Run ${
                                String.format(
                                    "%.1f",
                                    if (uiState.stats.todayGoal.isFinite())
                                        uiState.stats.todayGoal / 1000
                                    else 0.0
                                )
                            } KM",
                            fontSize = 20.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Motivational Message
                        Text(
                            text = uiState.motivationalMessage.takeIf { it.isNotEmpty() }
                                ?: "Let's start your journey!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Statistics
                        StatisticsRow(
                            totalDistance = if (uiState.stats.totalDistance.isFinite())
                                uiState.stats.totalDistance / 1000
                            else 0.0,
                            bestPace = uiState.stats.bestPace.takeIf { it.isNotEmpty() }
                                ?: "0:00",
                            consecutiveDays = uiState.stats.consecutiveDays.coerceAtLeast(0)
                        )
                    }

                    // Bottom Controls
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RunControls(
                            onStartClick = {
                                Log.d(tag, "Start run clicked")
                                viewModel.onEvent(HomeUiEvent.StartRun)
                                navController.navigate("run")
                            },
                            onSettingsClick = {
                                Log.d(tag, "Settings clicked")
                                viewModel.onEvent(HomeUiEvent.OpenSettings)
                                navController.navigate("settings")
                            },
                            onMusicClick = {
                                Log.d(tag, "Music clicked")
                                viewModel.onEvent(HomeUiEvent.OpenMusic)
                            }
                        )
                    }
                }

                // Loading indicator
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFFC4FF53)
                    )
                }

                // Error message
                uiState.error?.let { error ->
                    Log.d(tag, "Showing error: $error")
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Text(text = error.takeIf { it.isNotEmpty() } ?: "An error occurred")
                    }
                }
            }
        }
    }
}

@Composable
fun GoalCircleProgress(goal: Float, progress: Float) {
    val accentColor = Color(0xFFC4FF53)

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                style = Stroke(width = 12.dp.toPx())
            )
        }

        // Progress circle
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Goal text
        Text(
            text = String.format("%.1f", goal.takeIf { it.isFinite() } ?: 0f),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
fun MapPlaceholder(lastLat: Double, lastLng: Double) {
    MapHome(
        lastLat = lastLat,
        lastLng = lastLng,
        modifier = Modifier
            .size(180.dp, 140.dp)
            .clip(RoundedCornerShape(16.dp))
    )
}

@Composable
fun StatisticsRow(
    totalDistance: Double,
    bestPace: String,
    consecutiveDays: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            value = String.format("%.1f", totalDistance.takeIf { it.isFinite() } ?: 0.0),
            label = "KM"
        )
        StatItem(
            value = bestPace,
            label = "BEST PACE"
        )
        StatItem(
            value = consecutiveDays.toString(),
            label = "CONSECUTIVE\nDAYS"
        )
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(userName: String, onMenuClick: () -> Unit = {}) {
    TopAppBar(
        title = {
            Text(
                text = userName.takeIf { it.isNotEmpty() } ?: "WeRun",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.Black
                )
            }
        },
        actions = {
            Spacer(modifier = Modifier.width(68.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Composable
fun RunControls(
    onStartClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMusicClick: () -> Unit
) {
    val accentColor = Color(0xFFC4FF53)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF424242), shape = RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            borderColor = accentColor,
            onClick = onSettingsClick
        )

        StartRunButton(
            backgroundColor = accentColor,
            onClick = onStartClick
        )

        RoundIconButton(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Music",
            borderColor = accentColor,
            onClick = onMusicClick
        )
    }
}

@Composable
fun RoundIconButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    borderColor: Color,
    onClick: () -> Unit = {}
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .border(BorderStroke(3.dp, borderColor), CircleShape)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun StartRunButton(
    backgroundColor: Color,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(90.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.Black
        )
    ) {
        Text(
            text = "START",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController())
}