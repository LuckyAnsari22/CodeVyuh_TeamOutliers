package com.greeniq.app.ui.anomaly

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

// Theme Colors
val BgPrimary = Color(0xFF09090E)
val BgCard = Color(0xFF131520)
val AccentCyan = Color(0xFF07D8F6)
val CriticalRed = Color(0xFFFF3B30)
val HighAmber = Color(0xFFFF8C00)
val MediumYellow = Color(0xFFFFD60A)
val HealthGreen = Color(0xFF00FF88)
val GlassBorder = Color(0x33FFFFFF)

enum class AnomalyType {
    IDLING_WASTE, SPEED_INEFFICIENCY, OVERLOAD_DETECTED, ROUTE_DEVIATION, COLD_START_SEQUENCE, PREDICTIVE_MAINTENANCE
}

enum class Severity(val color: Color, val label: String) {
    CRITICAL(CriticalRed, "CRITICAL"),
    HIGH(HighAmber, "HIGH"),
    MEDIUM(MediumYellow, "MEDIUM")
}

data class Anomaly(
    val id: String,
    val truckId: String,
    val type: AnomalyType,
    val severity: Severity,
    val message: String,
    val co2ImpactKg: Double,
    val timestamp: String,
    var resolved: Boolean = false,
    var fadingOut: Boolean = false
)

fun generateRandomAnomaly(): Anomaly {
    val types = AnomalyType.values()
    val trucks = listOf("MH-04-AB-1234", "GJ-05-CD-5678", "KA-01-EF-9012", "UP-16-XY-9876", "TN-09-CD-1111")
    val type = types.random()
    val truck = trucks.random()
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    
    val (severity, message, co2) = when (type) {
        AnomalyType.IDLING_WASTE -> Triple(
            Severity.MEDIUM, 
            "Truck $truck idling 18 min at Nashik checkpoint. Wasting 0.24L diesel = 0.64 kg CO₂. [Idling EF: ARAI 2022]", 
            0.64
        )
        AnomalyType.SPEED_INEFFICIENCY -> Triple(
            Severity.HIGH,
            "Truck $truck at 94 km/h. Fuel efficiency drops 23% above 80 km/h. +2.1 kg CO₂ excess this hour.",
            2.1
        )
        AnomalyType.OVERLOAD_DETECTED -> Triple(
            Severity.CRITICAL,
            "Axle sensor: actual 26.8T vs declared 22T on manifest. Illegal overload +18% emission excess. [MoRTH regs]",
            4.8
        )
        AnomalyType.ROUTE_DEVIATION -> Triple(
            Severity.HIGH,
            "Truck $truck deviated 34km from optimal route. +26.8 kg excess CO₂ vs planned. [GREENIQ Opt M2]",
            26.8
        )
        AnomalyType.COLD_START_SEQUENCE -> Triple(
            Severity.MEDIUM,
            "Multiple cold starts detected. Cold start = 3× normal emission for first 5 min. [ARAI BS-VI, 2022]",
            1.2
        )
        AnomalyType.PREDICTIVE_MAINTENANCE -> Triple(
            Severity.HIGH,
            "Engine temp 103°C on $truck. Efficiency degrading — predict 15% emission increase if not serviced.",
            3.5
        )
    }

    return Anomaly(
        id = UUID.randomUUID().toString(),
        truckId = truck,
        type = type,
        severity = severity,
        message = message,
        co2ImpactKg = co2,
        timestamp = time
    )
}

@Composable
fun AnomalyScreen() {
    val anomalies = remember { mutableStateListOf<Anomaly>() }
    var activeCount by remember { mutableIntStateOf(0) }
    var resolvedCount by remember { mutableIntStateOf(0) }
    var totalCo2Excess by remember { mutableDoubleStateOf(0.0) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    // Simulation Coroutine
    LaunchedEffect(Unit) {
        // Initial anomalies
        anomalies.add(generateRandomAnomaly())
        delay(2000)
        anomalies.add(generateRandomAnomaly())
        
        while (true) {
            val delayMs = Random.nextLong(8000, 15000)
            delay(delayMs)
            val newAnomaly = generateRandomAnomaly()
            anomalies.add(0, newAnomaly) // Add to top
            
            // Keep list manageable
            if (anomalies.size > 20) anomalies.removeLast()
        }
    }

    // Update summary stats
    LaunchedEffect(anomalies.toList()) {
        activeCount = anomalies.count { !it.resolved && !it.fadingOut }
        totalCo2Excess = anomalies.filter { !it.resolved && !it.fadingOut }.sumOf { it.co2ImpactKg }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = BgPrimary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header
            Text("IoT FLEET MONITOR", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Live Anomaly Alerts", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Top Summary Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard, RoundedCornerShape(8.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryStat("$activeCount", "Active Alerts", activeCount > 0)
                Divider(modifier = Modifier.height(24.dp).width(1.dp), color = Color.DarkGray)
                SummaryStat("+${String.format("%.1f", totalCo2Excess)} kg", "CO₂ Excess/hr", totalCo2Excess > 0, CriticalRed)
                Divider(modifier = Modifier.height(24.dp).width(1.dp), color = Color.DarkGray)
                SummaryStat("$resolvedCount", "Auto-Resolved", false, HealthGreen)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Anomaly Feed
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(anomalies, key = { it.id }) { anomaly ->
                    AnimatedVisibility(
                        visible = !anomaly.fadingOut,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(animationSpec = tween(500))
                    ) {
                        AnomalyCard(
                            anomaly = anomaly,
                            onResolve = { 
                                val index = anomalies.indexOfFirst { it.id == anomaly.id }
                                if (index != -1) {
                                    val updated = anomalies[index].copy(resolved = true)
                                    anomalies[index] = updated
                                    resolvedCount++
                                }
                            },
                            onAlertDriver = {
                                snackbarHostState.currentSnackbarData?.dismiss()
                            },
                            onFadeOut = {
                                val index = anomalies.indexOfFirst { it.id == anomaly.id }
                                if (index != -1) {
                                    anomalies[index] = anomalies[index].copy(fadingOut = true)
                                }
                            },
                            snackbarHostState = snackbarHostState
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStat(value: String, label: String, isAlert: Boolean, customColor: Color? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value, 
            color = customColor ?: (if (isAlert) HighAmber else Color.White), 
            fontSize = 16.sp, 
            fontWeight = FontWeight.Bold
        )
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun AnomalyCard(
    anomaly: Anomaly, 
    onResolve: () -> Unit, 
    onAlertDriver: () -> Unit,
    onFadeOut: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-dismiss logic after resolving
    LaunchedEffect(anomaly.resolved) {
        if (anomaly.resolved) {
            delay(5000) // 5 seconds wait before fade out
            onFadeOut()
        }
    }

    val alpha by animateFloatAsState(targetValue = if (anomaly.resolved) 0.5f else 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .alpha(alpha)
            .background(BgCard, RoundedCornerShape(12.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Left color bar
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(if (anomaly.resolved) HealthGreen else anomaly.severity.color)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Severity + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (anomaly.resolved) {
                        Text("RESOLVED", color = HealthGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(HealthGreen.copy(alpha=0.2f), RoundedCornerShape(4.dp)).padding(horizontal=6.dp, vertical=2.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        val badgeColor = anomaly.severity.color.copy(alpha = 0.2f)
                        Text(
                            anomaly.severity.label, 
                            color = anomaly.severity.color, 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(badgeColor, RoundedCornerShape(4.dp)).padding(horizontal=6.dp, vertical=2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(anomaly.type.name.replace("_", " "), color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Text(anomaly.timestamp, color = Color.Gray, fontSize = 10.sp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Message
            Text(anomaly.message, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Impact
            Text("+${anomaly.co2ImpactKg} kg CO₂ excess", color = if (anomaly.resolved) Color.Gray else CriticalRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Actions
            if (!anomaly.resolved) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onResolve() }) {
                        Text("RESOLVE", color = HealthGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onAlertDriver()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Alert dispatched to driver of ${anomaly.truckId}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = anomaly.severity.color.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("ALERT DRIVER", color = anomaly.severity.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text("Will auto-dismiss in 5s...", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}
