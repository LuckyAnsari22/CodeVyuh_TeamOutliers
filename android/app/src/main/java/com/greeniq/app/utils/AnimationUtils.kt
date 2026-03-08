package com.greeniq.app.utils

import android.animation.ValueAnimator
import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.animation.DecelerateInterpolator
import android.widget.TextView

/**
 * Apply a pink→cyan gradient to text using a LinearGradient shader.
 */
fun TextView.applyGradientText() {
    paint.shader = LinearGradient(
        0f, 0f, paint.measureText(text.toString()), textSize,
        intArrayOf(0xFFE5417A.toInt(), 0xFF07D8F6.toInt()),
        null,
        Shader.TileMode.CLAMP
    )
}

/**
 * Animate a counter from 0 to target value.
 */
fun animateCounter(
    target: Long,
    duration: Long = 2000,
    onUpdate: (Long) -> Unit
) {
    val animator = ValueAnimator.ofFloat(0f, target.toFloat())
    animator.duration = duration
    animator.interpolator = DecelerateInterpolator()
    animator.addUpdateListener { onUpdate((it.animatedValue as Float).toLong()) }
    animator.start()
}
