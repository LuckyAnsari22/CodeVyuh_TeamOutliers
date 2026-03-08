package com.greeniq.app.network.models

import com.google.gson.annotations.SerializedName

// ── Carbon Tracking ──
data class CarbonTrackingRequest(
    val origin: String = "Mumbai",
    val destination: String = "Delhi",
    @SerializedName("distance_km") val distanceKm: Double = 1400.0,
    @SerializedName("weight_kg") val weightKg: Double = 18000.0,
    @SerializedName("vehicle_type") val vehicleType: String = "diesel_hcv_bsvi",
    @SerializedName("road_type") val roadType: String = "national_highway"
)

data class CarbonResult(
    val origin: String = "",
    val destination: String = "",
    @SerializedName("distance_km") val distanceKm: Double = 0.0,
    @SerializedName("weight_tonnes") val weightTonnes: Double = 0.0,
    @SerializedName("vehicle_type") val vehicleType: String = "",
    @SerializedName("ttw_emissions_kg") val ttwEmissions: Double = 0.0,
    @SerializedName("wtt_emissions_kg") val wttEmissions: Double = 0.0,
    @SerializedName("wtw_emissions_kg") val wtwEmissions: Double = 0.0,
    @SerializedName("emission_factor_per_ton_km") val emissionFactorPerTonKm: Double = 0.0,
    @SerializedName("carbon_grade") val carbonGrade: String = "",
    val recommendation: String = ""
)

// ── Route Optimization ──
data class RouteOptimizationRequest(
    val depot: Map<String, Double> = mapOf("lat" to 19.076, "lng" to 72.877),
    val deliveries: List<Map<String, Any>> = emptyList(),
    @SerializedName("vehicle_count") val vehicleCount: Int = 3,
    @SerializedName("vehicle_capacity_kg") val vehicleCapacityKg: Int = 25000,
    @SerializedName("vehicle_type") val vehicleType: String = "diesel_hcv_bsvi"
)

// ── Load Consolidation ──
data class ConsolidationRequest(
    val shipments: List<Map<String, Any>> = emptyList(),
    @SerializedName("vehicle_capacity_kg") val vehicleCapacityKg: Int = 25000,
    @SerializedName("max_detour_km") val maxDetourKm: Double = 50.0
)

// ── AI Advisor ──
data class AdvisorRequest(
    val question: String = "",
    @SerializedName("fleet_context") val fleetContext: Map<String, Any>? = null,
    @SerializedName("conversation_history") val conversationHistory: List<Map<String, String>> = emptyList()
)

data class AIQueryRequest(
    val query: String = "",
    val context: Map<String, Any>? = null
)

// ── Solution Card Data ──
data class SolutionItem(
    val id: String,
    val icon: String,
    val title: String,
    val description: String,
    val features: List<String>,
    val tag: String,
    val endpoint: String
)
