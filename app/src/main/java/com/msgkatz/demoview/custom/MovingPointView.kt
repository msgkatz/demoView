package com.msgkatz.demoview.custom

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import com.msgkatz.demoview.math.Float2Rotation


class MovingPointView(context: Context, attrs: AttributeSet) : View(context, attrs), ViewTreeObserver.OnPreDrawListener {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ballRadius: Double
        get() = Math.min(width, height) * 0.10 / 4.0
    private var finger: PointF = PointF()
    private lateinit var system: MotionSystem

    private val customAnimator: ValueAnimator = ValueAnimator().apply {

    }

    init {
        //val sPoint = PointF(width/2f, height/2f)
        //system = MotionSystem(sPoint, sPoint, sPoint)
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

                system.updateEndPoint(finger)
                //calc new moving path

                invalidate() //TODO: do we really need to invalidate here? - seems like we do

                true
            }

            else -> super.onTouchEvent(event)
        }

    }

    override fun onPreDraw(): Boolean {
        viewTreeObserver.removeOnPreDrawListener(this)

        val sPoint = PointF(width/2f, height/2f)
        system = MotionSystem(sPoint, sPoint, sPoint)

        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = System.currentTimeMillis()
        canvas.save()

        drawObject(canvas, now)

        canvas.restore()
        postInvalidateOnAnimation()
    }

    private fun drawObject(canvas: Canvas, time: Long) {
        val newPoint = system.getNextPoint(time)
        canvas.save()

        canvas.translate(newPoint.x, newPoint.y)
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawCircle(0f, 0f, ballRadius.toFloat(), paint)

        canvas.restore()
    }

}

class MotionSystem constructor(var startPoint: PointF,
                               var endPoint: PointF,
                               var curPoint: PointF,
                               private var speed: Float = 0.0f
) {

    //private var vt: VelocityTracker = null

    private var animator: ValueAnimator? = null

    fun updateEndPoint(newPoint: PointF) {
        startPoint = curPoint
        endPoint = newPoint
        updateAnimator()

        //curPoint = newPoint
        //endPoint = newPoint
    }

    fun getNextPoint(time: Long): PointF {

        return when {
            (curPoint == endPoint) -> curPoint
            else -> recalcNextPoint(time)
        }

    }

    private fun recalcNextPoint(time: Long): PointF {
        return curPoint
    }

    private fun updateAnimator() {
        animator?.end()

        val bezierParams = Float2Rotation(Math.min(startPoint.x, endPoint.x) + Math.abs(startPoint.x - endPoint.x),
                                          Math.min(startPoint.y, endPoint.y) + Math.abs(startPoint.y - endPoint.y))
        val startValue = Float2Rotation(startPoint.x, startPoint.y)
        val endValue = Float2Rotation(endPoint.x, endPoint.y)
        var newAnimator = ValueAnimator.ofObject(MotionEvaluator(bezierParams), startValue, endValue).apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            //addUpdateListener { invalidate() }
            addUpdateListener {
                val interimValue = animatedValue as Float2Rotation
                curPoint = PointF(interimValue.x, interimValue.y)
            }
        }

        animator = newAnimator
        animator?.start()
    }
}




class MotionEvaluator(private val bezierParams: Float2Rotation): TypeEvaluator<Float2Rotation> {


    override fun evaluate(fraction: Float, startValue: Float2Rotation, endValue: Float2Rotation): Float2Rotation {
        val x = ((1 - fraction) * (1 - fraction) * startValue.x + 2 * (1 - fraction) * fraction * bezierParams.x + fraction * fraction * endValue.x)
        val y = ((1 - fraction) * (1 - fraction) * startValue.y + 2 * (1 - fraction) * fraction * bezierParams.y + fraction * fraction * endValue.y)
        return Float2Rotation(x, y)
    }

}




