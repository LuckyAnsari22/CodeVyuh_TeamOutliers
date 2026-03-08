package com.greeniq.app.ui.home

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.greeniq.app.R
import com.greeniq.app.databinding.FragmentHomeBinding
import com.greeniq.app.gl.RoadMapRenderer
import com.greeniq.app.network.RetrofitClient
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var renderer: RoadMapRenderer? = null
    private val wasteHandler = Handler(Looper.getMainLooper())
    private val fpsHandler = Handler(Looper.getMainLooper())
    private var wasteStartTime = 0L
    private val WASTE_PER_SECOND = 4850 // ₹

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGL()
        setupStats()
        setupStateControls()
        setupQuickActions()
        setupModuleGrid()
        startWasteCounter()
        startFpsUpdater()
        startLiveDotPulse()
        animateEntrance()
    }

    private fun setupGL() {
        try {
            renderer = RoadMapRenderer()
            binding.glSurfaceView.setEGLContextClientVersion(2)
            binding.glSurfaceView.setRenderer(renderer)
            binding.glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        } catch (e: Exception) {
            // GL not supported on this device — hide the view
            binding.glSurfaceView.visibility = View.GONE
        }
    }

    private fun setupStats() {
        val stats = listOf(
            Pair("80–130 Mt", "CO₂/year road freight"),
            Pair("₹1.53L Cr", "Wasted annually"),
            Pair("45%", "Truck capacity empty"),
            Pair("38th", "Logistics Performance")
        )

        binding.statsRecycler.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.statsRecycler.adapter = StatsAdapter(stats)
    }

    private fun setupStateControls() {
        binding.btnNeutral.setOnClickListener {
            renderer?.currentState = RoadMapRenderer.MapState.NEUTRAL
            highlightStateButton(binding.btnNeutral)
            Toast.makeText(requireContext(), "🌐 Neutral — India's highway network", Toast.LENGTH_SHORT).show()
        }
        binding.btnDisease.setOnClickListener {
            renderer?.currentState = RoadMapRenderer.MapState.DISEASED
            highlightStateButton(binding.btnDisease)
            Toast.makeText(requireContext(), "🔴 Diseased — ₹1.53L Cr wasted, 45% empty", Toast.LENGTH_SHORT).show()
        }
        binding.btnHeal.setOnClickListener {
            renderer?.currentState = RoadMapRenderer.MapState.HEALING
            highlightStateButton(binding.btnHeal)
            Toast.makeText(requireContext(), "💚 GREENIQ — AI-optimized logistics", Toast.LENGTH_SHORT).show()
        }
        highlightStateButton(binding.btnNeutral)
    }

    private fun highlightStateButton(activeBtn: View) {
        listOf(binding.btnNeutral, binding.btnDisease, binding.btnHeal).forEach { btn ->
            btn.alpha = if (btn == activeBtn) 1f else 0.4f
        }
    }

    private fun setupQuickActions() {
        binding.cardCarbon.setOnClickListener {
            runModuleDemo("ps6", "Carbon Tracker")
        }
        binding.cardRoutes.setOnClickListener {
            runModuleDemo("ps4", "Route Optimization")
        }
        binding.cardAdvisor.setOnClickListener {
            safeNavigate(R.id.advisorFragment)
        }
        binding.btnLaunch.setOnClickListener {
            safeNavigate(R.id.solutionsFragment)
        }
        binding.btnDemo.setOnClickListener {
            runAllDemos()
        }
    }

    private fun setupModuleGrid() {
        binding.cardPs1.setOnClickListener { runModuleDemo("ps1", "Carrier Selection") }
        binding.cardPs2.setOnClickListener { runModuleDemo("ps2", "Document Intelligence") }
        binding.cardPs3.setOnClickListener { runModuleDemo("ps3", "Negotiation Agent") }
        binding.cardPs4.setOnClickListener { runModuleDemo("ps4", "Route Optimizer") }
        binding.cardPs5.setOnClickListener { runModuleDemo("ps5", "Load Consolidation") }
        binding.cardPs6.setOnClickListener { runModuleDemo("ps6", "Carbon Tracker") }
        binding.cardPs7.setOnClickListener { safeNavigate(R.id.advisorFragment) }
        binding.cardPs8.setOnClickListener { runModuleDemo("ps8", "Corporate Rebranding") }
        binding.cardPs9.setOnClickListener { runModuleDemo("ps9", "Lane Intelligence") }
    }

    private fun safeNavigate(destinationId: Int) {
        try {
            findNavController().navigate(destinationId)
        } catch (e: Exception) {
            context?.let {
                Toast.makeText(it, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun runAllDemos() {
        Toast.makeText(requireContext(), "🚀 Running all 9 module demos...", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch {
            val results = mutableListOf<String>()

            val modules = listOf(
                "ps1" to "Carrier Selection",
                "ps2" to "Document Intelligence",
                "ps3" to "Negotiation Agent",
                "ps4" to "Route Optimization",
                "ps5" to "Load Consolidation",
                "ps6" to "Carbon Tracker",
                "ps7" to "HARIT AI Advisor",
                "ps8" to "Corporate Rebranding",
                "ps9" to "Lane Intelligence"
            )

            for ((id, name) in modules) {
                try {
                    val response = when (id) {
                        "ps1" -> RetrofitClient.apiService.scoreCarriers()
                        "ps2" -> RetrofitClient.apiService.matchDocuments()
                        "ps3" -> RetrofitClient.apiService.listScenarios()
                        "ps4" -> RetrofitClient.apiService.demoRouteOptimization()
                        "ps5" -> RetrofitClient.apiService.demoConsolidation()
                        "ps6" -> RetrofitClient.apiService.demoCarbonTracking()
                        "ps7" -> RetrofitClient.apiService.getGlobalData()
                        "ps8" -> RetrofitClient.apiService.getBrandAssets()
                        "ps9" -> RetrofitClient.apiService.demoLaneIntelligence()
                        else -> continue
                    }
                    results.add(if (response.isSuccessful) "✅ $name" else "⚠️ $name (${response.code()})")
                } catch (e: Exception) {
                    results.add("📱 $name — offline data ready")
                }
            }

            showDemoResults(results)
        }
    }

    private fun runModuleDemo(moduleId: String, moduleName: String) {
        showModuleResult(moduleName, getOfflineDemoData(moduleId))

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = when (moduleId) {
                    "ps1" -> RetrofitClient.apiService.scoreCarriers()
                    "ps2" -> RetrofitClient.apiService.matchDocuments()
                    "ps3" -> RetrofitClient.apiService.listScenarios()
                    "ps4" -> RetrofitClient.apiService.demoRouteOptimization()
                    "ps5" -> RetrofitClient.apiService.demoConsolidation()
                    "ps6" -> RetrofitClient.apiService.demoCarbonTracking()
                    "ps7" -> RetrofitClient.apiService.getGlobalData()
                    "ps8" -> RetrofitClient.apiService.getBrandAssets()
                    "ps9" -> RetrofitClient.apiService.demoLaneIntelligence()
                    else -> return@launch
                }
                if (response.isSuccessful) {
                    Toast.makeText(context ?: return@launch,
                        "✅ $moduleName: Live data received!", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) { }
        }
    }

    private fun showModuleResult(title: String, content: String) {
        val ctx = context ?: return
        try {
            AlertDialog.Builder(ctx)
                .setTitle("🧪 $title")
                .setMessage(content)
                .setPositiveButton("Done") { d, _ -> d.dismiss() }
                .show()
                .apply {
                    window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(ctx.getColor(R.color.bg_card)))
                    findViewById<android.widget.TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(
                        ctx.getColor(R.color.text_primary))
                    findViewById<android.widget.TextView>(android.R.id.message)?.apply {
                        setTextColor(ctx.getColor(R.color.text_secondary))
                        textSize = 13f
                        setLineSpacing(0f, 1.4f)
                    }
                    getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                        ctx.getColor(R.color.accent_cyan))
                }
        } catch (e: Exception) {
            Toast.makeText(ctx, content.take(200), Toast.LENGTH_LONG).show()
        }
    }

    private fun showDemoResults(results: List<String>) {
        val ctx = context ?: return
        try {
            AlertDialog.Builder(ctx)
                .setTitle("🚀 Global AI Execution Complete")
                .setMessage(results.joinToString("\n\n"))
                .setPositiveButton("Done") { d, _ -> d.dismiss() }
                .show()
                .apply {
                    window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(ctx.getColor(R.color.bg_card)))
                    findViewById<android.widget.TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(
                        ctx.getColor(R.color.text_primary))
                    findViewById<android.widget.TextView>(android.R.id.message)?.apply {
                        setTextColor(ctx.getColor(R.color.text_secondary))
                        textSize = 13f
                        setLineSpacing(0f, 1.4f)
                    }
                    getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                        ctx.getColor(R.color.accent_cyan))
                }
        } catch (e: Exception) {
            Toast.makeText(ctx, "Results ready. Check dashboard.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getOfflineDemoData(moduleId: String): String {
        return when (moduleId) {
            "ps1" -> """
                Carrier Selection (Offline Demo)
                
                Algorithm: XGBoost + TOPSIS
                Carriers Scored: 15
                
                Top 3 Carriers:
                1. Rivigo Express — 0.93/1.0
                2. Delhivery Freight — 0.87/1.0
                3. TCI Express — 0.82/1.0
            """.trimIndent()
            "ps2" -> "Document Intelligence (Offline Demo)"
            "ps3" -> "Negotiation Agent (Offline Demo)"
            "ps4" -> "Route Optimization (Offline Demo)"
            "ps5" -> "Load Consolidation (Offline Demo)"
            "ps6" -> "Carbon Tracker (Offline Demo)"
            "ps7" -> "HARIT AI Advisor (Offline Demo)"
            "ps8" -> "Corporate Rebranding (Offline Demo)"
            "ps9" -> "Lane Intelligence (Offline Demo)"
            else -> "Demo data not available"
        }
    }

    private fun startWasteCounter() {
        wasteStartTime = System.currentTimeMillis()
        wasteHandler.post(object : Runnable {
            override fun run() {
                if (_binding == null) return
                val elapsed = (System.currentTimeMillis() - wasteStartTime) / 1000.0
                val wasted = (elapsed * WASTE_PER_SECOND).toLong()
                binding.liveWasteValue.text = "₹${String.format("%,d", wasted)}"
                wasteHandler.postDelayed(this, 50)
            }
        })
    }

    private fun startFpsUpdater() {
        fpsHandler.post(object : Runnable {
            override fun run() {
                if (_binding == null) return
                renderer?.let {
                    binding.tvFps.text = "${it.currentFps} FPS"
                }
                fpsHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun startLiveDotPulse() {
        if (_binding == null) return
        binding.liveDot.animate().alpha(0.3f).setDuration(800).withEndAction {
            if (_binding == null) return@withEndAction
            binding.liveDot.animate().alpha(1f).setDuration(800).withEndAction { startLiveDotPulse() }.start()
        }.start()
    }

    private fun animateEntrance() {
        if (_binding == null) return
        try {
            val fadeUp = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_up)
            binding.heroTag.startAnimation(fadeUp)
            binding.heroTitle.startAnimation(fadeUp)
            binding.heroSubtitle.startAnimation(fadeUp)
        } catch (_: Exception) { }
    }

    override fun onResume() {
        super.onResume()
        try {
            binding.glSurfaceView.onResume()
        } catch (_: Exception) { }
    }

    override fun onPause() {
        super.onPause()
        try {
            binding.glSurfaceView.onPause()
        } catch (_: Exception) { }
        wasteHandler.removeCallbacksAndMessages(null)
        fpsHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        wasteHandler.removeCallbacksAndMessages(null)
        fpsHandler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
