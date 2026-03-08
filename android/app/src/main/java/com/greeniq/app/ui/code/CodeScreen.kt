package com.greeniq.app.ui.code

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Theme Colors
val BgPrimary = Color(0xFF09090E)
val CodeBlockBg = Color(0xFF050F14)
val AccentCyan = Color(0xFF00E5FF)
val StringGreen = Color(0xFF00FF88)
val CommentGrey = Color(0xFF546E7A)
val NumberOrange = Color(0xFFFF8C00)
val GlassBorder = Color(0x33FFFFFF)

@Composable
fun CodeScreen() {
    Scaffold(
        containerColor = BgPrimary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column {
                    Text("SYSTEM BLUEPRINT", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Implementation Code", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Real IoT implementation snippets powering GREENIQ.", color = Color.Gray, fontSize = 14.sp)
                }
            }

            item {
                CodeBlockCard(
                    title = "IoT Telemetry Payload — Published every 5 seconds per truck",
                    code = """
{
  "truck_id": "MH-04-AB-1234",
  "timestamp": "2026-03-08T14:23:07.431Z",
  "gps": {
    "lat": 19.432,
    "lng": 74.128,
    "speed_kmh": 67.4,
    "heading_deg": 12.3,
    "altitude_m": 584
  },
  "obd": {
    "engine_rpm": 1847,
    "coolant_temp_c": 89,
    "fuel_rate_lph": 12.3,
    "throttle_pct": 42,
    "maf_g_per_sec": 18.7,
    "engine_load_pct": 61
  },
  "sensors": {
    "axle_load_kg": 18420,
    "ambient_temp_c": 31,
    "harsh_brake_count": 0,
    "harsh_accel_count": 1
  },
  "edge_computed": {
    "co2_rate_kg_per_hr": 32.94,
    "emission_ef_kg_per_tonkm": 0.0712,
    "efficiency_score": 78,
    "idling_minutes": 0
  }
}
                    """.trimIndent()
                )
            }

            item {
                CodeBlockCard(
                    title = "Cloud Function — triggers on every telemetry write, computes carbon",
                    code = """
exports.onTelemetry = functions.database
  .ref('/trucks/{truckId}/telemetry/latest')
  .onWrite(async (change, context) => {
    const data = change.after.val();
    
    // Sensor fusion — combine OBD + GPS + load
    const fuelRateLph = data.obd.fuel_rate_lph;
    const loadTonnes = data.sensors.axle_load_kg / 1000;
    const speedKmh = data.gps.speed_kmh;
    
    // GLEC v3 WTW emission calculation
    const CO2_PER_LITRE_WTW = 2.68 + 0.58; // TTW + WTT, GLEC v3 2023
    const co2RateKgPerHr = fuelRateLph * CO2_PER_LITRE_WTW;
    
    // Anomaly detection
    const anomalies = [];
    if (speedKmh < 5 && data.obd.engine_rpm > 600) {
      anomalies.push({ type: 'IDLING_WASTE', 
        co2_waste_kg_per_hr: fuelRateLph * CO2_PER_LITRE_WTW });
    }
    if (speedKmh > 85) {
      anomalies.push({ type: 'SPEED_INEFFICIENCY',
        excess_co2_pct: (speedKmh - 80) * 0.8 }); // ARAI speed-EF curve
    }
    
    // Write to Firestore for GREENIQ app
    await firestore.collection('carbon_records').add({
      truck_id: context.params.truckId,
      timestamp: data.timestamp,
      co2_rate_kg_per_hr: co2RateKgPerHr,
      anomalies: anomalies,
      glec_v3_compliant: true
    });
  });
                    """.trimIndent()
                )
            }

            item {
                CodeBlockCard(
                    title = "Android MQTT Client — subscribes to live truck feed",
                    code = """
class IoTRepository {
  private val mqttClient = MqttAndroidClient(
    context, "ssl://broker.hivemq.com:8883", clientId
  )
  
  fun subscribeTruckTelemetry(truckId: String): Flow<TruckTelemetry> = 
    callbackFlow {
      mqttClient.subscribe(
        "greeniq/fleet/${'$'}truckId/telemetry",
        qos = 1
      ) { _, message ->
        val telemetry = Json.decodeFromString<TruckTelemetry>(
          message.payload.toString(Charsets.UTF_8)
        )
        trySend(telemetry)
      }
      awaitClose { mqttClient.unsubscribe("greeniq/fleet/${'$'}truckId/telemetry") }
    }
}
                    """.trimIndent()
                )
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun CodeBlockCard(title: String, code: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CodeBlockBg, RoundedCornerShape(12.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = Color.LightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = applySyntaxHighlighting(code),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color.White
            )
        )
    }
}

fun applySyntaxHighlighting(code: String): AnnotatedString {
    return buildAnnotatedString {
        append(code)
        
        val keywordRegex = "\\b(val|fun|if|await|async|const|class|private|exports|callbackFlow|trySend|awaitClose|return|true|false)\\b".toRegex()
        val stringRegex = "(\"[^\"]*\"|'[^']*')".toRegex()
        val commentRegex = "(//.*)".toRegex()
        val numberRegex = "\\b(\\d+(\\.\\d+)?)\\b".toRegex()
        
        // 1. Numbers
        numberRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = NumberOrange), match.range.first, match.range.last + 1)
        }
        
        // 2. Keywords
        keywordRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = AccentCyan), match.range.first, match.range.last + 1)
        }
        
        // 3. Strings
        stringRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = StringGreen), match.range.first, match.range.last + 1)
        }
        
        // 4. Comments (Applied last so it overrides anything incorrectly caught inside comments)
        commentRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = CommentGrey), match.range.first, match.range.last + 1)
        }
    }
}
