package com.soul.ocr

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply {
        color = 0xFFFFA500.toInt() // Orange color
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private var path: Path? = null

    fun updatePath(points: List<Pair<Float, Float>>) {
        path = Path().apply {
            if (points.size >= 4) {
                moveTo(points[0].first, points[0].second)
                for (i in 1 until points.size) {
                    lineTo(points[i].first, points[i].second)
                }
                close()
            }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path?.let { canvas.drawPath(it, paint) }
    }
    fun clear() {
        path = null
        invalidate()
    }

}