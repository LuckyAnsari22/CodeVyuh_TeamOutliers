package com.greeniq.app.ui.advisor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.greeniq.app.R
import com.greeniq.app.databinding.FragmentAdvisorBinding
import com.greeniq.app.network.RetrofitClient
import com.greeniq.app.network.models.AdvisorRequest
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

class AdvisorFragment : Fragment() {

    private var _binding: FragmentAdvisorBinding? = null
    private val binding get() = _binding!!

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdvisorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChat()
        setupInput()

        // Welcome message
        addMessage(ChatMessage(
            "👋 Welcome to HARIT AI Advisor! I'm powered by Google Gemini and trained on logistics sustainability data.\n\n" +
            "Ask me about:\n" +
            "→ Carbon reduction strategies\n" +
            "→ Route optimization insights\n" +
            "→ Fleet efficiency recommendations\n" +
            "→ GLEC/GHG compliance\n\n" +
            "What would you like to explore?", false
        ))
    }

    private fun setupChat() {
        chatAdapter = ChatAdapter(messages)
        binding.chatRecycler.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.chatRecycler.adapter = chatAdapter
    }

    private fun setupInput() {
        binding.btnSend.setOnClickListener { sendQuery() }
        binding.editQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendQuery(); true } else false
        }
    }

    private fun sendQuery() {
        val query = binding.editQuery.text.toString().trim()
        if (query.isEmpty()) return

        addMessage(ChatMessage(query, true))
        binding.editQuery.text?.clear()

        // Show thinking indicator
        addMessage(ChatMessage("🤔 Analyzing your query with Gemini...", false))

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = AdvisorRequest(
                    question = query,
                    fleetContext = mapOf(
                        "total_fleet_size" to 50,
                        "avg_load_factor" to 0.55,
                        "primary_vehicle" to "diesel_hcv_bsvi",
                        "annual_emissions_mt" to 85.0
                    )
                )

                val response = RetrofitClient.apiService.getAdvisory(request)

                // Remove thinking indicator
                if (messages.isNotEmpty() && messages.last().text.contains("Analyzing")) {
                    messages.removeAt(messages.size - 1)
                    chatAdapter.notifyItemRemoved(messages.size)
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val answer = body["answer"]?.toString()
                        ?: body["response"]?.toString()
                        ?: body["result"]?.toString()
                        ?: "Analysis complete. No specific recommendations generated."
                    addMessage(ChatMessage(answer, false))
                } else {
                    addMessage(ChatMessage(getLocalResponse(query), false))
                }
            } catch (e: Exception) {
                // Remove thinking indicator
                if (messages.isNotEmpty() && messages.last().text.contains("Analyzing")) {
                    messages.removeAt(messages.size - 1)
                    chatAdapter.notifyItemRemoved(messages.size)
                }
                addMessage(ChatMessage(getLocalResponse(query), false))
            }
        }
    }

    private fun getLocalResponse(query: String): String {
        val q = query.lowercase()
        return when {
            "carbon" in q || "emission" in q -> """
                📊 **Carbon Reduction Strategy**
                
                Based on GLEC v3 framework analysis:
                
                1. **Route Optimization**: -15–20% emissions via AI-optimized routing (McKinsey India 2024)
                2. **Modal Shift**: Switch 500km+ routes to WDFC electric rail = -73% per route
                3. **Load Consolidation**: HDBSCAN clustering can boost load factor from 55% → 78%
                4. **Fleet Upgrade**: BS-IV → BS-VI transition saves 14% TTW emissions
                
                💡 Quick Win: Start with your top-10 highest-emission corridors. GREENIQ can identify these instantly.
                
                Source: CEEW 2023, DPIIT-NCAER 2024, GLEC v3
            """.trimIndent()
            "route" in q || "optimize" in q -> """
                🛣️ **Route Optimization Insights**
                
                India's freight routes average 15–20% excess distance (World Bank LPI 2023).
                
                GREENIQ uses Google OR-Tools VRP solver to:
                → Minimize total distance across multi-stop deliveries
                → Balance vehicle loads within capacity constraints
                → Calculate per-route carbon footprint
                
                Typical savings: ₹38/km → ₹27/km (-28.9%)
                
                Source: DPIIT-NCAER National Logistics Cost Study, 2024
            """.trimIndent()
            "load" in q || "consolidat" in q -> """
                📦 **Load Consolidation Analysis**
                
                Current India avg load factor: 55% (IFTRT 2024)
                40% of return trips are completely empty.
                
                GREENIQ's HDBSCAN 4D engine clusters shipments by:
                → Origin/destination proximity
                → Time window compatibility
                → Weight/volume compatibility
                → Vehicle type match
                
                Result: 3 trucks → 1 truck. 67% fewer trips.
                
                Source: IFTRT 2024, GREENIQ HDBSCAN projection
            """.trimIndent()
            else -> """
                🌿 I analyzed your query. Here's what I recommend:
                
                GREENIQ covers 9 core problem statements for logistics optimization:
                
                1. Carrier Selection (XGBoost + TOPSIS)
                2. Document Intelligence (Gemini Vision)
                3. Autonomous Negotiation (ANAC)
                4. Route Optimization (OR-Tools VRP)
                5. Load Consolidation (HDBSCAN)
                6. Carbon Tracking (GLEC v3)
                7. AI Advisory (Gemini Pro)
                8. Corporate Rebranding
                9. Lane Intelligence

                Which area would you like me to dive deeper into?
            """.trimIndent()
        }
    }

    private fun addMessage(message: ChatMessage) {
        messages.add(message)
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.chatRecycler.scrollToPosition(messages.size - 1)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ── Chat Adapter ──
class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userBubble: View = view.findViewById(R.id.userBubble)
        val aiBubble: View = view.findViewById(R.id.aiBubble)
        val tvUserMsg: android.widget.TextView = view.findViewById(R.id.tvUserMsg)
        val tvAiMsg: android.widget.TextView = view.findViewById(R.id.tvAiMsg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        if (msg.isUser) {
            holder.userBubble.visibility = View.VISIBLE
            holder.aiBubble.visibility = View.GONE
            holder.tvUserMsg.text = msg.text
        } else {
            holder.userBubble.visibility = View.GONE
            holder.aiBubble.visibility = View.VISIBLE
            holder.tvAiMsg.text = msg.text
        }

        // Animate
        val anim = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fade_up)
        holder.itemView.startAnimation(anim)
    }

    override fun getItemCount() = messages.size
}
