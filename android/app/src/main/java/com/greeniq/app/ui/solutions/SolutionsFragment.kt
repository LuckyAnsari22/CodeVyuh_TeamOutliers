package com.greeniq.app.ui.solutions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.greeniq.app.R
import com.greeniq.app.databinding.FragmentSolutionsBinding
import com.greeniq.app.network.RetrofitClient
import com.greeniq.app.network.models.SolutionItem
import kotlinx.coroutines.launch

class SolutionsFragment : Fragment() {

    private var _binding: FragmentSolutionsBinding? = null
    private val binding get() = _binding!!

    private val solutions = listOf(
        SolutionItem("ps1", "🎯", "Carrier Selection", "AI-powered carrier scoring using XGBoost + TOPSIS multi-criteria analysis with SHAP explainability.",
            listOf("XGBoost predictive scoring", "TOPSIS ranking", "SHAP explanations"), "PS1 · ML", "/api/v1/ps1/score"),
        SolutionItem("ps2", "📄", "Document Intelligence", "Gemini Vision extracts structured data from lorry receipts, PODs, and invoices with confidence scoring.",
            listOf("OCR + Gemini Vision", "Field extraction", "Document matching"), "PS2 · AI", "/api/v1/ps2/match"),
        SolutionItem("ps3", "🤝", "Autonomous Negotiation", "ANAC-protocol negotiation agents that simulate realistic multi-round freight rate negotiations.",
            listOf("ANAC protocols", "Multi-round simulation", "Nash equilibrium"), "PS3 · Game Theory", "/api/v1/ps3/scenarios"),
        SolutionItem("ps4", "🛣️", "Route Optimization", "Google OR-Tools VRP solver minimizing distance, time, and emissions across multi-stop delivery routes.",
            listOf("OR-Tools VRP", "Multi-vehicle routing", "Carbon-aware planning"), "PS4 · Optimization", "/api/v1/ps4/demo"),
        SolutionItem("ps5", "📦", "Load Consolidation", "HDBSCAN 4D geospatial clustering merges compatible shipments — 67% fewer trips, 23pt load factor boost.",
            listOf("HDBSCAN clustering", "FFD bin packing", "4D geospatial"), "PS5 · ML", "/api/v1/ps5/demo"),
        SolutionItem("ps6", "⚡", "Carbon Tracker", "GLEC Framework v3 compliant WTW emissions calculator with real-time grading and fleet-level analytics.",
            listOf("GLEC v3 / GHG Protocol", "WTW methodology", "Carbon grading A-F"), "PS6 · Analytics", "/api/v1/ps6/demo"),
        SolutionItem("ps7", "🤖", "HARIT AI Advisor", "Gemini-powered strategic sustainability advisor providing data-driven, cited, actionable recommendations.",
            listOf("Gemini Pro", "Strategic analysis", "Cited recommendations"), "PS7 · GenAI", "/api/v1/ps7/global-data"),
        SolutionItem("ps8", "🎨", "Corporate Rebranding", "AI-driven brand positioning analysis and sustainability communication strategy generator.",
            listOf("Brand analysis", "Sustainability comms", "ESG alignment"), "PS8 · Strategy", "/api/v1/ps8/brand-assets"),
        SolutionItem("ps9", "🧹", "Lane Intelligence", "Automated data cleaner for freight lane records — dedup, normalize, and enrich historical shipment data.",
            listOf("Data deduplication", "Normalization", "Quality scoring"), "PS9 · Data", "/api/v1/ps9/demo")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSolutionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.solutionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.solutionsRecycler.adapter = SolutionsAdapter(solutions) { solution ->
            runDemo(solution)
        }
    }

    private fun runDemo(solution: SolutionItem) {
        Toast.makeText(requireContext(), "⚡ Running ${solution.title}...", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = when (solution.id) {
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
                    showResult("✅ ${solution.title}", "Server responded successfully!\n\n${response.body().toString().take(400)}")
                } else {
                    showResult("📱 ${solution.title}", getOfflineData(solution.id))
                }
            } catch (e: Exception) {
                // Backend not running — show rich offline demo data
                showResult("📱 ${solution.title}", getOfflineData(solution.id))
            }
        }
    }

    private fun showResult(title: String, content: String) {
        val ctx = context ?: return
        try {
            androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setTitle(title)
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
                    getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                        ctx.getColor(R.color.accent_cyan))
                }
        } catch (e: Exception) {
            Toast.makeText(ctx, content.take(200), Toast.LENGTH_LONG).show()
        }
    }

    private fun getOfflineData(id: String): String = when (id) {
        "ps1" -> "Carriers Scored: 15\n\n1. Rivigo Express - 0.93/1.0\n   On-time: 94% | Rs38/km | Grade: A\n2. Delhivery Freight - 0.87/1.0\n   On-time: 91% | Rs42/km | Grade: B+\n3. TCI Express - 0.82/1.0\n   On-time: 88% | Rs36/km | Grade: B\n\nSHAP: On-time (34%) > Cost (28%) > Carbon (22%)"
        "ps2" -> "Documents Processed: 3\n\nLR-2024-7891: 12/12 fields | 97.3%\nPOD-0451: 10/10 fields | 95.8%\nINV-2024-3312: 15/15 fields | 98.1%\n\nCross-match: 100% | Avg: 2.3s/doc"
        "ps3" -> "Scenario: Mumbai to Delhi FTL\n\nR1: Rs58,000 vs Rs42,000\nR2: Rs52,000 vs Rs45,000\nR3: Rs48,500 vs Rs47,000\nR4: DEAL at Rs47,800\n\nSavings: 17.6% vs market rate"
        "ps4" -> "Depot: Mumbai | 3 vehicles | 12 stops\n\nV1: Mumbai-Pune-Kolhapur-Belgaum\n  604 km | 22.4T | 267 kg CO2\nV2: Mumbai-Nashik-Aurangabad-Nagpur\n  838 km | 24.1T | 371 kg CO2\nV3: Mumbai-Surat-Vadodara-Ahmedabad\n  524 km | 19.8T | 232 kg CO2\n\nSaved: 23.4% distance, 18.7% carbon"
        "ps5" -> "Input: 47 shipments to 16 clusters\nTrucks: 47 to 16 (66% reduction)\n\nTop: Mumbai-Delhi corridor\n  8 shipments to 2 FTL trucks\n  Load factor: 55% to 89%\n  CO2 saved: 1,847 kg\n\n31 trips eliminated\nFuel: 12,400L saved"
        "ps6" -> "Route: Mumbai to Delhi (1,400 km)\nVehicle: BS-VI HCV | 18T\n\nTTW: 784.0 kg CO2\nWTT: 176.4 kg CO2\nTotal WTW: 960.4 kg CO2e\n\nFactor: 0.0381 kg/ton.km\nGrade: B+ (51.7% below avg)"
        "ps7" -> "Top 3 Recommendations:\n\n1. WDFC rail for Mumbai-Delhi: -73% emissions\n2. HDBSCAN consolidation: empty 45% to 12%\n3. BS-IV to BS-VI upgrade: -14% TTW\n\nProjected: Rs2.8 Cr saved, 4,200t CO2 reduced"
        "ps8" -> "ESG Score: 42 to 78/100\n\n1. GLEC v3 certified reporting\n2. Carbon grade on invoices\n3. Quarterly sustainability report\n4. SmartFreight certification\n\n+15% retention, +23% new clients"
        "ps9" -> "Input: 12,847 lane records\n\nDuplicates: 847 (6.6%)\nNormalized: 12,000 records\nQuality: 94.2/100\n\nTop: Mumbai-Delhi (2,341/mo)\nWasteful: Kolkata-Chennai (37% empty)\nHigh CO2: Delhi-Bangalore (1,240 kg avg)"
        else -> "Demo data loaded successfully"
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ── Adapter ──
class SolutionsAdapter(
    private val items: List<SolutionItem>,
    private val onDemoClick: (SolutionItem) -> Unit
) : RecyclerView.Adapter<SolutionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvIcon)
        val tvTag: TextView = view.findViewById(R.id.tvTag)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val featuresContainer: LinearLayout = view.findViewById(R.id.featuresContainer)
        val tvAction: TextView = view.findViewById(R.id.tvAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_solution_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvIcon.text = item.icon
        holder.tvTag.text = item.tag
        holder.tvTitle.text = item.title
        holder.tvDescription.text = item.description

        // Features
        holder.featuresContainer.removeAllViews()
        item.features.forEach { feature ->
            val tv = TextView(holder.itemView.context).apply {
                text = "→  $feature"
                setTextColor(holder.itemView.context.getColor(R.color.text_muted))
                textSize = 12f
                setPadding(0, 2, 0, 2)
            }
            holder.featuresContainer.addView(tv)
        }

        holder.tvAction.text = "Run Demo →"
        holder.itemView.setOnClickListener { onDemoClick(item) }

        // Stagger animation
        val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fade_up)
        animation.startOffset = (position * 100).toLong()
        holder.itemView.startAnimation(animation)
    }

    override fun getItemCount() = items.size
}
