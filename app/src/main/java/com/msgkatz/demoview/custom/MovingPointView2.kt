package com.msgkatz.demoview.custom

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import com.msgkatz.demoview.R
import com.msgkatz.demoview.math.Float2
import com.msgkatz.demoview.math.Ray
import kotlin.math.roundToInt

class MovingPointView2(context: Context, attrs: AttributeSet) : View(context, attrs), ViewTreeObserver.OnPreDrawListener {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ballRadius: Double
        get() = Math.min(width, height) * 0.10 / 4.0
    private var finger: PointF = PointF()
    private lateinit var system: MotionSystem2

    private val objectSize: Float = resources.getDimensionPixelSize(R.dimen.size_bounds_car_path).toFloat()
    //private val objectPath: Path = DrawablePathHelper.getCarOfSize(objectSize.roundToInt())
    private val objectPath: Path = DrawablePathHelper.getArrowOfSize(objectSize.roundToInt())

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

        val sPoint = PointF(width/2f, height/1.5f)
        val dPoint = PointF(width/2f, height/2f)
        system = MotionSystem2(sPoint, dPoint)

        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = System.currentTimeMillis()
        canvas.save()

        drawObject2(canvas, now)

        canvas.restore()
        postInvalidateOnAnimation()
    }

    private fun drawObject2(canvas: Canvas, time: Long) {
        val newPoint = system.getNextPoint(time)
        canvas.save()

        canvas.translate(newPoint.x, newPoint.y)
        canvas.rotate(newPoint.r)
        //paint.style = Paint.Style.STROKE //.FILL
        paint.style = Paint.Style.FILL
        paint.color = Color.BLUE
        canvas.drawPath(objectPath, paint)


        canvas.restore()
    }

    private fun drawObject(canvas: Canvas, time: Long) {
        val newPoint = system.getNextPoint(time)
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

}




class MotionSystem2(private var startPoint: Ray
) {
    //TODO: use calcDirectionDelta only when searching for interim bezier point !!!
    //TODO: ie exclude calcDirectionDelta from constructor
//    constructor(startPointF: PointF, directionPointF: PointF) : this(Ray(Float2(startPointF.x, startPointF.y),
//        calcDirectionDelta(startPointF, directionPointF)))

    constructor(startPointF: PointF, directionPointF: PointF) : this(Ray(Float2(startPointF.x, startPointF.y),
        Float2(directionPointF.x, directionPointF.y)
    ))


    private var animator: ValueAnimator? = null

    private var curPoint: Ray
    private var endPoint: Ray
    private var curPointCalculated: Float2Rotation

    init {
        curPoint = startPoint
        endPoint = startPoint
        curPointCalculated = Float2Rotation(startPoint.origin.x, startPoint.origin.y, 0.0f)
            //Math.toDegrees(calcAngle(startPoint)).toFloat())
    }

    fun updateEndPoint(newPoint: PointF) {
        startPoint = curPoint
        endPoint = Ray(Float2(newPoint.x, newPoint.y), Float2())
        updateAnimator()
    }

    fun getNextPoint(time: Long): Float2Rotation {
        return curPointCalculated
    }

    private fun updateAnimator() {
        animator?.end()

        val startValue = startPoint
        val endValue = endPoint

        val directionDelta = calcDirectionDelta(startPoint)

        val bezierParam = when {
            startPoint.origin.x == endPoint.origin.x -> {
                val dy = Math.abs(startPoint.origin.y - endPoint.origin.y)
                pointAt(startPoint, dy, directionDelta)
            }
            else -> {
                val dx = Math.abs(startPoint.origin.x - endPoint.origin.x)
                pointAt(startPoint, dx, directionDelta)
            }
        }

        var newAnimator = ValueAnimator.ofObject(MotionEvaluator2(bezierParam), startValue, endValue).apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            //addUpdateListener { invalidate() }
            addUpdateListener {
                val interimValue = animatedValue as Ray

                val prevAngle = calcAngle(curPoint)
                val newAngle = calcAngle(interimValue)

                curPoint = interimValue

//                curPointCalculated = Float2Rotation(interimValue.origin.x, interimValue.origin.y,
//                    Math.toDegrees(calcAngle(interimValue)).toFloat())
                curPointCalculated = Float2Rotation(interimValue.origin.x, interimValue.origin.y,
                    curPointCalculated.r - Math.toDegrees(clamp(prevAngle - newAngle, - MAX_ANGLE, MAX_ANGLE)).toFloat())
            }
        }

        animator = newAnimator
        animator?.start()
    }



    companion object {

        private val MAX_ANGLE = 1e-1

        fun calcAngle(ray: Ray): Double {
            val deltaX = (ray.origin.x - ray.direction.x).toDouble()
            val deltaY = (ray.origin.y - ray.direction.y).toDouble()
            return Math.atan2(deltaY, deltaX)
        }

        fun clamp(value: Double, min: Double, max: Double): Double {
            if (value < min) {
                return min
            }
            return if (value > max) {
                max
            } else value
        }

        fun calcDirectionDelta(ray: Ray): Float2 {
            val deltaX = ray.direction.x - ray.origin.x
            val deltaY = ray.direction.y - ray.origin.y

            return when (deltaX) {
                0.0f -> {
                    val angle = calcAngle(ray)
                    if (angle > 0 && angle < Math.PI)
                        Float2(0.0f, -1.0f)
                    else
                        Float2(0.0f, 1.0f)
                }
                else -> Float2(1.0f, deltaY/deltaX)
            }
        }

        fun calcDirectionDelta(p0: PointF, p1: PointF): Float2 {
            val deltaX = p1.x - p0.x
            val deltaY = p1.y - p0.y

            return when (deltaX) {
                0.0f -> {
                    val angle = calcAngle(Ray(Float2(p0.x, p0.y), Float2(p1.x, p1.y)))
                    if (angle > 0 && angle < Math.PI)
                        Float2(0.0f, 1.0f)
                    else
                        Float2(0.0f, -1.0f)
                }
                else -> Float2(1.0f, deltaY/deltaX)
            }
        }

        fun pointAt(ray: Ray, delta: Float, directionDelta: Float2): Float2 {
            return Float2(ray.origin.x + directionDelta.x * delta, ray.origin.y + directionDelta.y * delta)
        }
    }

//    private fun calcDirectionDelta(p0: PointF, p1: PointF): Float2 {
//        val deltaX = p1.x - p0.x
//        val deltaY = p1.y - p0.y
//
//        return Float2(1.0f, deltaY/deltaX)
//    }
}

class MotionEvaluator2(private val bezierParams: Float2): TypeEvaluator<Ray> {


    override fun evaluate(fraction: Float, startValue: Ray, endValue: Ray): Ray {
        val x = ((1 - fraction) * (1 - fraction) * startValue.origin.x
                + 2 * (1 - fraction) * fraction * bezierParams.x
                + fraction * fraction * endValue.origin.x)
        val y = ((1 - fraction) * (1 - fraction) * startValue.origin.y
                + 2 * (1 - fraction) * fraction * bezierParams.y
                + fraction * fraction * endValue.origin.y)

        val xDirect = 2 * (1 - fraction) * (bezierParams.x - startValue.origin.x)
        + 2 * fraction * (endValue.origin.x - bezierParams.x)

        val yDirect = 2 * (1 - fraction) * (bezierParams.y - startValue.origin.y)
        + 2 * fraction * (endValue.origin.y - bezierParams.y)

        return Ray(Float2(x, y), Float2(xDirect, yDirect))
    }

}