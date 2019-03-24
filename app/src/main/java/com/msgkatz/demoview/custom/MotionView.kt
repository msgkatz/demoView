package com.msgkatz.demoview.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import com.msgkatz.demoview.R
import com.msgkatz.demoview.math.*
import com.msgkatz.demoview.utils.Parameters
import kotlin.math.roundToInt

class MotionView(context: Context, attrs: AttributeSet) : View(context, attrs), ViewTreeObserver.OnPreDrawListener {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLUE
    }
    private val paintDirection = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.GREEN
    }
    private lateinit var system: MotionSystem

    private val objectSize: Float = resources.getDimensionPixelSize(R.dimen.size_bounds_car_path).toFloat()
    private val objectPath: Path = DrawablePathHelper.getArrowOfSize(objectSize.roundToInt())

    init {
        viewTreeObserver.addOnPreDrawListener(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = View.MeasureSpec.getSize(heightMeasureSpec)
        val size = if (width > height) height else width
        setMeasuredDimension(size, size)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                system.updateEndPoint(Float2(event.x, event.y))
                invalidate()
                true
            }
            else -> super.onTouchEvent(event)
        }
    }

    override fun onPreDraw(): Boolean {

        viewTreeObserver.removeOnPreDrawListener(this)

        val sPoint = Float2(width/2f, height/1.5f)
        system = MotionSystem(sPoint, width.toFloat())

        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val newPoint = system.getNextPoint()
        canvas.translateAndRotate(translationX = newPoint.x,
                                  translationY = newPoint.y,
                                  rotation = newPoint.r) {
            drawPath(objectPath, paint)
        }

        if (Parameters.DEBUG)
            drawDirect(canvas)

        postInvalidateOnAnimation()
    }

    private fun drawDirect(canvas: Canvas) {
        val newPoint = system.getNextRay()
        canvas.drawLine(newPoint.origin.x, newPoint.origin.y, newPoint.direction.x, newPoint.direction.y, paintDirection)
    }

    private inline fun Canvas.translateAndRotate(translationX: Float = 0f,
                                                 translationY: Float = 0f,
                                                 rotation: Float = 0f,
                                                 draw: Canvas.() -> Unit) {
        val checkpoint = save()
        translate(translationX, translationY)
        rotate(rotation)
        try {
            draw()
        } finally {
            restoreToCount(checkpoint)
        }
    }

}

