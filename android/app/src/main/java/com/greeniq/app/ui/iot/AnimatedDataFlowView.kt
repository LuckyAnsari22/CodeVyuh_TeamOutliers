package com.greeniq.app.ui.iot

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class AnimatedDataFlowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#4DFFFFFF") // Subtle border color (30% white)
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#07D8F6") // Accent cyan
    }

    private val path = Path()
    private var progress = 0f
    private var animator: ValueAnimator? = null
    
    // Properties that can be set via XML attributes if needed, but hardcoded for now for simplicity
    var dotColor: Int = Color.parseColor("#07D8F6")
        set(value) {
            field = value
            dotPaint.color = value
            invalidate()
        }

    init {
        // Parse custom attributes if we had them
        context.theme.obtainStyledAttributes(
            attrs,
            intArrayOf(android.R.attr.colorAccent), // Hack: Just grabbing a color for now, ideally make custom styleable
            0, 0
        ).apply {
            try {
                // If we defined a custom attribute for color, we'd read it here.
            } finally {
                recycle()
            }
        }
        
        startAnimation()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500 // 1.5 seconds for a dot to travel down
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                progress = animation.animatedValue as Float
                // Also shift the dash phase slightly for a moving line effect, optional
                linePaint.pathEffect = DashPathEffect(floatArrayOf(15f, 15f), -(progress * 30f))
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val centerX = w / 2f
        
        // Ensure we draw multiple dots based on height so it looks like a continuous stream
        val numDots = Math.max(1, (h / 100f).toInt()) // roughly one dot every 100px

        // Draw the background dashed line
        path.reset()
        path.moveTo(centerX, 0f)
        path.lineTo(centerX, h)
        canvas.drawPath(path, linePaint)

        // Draw the moving dots
        for (i in 0 until numDots + 1) { // +1 to ensure coverage
            // Calculate relative offset for this dot
            val offset = i.toFloat() / numDots
            
            // Current position combines the animation progress and the dot's offset
            var currentPos = (progress + offset) % 1.0f
            
            // Actual Y coordinate
            val dotY = currentPos * h
            
            // Fade in at top, fade out at bottom
            val alpha = when {
                currentPos < 0.1f -> (currentPos * 10f * 255).toInt()
                currentPos > 0.9f -> ((1f - currentPos) * 10f * 255).toInt()
                else -> 255
            }
            
            dotPaint.alpha = alpha.coerceIn(0, 255)
            canvas.drawCircle(centerX, dotY, 6f, dotPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
