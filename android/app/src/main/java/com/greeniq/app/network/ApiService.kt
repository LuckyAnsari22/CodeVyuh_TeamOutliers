package com.greeniq.app.network

import com.greeniq.app.network.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── HEALTH ──
    @GET("api/v1/health")
    suspend fun healthCheck(): Response<Map<String, Any>>

    // ── PS1: CARRIER SELECTION ──
    @POST("api/v1/ps1/score")
    suspend fun scoreCarriers(@Body weights: Map<String, Double>? = null): Response<Map<String, Any>>

    // ── PS2: DOCUMENT INTELLIGENCE ──
    @GET("api/v1/ps2/match")
    suspend fun matchDocuments(): Response<Map<String, Any>>

    // ── PS3: NEGOTIATION ──
    @GET("api/v1/ps3/scenarios")
    suspend fun listScenarios(): Response<Map<String, Any>>

    @GET("api/v1/ps3/negotiate/{scenarioKey}")
    suspend fun runNegotiation(@Path("scenarioKey") key: String): Response<Map<String, Any>>

    // ── PS4: ROUTE OPTIMIZATION ──
    @GET("api/v1/ps4/demo")
    suspend fun demoRouteOptimization(): Response<Map<String, Any>>

    @POST("api/v1/ps4/optimize")
    suspend fun optimizeRoutes(@Body request: RouteOptimizationRequest): Response<Map<String, Any>>

    // ── PS5: LOAD CONSOLIDATION ──
    @GET("api/v1/ps5/demo")
    suspend fun demoConsolidation(): Response<Map<String, Any>>

    @POST("api/v1/ps5/consolidate")
    suspend fun consolidateLoads(@Body request: ConsolidationRequest): Response<Map<String, Any>>

    // ── PS6: CARBON TRACKER ──
    @GET("api/v1/ps6/demo")
    suspend fun demoCarbonTracking(): Response<Map<String, Any>>

    @POST("api/v1/ps6/calculate")
    suspend fun calculateCarbon(@Body request: CarbonTrackingRequest): Response<CarbonResult>

    // ── PS7: LORRI INTELLIGENCE ──
    @GET("api/v1/ps7/global-data")
    suspend fun getGlobalData(): Response<Map<String, Any>>

    @POST("api/v1/ps7/ask")
    suspend fun queryAI(@Body request: AIQueryRequest): Response<Map<String, Any>>

    // ── PS8: REBRANDING ──
    @GET("api/v1/ps8/brand-assets")
    suspend fun getBrandAssets(): Response<Map<String, Any>>

    // ── PS9: LANE INTELLIGENCE ──
    @GET("api/v1/ps9/demo")
    suspend fun demoLaneIntelligence(): Response<Map<String, Any>>

    // ── SUSTAINABILITY ADVISOR ──
    @POST("api/v1/sustainability/advise")
    suspend fun getAdvisory(@Body request: AdvisorRequest): Response<Map<String, Any>>

    // ── ROOT ──
    @GET("/")
    suspend fun root(): Response<Map<String, Any>>
}
