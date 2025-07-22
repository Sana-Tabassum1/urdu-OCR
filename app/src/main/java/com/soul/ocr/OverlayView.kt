package com.soul.ocr

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var rects: List<Rect> = emptyList()
    private var cornerPoints: List<PointF>? = null

    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    fun setCorners(points: List<PointF>?) {
        cornerPoints = points
        invalidate()
    }
    fun setRects(rects: List<Rect>) {
        this.rects = rects
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val paint = Paint().apply {
            color = Color.GREEN
            strokeWidth = 6f
            style = Paint.Style.STROKE
        }

        rects.forEach {
            canvas.drawRect(it, paint)
        }
    }


}

