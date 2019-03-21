package com.msgkatz.demoview.custom

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import com.msgkatz.demoview.R
import kotlin.math.roundToInt




class MovingPointView3(context: Context, attrs: AttributeSet) : View(context, attrs), ViewTreeObserver.OnPreDrawListener {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ballRadius: Double
        get() = Math.min(width, height) * 0.10 / 4.0
    private var finger: PointF = PointF()
    private lateinit var system: MotionSystem3

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
        system = MotionSystem3(sPoint, dPoint)

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

    private fun drawDirect(canvas: Canvas) {
        val newPoint = system.getNextPoint2()
        canvas.save()

        //canvas.translate(newPoint.origin.x, newPoint.origin.y)

        //paint.style = Paint.Style.STROKE //.FILL
        paint.style = Paint.Style.FILL
        paint.color = Color.GREEN

        canvas.drawLine(newPoint.origin.x, newPoint.origin.y, newPoint.direction.x, newPoint.direction.y, paint)


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

/**
data class Float2(var x: Float = 0.0f, var y: Float = 0.0f) {
    constructor(v: Float) : this(v, v)
    constructor(v: Float2) : this(v.x, v.y)
}

data class Ray(var origin: Float2 = Float2(), var direction: Float2) {

    fun toPointF(): PointF {
        return PointF(origin.x, origin.y)
    }
}
**/


class MotionSystem3(private var startPoint: Ray
) {
    //TODO: use calcDirectionDelta only when searching for interim bezier point !!!
    //TODO: ie exclude calcDirectionDelta from constructor
//    constructor(startPointF: PointF, directionPointF: PointF) : this(Ray(Float2(startPointF.x, startPointF.y),
//        calcDirectionDelta(startPointF, directionPointF)))

    constructor(startPointF: PointF, directionPointF: PointF) : this(Ray(Float2(startPointF.x, startPointF.y),
        Float2(directionPointF.x, directionPointF.y)))


    private var animator: ValueAnimator? = null

    private var curPoint: Ray
    private var endPoint: Ray
    private var curPointCalculated: Float2Rotation
    private var pathMeasure: PathMeasure = PathMeasure()

    init {
        curPoint = startPoint
        endPoint = startPoint
        curPointCalculated = Float2Rotation(startPoint.origin.x, startPoint.origin.y, 0.0f)
        //Math.toDegrees(calcAngle(startPoint)).toFloat())
    }

    fun updateEndPoint(newPoint: PointF) {
        startPoint = curPoint


        val endDirection = when {
            curPoint.origin.y > newPoint.y -> Float2(newPoint.x, newPoint.y - 1)
            else -> Float2(newPoint.x, newPoint.y + 1)
        }
        //endPoint = Ray(Float2(newPoint.x, newPoint.y), Float2())
        endPoint = Ray(Float2(newPoint.x, newPoint.y), endDirection)
        updateAnimator()
        Log.d("new points::", "curPoint: ${curPoint}, endPoint: ${endPoint}")
    }

    fun getNextPoint(time: Long): Float2Rotation {
        return curPointCalculated
    }

    fun getNextPoint2(): Ray {
        return curPoint
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

//        val bezierParam = Float2(Math.min(startPoint.origin.x, endPoint.origin.x) + Math.abs(startPoint.origin.x - endPoint.origin.x),
//            Math.min(startPoint.origin.y, endPoint.origin.y) + Math.abs(startPoint.origin.y - endPoint.origin.y))

        buildPath(bezierParam)

        var newAnimator = ValueAnimator.ofObject(MotionEvaluator3(bezierParam, pathMeasure), startValue, endValue).apply {
            duration = 800 * 2
            interpolator = AccelerateDecelerateInterpolator()
            //addUpdateListener { invalidate() }
            addUpdateListener {
                val interimValue = animatedValue as Ray
                curPoint = interimValue

                val angleDeg = Math.toDegrees(interimValue.realAngle).toFloat()
                var t = 360 - angleDeg
                if (t < 0) {
                    t += 360f
                }
                if (t > 360) {
                    t -= 360f
                }
                //help smooth everything out
                t = t.toInt().toFloat()
                t = t / 5
                t = t.toInt().toFloat()
                t = t * 5
                curPointCalculated = Float2Rotation(interimValue.origin.x, interimValue.origin.y,
                    //t)
                    Math.toDegrees(interimValue.realAngle).toFloat() + 90)
                    //Math.toDegrees(calcAngle(interimValue)).toFloat())


//                curPointCalculated = Float2Rotation(interimValue.origin.x, interimValue.origin.y,
//                    curPointCalculated.r - Math.toDegrees(clamp(prevAngle - newAngle, - MAX_ANGLE, MAX_ANGLE)).toFloat())
            }
        }

        animator = newAnimator
        animator?.start()
    }

    private fun buildPath(param: Float2) {
        val p: Path = Path().apply {
            moveTo(startPoint.origin.x, startPoint.origin.y)
            quadTo(param.x, param.y, endPoint.origin.x, endPoint.origin.y)
        }

        pathMeasure.setPath(p, false)


        //pathMeasure.getPosTan()

    }



    companion object {

        private val MAX_ANGLE = 1e-1

        fun calcAngle(ray: Ray): Double {
//            val deltaX = (ray.origin.x - ray.direction.x).toDouble()
//            val deltaY = (ray.origin.y - ray.direction.y).toDouble()

            val deltaX = (ray.direction.x - ray.origin.x).toDouble()
            val deltaY = (ray.direction.y - ray.origin.y).toDouble()

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
                        Float2(0.0f, 1.0f)
                    else
                        Float2(0.0f, -1.0f)
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
                        Float2(0.0f, -1.0f)
                    else
                        Float2(0.0f, 1.0f)
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

class MotionEvaluator3(private val bezierParams: Float2, private val pathMeasure: PathMeasure): TypeEvaluator<Ray> {


    override fun evaluate(fraction: Float, startValue: Ray, endValue: Ray): Ray {
        //Log.d("fraction=", "$fraction")
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

        var pos: FloatArray = FloatArray(2)
        var tan: FloatArray = FloatArray(2)
        pathMeasure.getPosTan(pathMeasure.length * fraction, pos, tan)

        val angle = Math.atan2(tan[1].toDouble(), tan[0].toDouble())
        val angle2 = Math.atan2((-yDirect + y).toDouble(), (-xDirect + x).toDouble())

        //Log.d("tanEq::", "x: (${xDirect - x} or ${tan[0]}, y: ${yDirect - y} or ${tan[1]}); angles: $angle or $angle2")
        Log.d("direct&&angle::", "Direct: ($xDirect or ${pos[0]}, $yDirect or ${pos[1]}); Angle: Rad=${angle}, Deg=${Math.toDegrees(angle)}")
        return Ray(Float2(x, y), Float2(xDirect, yDirect), angle)
    }

}