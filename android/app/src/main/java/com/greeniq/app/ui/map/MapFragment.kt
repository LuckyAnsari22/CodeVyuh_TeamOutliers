package com.greeniq.app.ui.map

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.greeniq.app.R
import com.greeniq.app.databinding.FragmentMapBinding
import com.greeniq.app.gl.RoadMapRenderer

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var renderer: RoadMapRenderer
    private val fpsHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        renderer = RoadMapRenderer()
        binding.glMapView.setEGLContextClientVersion(2)
        binding.glMapView.setRenderer(renderer)
        binding.glMapView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        setupControls()
        startFpsUpdater()

        // Pulse the live dot
        pulseLiveDot()
    }

    private fun setupControls() {
        binding.btnNeutral.setOnClickListener {
            renderer.currentState = RoadMapRenderer.MapState.NEUTRAL
            highlightButton(binding.btnNeutral)
        }

        binding.btnDisease.setOnClickListener {
            renderer.currentState = RoadMapRenderer.MapState.DISEASED
            highlightButton(binding.btnDisease)
        }

        binding.btnHeal.setOnClickListener {
            renderer.currentState = RoadMapRenderer.MapState.HEALING
            highlightButton(binding.btnHeal)
        }

        highlightButton(binding.btnNeutral)
    }

    private fun highlightButton(activeBtn: View) {
        listOf(binding.btnNeutral, binding.btnDisease, binding.btnHeal).forEach { btn ->
            btn.alpha = if (btn == activeBtn) 1f else 0.5f
        }
    }

    private fun startFpsUpdater() {
        fpsHandler.post(object : Runnable {
            override fun run() {
                binding.tvFps.text = "${renderer.currentFps} FPS"
                val color = if (renderer.currentFps >= 30)
                    requireContext().getColor(R.color.health_green)
                else
                    requireContext().getColor(R.color.sick_red)
                binding.tvFps.setTextColor(color)
                fpsHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun pulseLiveDot() {
        binding.liveDot.animate()
            .alpha(0.3f)
            .setDuration(800)
            .withEndAction {
                binding.liveDot.animate()
                    .alpha(1f)
                    .setDuration(800)
                    .withEndAction { pulseLiveDot() }
                    .start()
            }
            .start()
    }

    override fun onResume() {
        super.onResume()
        binding.glMapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.glMapView.onPause()
        fpsHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fpsHandler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
