package com.greeniq.app.ui.twin

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// Theme Colors
val BgPrimary = Color(0xFF09090E)
val BgCard = Color(0xFF131520)
val AccentCyan = Color(0xFF07D8F6)
val HealthGreen = Color(0xFF00FF88)
val AlertAmber = Color(0xFFFF8C00)
val CriticalRed = Color(0xFFFF3B30)
val GlassBorder = Color(0x33FFFFFF)

@Composable
fun DigitalTwinScreen() {
    var selectedTruckId by remember { mutableIntStateOf(0) }
    
    val trucks = listOf(
        "MH-04-AB-1234 | Mumbai → Delhi",
        "UP-16-XY-9876 | Noida → Lucknow",
        "GJ-01-PQ-5555 | Ahmedabad → Surat",
        "TN-09-CD-1111 | Chennai → Bangalore"
    )

    // Coroutine State
    var syncTime by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            syncTime += 1
            if (syncTime > 5) syncTime = 0
        }
    }

    var speed by remember { mutableIntStateOf(67) }
    var rpm by remember { mutableIntStateOf(1847) }
    var coolant by remember { mutableIntStateOf(89) }

    LaunchedEffect(selectedTruckId) {
        while (true) {
            delay(3000)
            speed = Random.nextInt(55, 81)
            rpm = Random.nextInt(1400, 2201)
            coolant = Random.nextInt(82, 106)
        }
    }

    val fuelRate = (rpm / 2200f) * 18f
    val co2Rate = fuelRate * 2.68f * 1.10f // kg CO2/hr

    val healthColor = when {
        coolant > 100 || speed > 75 -> CriticalRed
        rpm > 2000 -> AlertAmber
        else -> HealthGreen
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = BgCard) {
                trucks.forEachIndexed { index, _ ->
                    NavigationBarItem(
                        selected = selectedTruckId == index,
                        onClick = { selectedTruckId = index },
                        icon = { Text("🚚") },
                        label = { Text("T-${index+1}", color = if(selectedTruckId == index) AccentCyan else Color.Gray) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = AccentCyan,
                            unselectedIconColor = Color.Gray
                        )
                    )
                }
            }
        },
        containerColor = BgPrimary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // SECTION 1: Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CARBON DIGITAL TWIN", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(trucks[selectedTruckId], color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("SYNC: ${syncTime}s ago", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TwinHeader(healthColor)
            }

            // SECTION 2: Sensor Fusion
            item {
                Text("LIVE SENSOR FUSION", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                SensorGrid(speed, rpm, fuelRate, coolant)
            }

            // SECTION 3: Carbon Rate
            item {
                CarbonRateSection(co2Rate)
            }

            // SECTION 4: Prediction
            item {
                PredictionSection(speed, coolant)
            }

            // SECTION 5: Coaching
            item {
                DriverCoaching(speed, rpm, co2Rate)
            }

            // SECTION 6: Ledger
            item {
                TripLedger()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun TwinHeader(healthColor: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(BgCard, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(120.dp, 80.dp)) {
            val hc = healthColor.copy(alpha = pulseAlpha)
            // Draw a basic truck
            drawRoundRect(
                color = hc,
                topLeft = Offset(0f, 20f),
                size = Size(70f, 40f),
                cornerRadius = CornerRadius(8f)
            )
            drawRoundRect(
                color = hc,
                topLeft = Offset(72f, 30f),
                size = Size(38f, 30f),
                cornerRadius = CornerRadius(8f)
            )
            // Wheels
            drawCircle(color = Color.White, radius = 10f, center = Offset(20f, 65f))
            drawCircle(color = Color.White, radius = 10f, center = Offset(50f, 65f))
            drawCircle(color = Color.White, radius = 10f, center = Offset(90f, 65f))
        }
        
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).background(healthColor, RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.width(4.dp))
            Text("TWIN SYNCED", color = healthColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SensorGrid(speed: Int, rpm: Int, fuelRate: Float, coolant: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SensorBox(modifier = Modifier.weight(1f), title = "GPS Speed", value = "$speed", unit = "km/h")
            SensorBox(modifier = Modifier.weight(1f), title = "Engine RPM", value = "$rpm", unit = "RPM")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SensorBox(modifier = Modifier.weight(1f), title = "Fuel Rate", value = String.format("%.1f", fuelRate), unit = "L/hr")
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text("Load (Static)", color = Color.Gray, fontSize = 10.sp)
                Text("18.4 / 25.0 T", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = 18.4f / 25.0f,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = AccentCyan,
                    trackColor = Color.DarkGray
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val coolantColor = if (coolant > 100) CriticalRed else Color.White
            SensorBox(modifier = Modifier.weight(1f), title = "Coolant Temp", value = "$coolant", unit = "°C", valueColor = coolantColor)
            SensorBox(modifier = Modifier.weight(1f), title = "Road Quality", value = "1.05", unit = "x (NH)")
        }
    }
}

@Composable
fun SensorBox(modifier: Modifier = Modifier, title: String, value: String, unit: String, valueColor: Color = Color.White) {
    Column(
        modifier = modifier
            .background(Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(title, color = Color.Gray, fontSize = 10.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(2.dp))
            Text(unit, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
fun CarbonRateSection(co2Rate: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("REAL-TIME CARBON RATE", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(String.format("%.1f", co2Rate), color = AccentCyan, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text(" kg CO₂/hour", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        Text("Source: GLEC v3 (WTW)", color = Color.Gray, fontSize = 10.sp)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("vs Fleet Average", color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("This Truck", color = AccentCyan, fontSize = 10.sp, modifier = Modifier.width(60.dp))
            LinearProgressIndicator(progress = 0.7f, color = AccentCyan, modifier = Modifier.weight(1f).height(8.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Fleet Avg", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(60.dp))
            LinearProgressIndicator(progress = 0.6f, color = Color.Gray, modifier = Modifier.weight(1f).height(8.dp))
        }
    }
}

@Composable
fun PredictionSection(speed: Int, coolant: Int) {
    val showWarning = speed > 75 || coolant > 95
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("AI EMISSION FORECAST — Next 30 min", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("XGBoost model [Vertex AI]", color = Color.Gray, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (showWarning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x33FFFF3B30), RoundedCornerShape(8.dp))
                    .border(1.dp, CriticalRed, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text("⚠️ EMISSION SPIKE PREDICTED", color = CriticalRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            val w = size.width
            val h = size.height
            val mid = w * 0.3f
            
            // Actual Line (Green)
            val path1 = Path().apply {
                moveTo(0f, h * 0.7f)
                lineTo(mid * 0.5f, h * 0.6f)
                lineTo(mid, h * 0.5f)
            }
            drawPath(path1, HealthGreen, style = Stroke(width = 4f))
            
            // Predicted Line (Cyan Dashed)
            val path2 = Path().apply {
                moveTo(mid, h * 0.5f)
                if (showWarning) {
                    lineTo(w * 0.6f, h * 0.2f)
                    lineTo(w, h * 0.1f)
                } else {
                    lineTo(w * 0.6f, h * 0.5f)
                    lineTo(w, h * 0.6f)
                }
            }
            drawPath(
                path2, 
                AccentCyan, 
                style = Stroke(
                    width = 4f, 
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )
            )
            
            // Divider
            drawLine(Color.Gray, Offset(mid, 0f), Offset(mid, h), strokeWidth = 2f)
            
            // Avg line
            drawLine(Color.DarkGray, Offset(0f, h * 0.4f), Offset(w, h * 0.4f), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f,5f),0f))
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Last 10m", color = HealthGreen, fontSize = 10.sp)
            Text("Now", color = Color.White, fontSize = 10.sp)
            Text("Next 30m Forecast", color = AccentCyan, fontSize = 10.sp)
        }
    }
}

@Composable
fun DriverCoaching(speed: Int, rpm: Int, co2Rate: Float) {
    val message = when {
        speed > 75 -> "💡 Reduce speed to 65 km/h — saves 18% fuel [optimal speed curve, ARAI 2022]"
        rpm > 2000 -> "💡 Shift up — high RPM at this speed wastes 0.8L/hr extra"
        co2Rate > 35f -> "💡 You're 12% above fleet average this hour"
        else -> "✅ Excellent driving — you're in the green zone this trip"
    }
    val color = if (message.startsWith("✅")) HealthGreen else AlertAmber

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("REAL-TIME COACHING", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = color, fontSize = 14.sp)
    }
}

@Composable
fun TripLedger() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard, RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("TRIP CARBON LEDGER", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        LedgerRow("Distance covered", "284 km")
        LedgerRow("CO₂ emitted so far", "127.4 kg")
        LedgerRow("vs optimal route", "+8.2 kg", CriticalRed)
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Color.DarkGray)
        Spacer(modifier = Modifier.height(8.dp))
        LedgerRow("Projected total", "445 kg CO₂e", Color.White)
        LedgerRow("CCTS impact", "−0.12 credits", AlertAmber)
    }
}

@Composable
fun LedgerRow(label: String, value: String, valueColor: Color = AccentCyan) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.LightGray, fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
