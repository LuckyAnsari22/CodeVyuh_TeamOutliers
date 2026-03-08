package com.greeniq.app.ui.carbon

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.greeniq.app.R
import com.greeniq.app.databinding.FragmentCarbonBinding
import com.greeniq.app.network.RetrofitClient
import com.greeniq.app.network.models.CarbonTrackingRequest
import kotlinx.coroutines.launch
import kotlin.math.*

class CarbonFragment : Fragment() {

    private var _binding: FragmentCarbonBinding? = null
    private val binding get() = _binding!!

    private val cities = listOf("Delhi", "Mumbai", "Chennai", "Kolkata", "Bangalore",
        "Hyderabad", "Ahmedabad", "Pune", "Jaipur", "Surat", "Kanyakumari")
    private val vehicles = listOf("diesel_hcv_bsvi", "diesel_hcv_bsiv", "cng_hcv",
        "electric_truck", "rail_electric")
    private val vehicleLabels = listOf("Diesel HCV (BS-VI)", "Diesel HCV (BS-IV)", "CNG HCV",
        "Electric Truck", "Rail (Electric)")

    // Emission factors (GLEC v3)
    private val EF = mapOf(
        "diesel_hcv_bsvi" to 0.0788,
        "diesel_hcv_bsiv" to 0.0915,
        "cng_hcv" to 0.0775,
        "electric_truck" to 0.0385,
        "rail_electric" to 0.0210
    )

    // City coordinates for distance calculation
    private val COORDS = mapOf(
        "Delhi" to Pair(28.704, 77.102),
        "Mumbai" to Pair(19.076, 72.877),
        "Chennai" to Pair(13.082, 80.273),
        "Kolkata" to Pair(22.572, 88.362),
        "Bangalore" to Pair(12.971, 77.594),
        "Hyderabad" to Pair(17.385, 78.486),
        "Ahmedabad" to Pair(23.022, 72.571),
        "Pune" to Pair(18.520, 73.856),
        "Jaipur" to Pair(26.912, 75.787),
        "Surat" to Pair(21.170, 72.831),
        "Kanyakumari" to Pair(8.077, 77.550)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCarbonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        setupCalculateButton()
        setupProvenanceToggle()
    }

    private fun setupProvenanceToggle() {
        binding.btnToggleProvenance.setOnClickListener {
            val content = binding.provenanceContent
            val icon = binding.provenanceIcon
            if (content.visibility == View.VISIBLE) {
                content.visibility = View.GONE
                icon.text = "+"
            } else {
                content.visibility = View.VISIBLE
                icon.text = "−"
            }
        }
    }

    private fun setupSpinners() {
        val ctx = requireContext()
        val cityAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, cities)
        binding.spinnerOrigin.adapter = cityAdapter
        binding.spinnerDest.adapter = cityAdapter
        binding.spinnerDest.setSelection(1) // Default: Mumbai

        val vehicleAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, vehicleLabels)
        binding.spinnerVehicle.adapter = vehicleAdapter
    }

    private fun setupCalculateButton() {
        binding.btnCalculate.setOnClickListener {
            val origin = cities[binding.spinnerOrigin.selectedItemPosition]
            val dest = cities[binding.spinnerDest.selectedItemPosition]
            val weight = binding.editWeight.text.toString().toDoubleOrNull() ?: 18000.0
            val vehicleType = vehicles[binding.spinnerVehicle.selectedItemPosition]

            if (origin == dest) {
                binding.editWeight.error = "Origin and destination must differ"
                return@setOnClickListener
            }

            calculateCarbon(origin, dest, weight, vehicleType)
        }
    }

    private fun calculateCarbon(origin: String, dest: String, weight: Double, vehicleType: String) {
        binding.progressLoading.visibility = View.VISIBLE
        binding.resultCard.visibility = View.GONE

        // Try backend first, fallback to local calculation
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val distance = calculateDistance(origin, dest)
                val request = CarbonTrackingRequest(
                    origin = origin,
                    destination = dest,
                    distanceKm = distance,
                    weightKg = weight,
                    vehicleType = vehicleType
                )

                val response = RetrofitClient.apiService.calculateCarbon(request)
                binding.progressLoading.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    showResults(response.body()!!)
                } else {
                    // Fallback to local calculation
                    showLocalResults(origin, dest, distance, weight, vehicleType)
                }
            } catch (e: Exception) {
                binding.progressLoading.visibility = View.GONE
                // Fallback to local calc (works offline)
                val distance = calculateDistance(origin, dest)
                showLocalResults(origin, dest, distance, weight, vehicleType)
            }
        }
    }

    private fun showLocalResults(origin: String, dest: String, distance: Double, weight: Double, vehicleType: String) {
        val ef = EF[vehicleType] ?: 0.0788
        val weightTonnes = weight / 1000.0
        val wtwEmissions = ef * weightTonnes * distance
        val grade = getGrade(ef)

        val result = com.greeniq.app.network.models.CarbonResult(
            origin = origin,
            destination = dest,
            distanceKm = distance,
            weightTonnes = weightTonnes,
            vehicleType = vehicleType,
            wtwEmissions = wtwEmissions,
            emissionFactorPerTonKm = ef,
            carbonGrade = grade.first,
            recommendation = getRecommendation(vehicleType, distance)
        )
        showResults(result)
    }

    private fun showResults(result: com.greeniq.app.network.models.CarbonResult) {
        binding.resultCard.visibility = View.VISIBLE
        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_in)
        binding.resultCard.startAnimation(anim)

        // Animate emission number
        val animator = ValueAnimator.ofFloat(0f, result.wtwEmissions.toFloat())
        animator.duration = 1500
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener {
            binding.tvEmissions.text = "${String.format("%,.0f", it.animatedValue as Float)} kg"
        }
        animator.start()

        // Grade
        val (grade, label) = getGrade(result.emissionFactorPerTonKm)
        binding.tvGrade.text = grade
        binding.tvGrade.setBackgroundColor(getGradeColor(grade))
        binding.tvPerTonKm.text = "${String.format("%.4f", result.emissionFactorPerTonKm)} kg CO₂e/ton-km ($label)"

        // Comparison bar
        val ratio = minOf(result.emissionFactorPerTonKm / 0.0788, 1.5)
        val barAnimator = ValueAnimator.ofInt(0, (ratio * 100).toInt().coerceAtMost(100))
        barAnimator.duration = 1000
        barAnimator.startDelay = 500
        barAnimator.addUpdateListener {
            binding.comparisonBar.progress = it.animatedValue as Int
        }
        barAnimator.start()

        // Recommendation
        binding.tvRecommendation.text = result.recommendation.ifEmpty {
            getRecommendation(result.vehicleType, result.distanceKm)
        }
    }

    private fun calculateDistance(origin: String, dest: String): Double {
        val o = COORDS[origin] ?: return 1400.0
        val d = COORDS[dest] ?: return 1400.0
        val R = 6371.0
        val dLat = Math.toRadians(d.first - o.first)
        val dLon = Math.toRadians(d.second - o.second)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(o.first)) * cos(Math.toRadians(d.first)) * sin(dLon / 2).pow(2)
        val straight = R * 2 * atan2(sqrt(a), sqrt(1 - a))
        return straight * 1.3 // Road factor
    }

    private fun getGrade(perTonKm: Double): Pair<String, String> = when {
        perTonKm <= 0.030 -> "A" to "Excellent"
        perTonKm <= 0.055 -> "B" to "Good"
        perTonKm <= 0.079 -> "C" to "Average"
        perTonKm <= 0.110 -> "D" to "Below Avg"
        else -> "F" to "Critical"
    }

    private fun getGradeColor(grade: String): Int {
        val ctx = requireContext()
        return when (grade) {
            "A" -> ContextCompat.getColor(ctx, R.color.carbon_excellent)
            "B" -> ContextCompat.getColor(ctx, R.color.carbon_good)
            "C" -> ContextCompat.getColor(ctx, R.color.accent_cyan)
            "D" -> ContextCompat.getColor(ctx, R.color.carbon_warning)
            else -> ContextCompat.getColor(ctx, R.color.carbon_critical)
        }
    }

    private fun getRecommendation(vehicleType: String, distance: Double): String = when {
        vehicleType == "diesel_hcv_bsiv" -> "⚡ Upgrade to BS-VI: saves 14% TTW emissions and 50% PM2.5."
        vehicleType == "diesel_hcv_bsvi" && distance > 500 ->
            "🚂 Switch to WDFC Electric Rail for this ${distance.toInt()}km route = -73% emissions."
        vehicleType == "cng_hcv" ->
            "✅ CNG is cleaner TTW but WTT offsets gains. Consider electric for max reduction."
        vehicleType == "electric_truck" ->
            "🏆 Zero direct emissions! Grid factor: 0.82 kg CO₂/kWh (CEA 2023)."
        vehicleType == "rail_electric" ->
            "🏆 Best-in-class! 73% less than diesel HCV. Check DFC availability."
        else -> "💡 Consolidate loads to improve load factor. Every 10% LF improvement = ~12% emission reduction."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
