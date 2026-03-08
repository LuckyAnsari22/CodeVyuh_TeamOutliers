package com.greeniq.app.ui.iot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.greeniq.app.R
import com.greeniq.app.databinding.FragmentIotArchitectureBinding

class IotArchitectureFragment : Fragment() {

    private var _binding: FragmentIotArchitectureBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIotArchitectureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Optional: Animate cards appearing from bottom on load
        val rootLayout = binding.root as ViewGroup
        val linearLayout = rootLayout.getChildAt(0) as ViewGroup
        
        for (i in 0 until linearLayout.childCount) {
            val child = linearLayout.getChildAt(i)
            val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_up)
            animation.startOffset = (i * 100).toLong()
            child.startAnimation(animation)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
