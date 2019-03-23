package com.msgkatz.demoview.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import com.msgkatz.demoview.R
import com.msgkatz.demoview.math.*
import kotlin.math.roundToInt

class MotionView(context: Context, attrs: AttributeSet) : View(context, attrs), ViewTreeObserver.OnPreDrawListener {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ballRadius: Double
        get() = Math.min(width, height) * 0.10 / 4.0
    private var finger: PointF = PointF()
    private lateinit var system: MotionSystem

    private val objectSize: Float = resources.getDimensionPixelSize(R.dimen.size_bounds_car_path).toFloat()
    //private val objectPath: Path = DrawablePathHelper.getCarOfSize(objectSize.roundToInt())
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
                finger.set(event.x, event.y)

                system.updateEndPoint(Float2.fromPointF(finger))

                invalidate() //TODO: do we really need to invalidate here? - seems like we do

                true
            }

            else -> super.onTouchEvent(event)
        }

    }

    override fun onPreDraw(): Boolean {
        viewTreeObserver.removeOnPreDrawListener(this)

        val sPoint = PointF(width/2f, height/1.5f)
        system = MotionSystem(Float2.fromPointF(sPoint), width.toFloat())

        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = System.currentTimeMillis()
        canvas.save()

        drawObject2(canvas, now)
        drawDirect(canvas)

        canvas.restore()
        postInvalidateOnAnimation()
    }

    private fun drawObject2(canvas: Canvas, time: Long) {
        val newPoint = system.getNextPoint()
        canvas.save()

        canvas.translate(newPoint.x, newPoint.y)
        canvas.rotate(newPoint.r)

        paint.style = Paint.Style.FILL
        paint.color = Color.BLUE
        canvas.drawPath(objectPath, paint)

        canvas.restore()
    }

    private fun drawDirect(canvas: Canvas) {
        val newPoint = system.getNextRay()
        canvas.save()

        paint.style = Paint.Style.FILL
        paint.color = Color.GREEN

        canvas.drawLine(newPoint.origin.x, newPoint.origin.y, newPoint.direction.x, newPoint.direction.y, paint)

        canvas.restore()
    }

    private fun drawObject(canvas: Canvas, time: Long) {
        val newPoint = system.getNextPoint()
        canvas.save()

        /**
        //canvas.rotate(newPoint.r, newPoint.x, newPoint.y)
        canvas.rotate(newPoint.r)
        canvas.translate(newPoint.x, newPoint.y)
         **/

        canvas.translate(newPoint.x, newPoint.y)
        canvas.rotate(newPoint.r)

        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        //canvas.drawPath(objectPath, paint)
        canvas.drawCircle(0f, 0f, ballRadius.toFloat(), paint)

        canvas.restore()
    }

    private inline fun Float2.Companion.fromPointF(point: PointF): Float2 {
        return Float2(point.x, point.y)
    }

}

