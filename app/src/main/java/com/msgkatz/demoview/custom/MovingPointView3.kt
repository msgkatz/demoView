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
import com.msgkatz.demoview.math.*
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
        val dPoint = PointF(width/2.5f, -height/2.5f)
        system = MotionSystem3(sPoint, dPoint, width.toFloat())

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

class MotionSystem3(private var startPoint: Ray,
                    private val maxWidth: Float
) {
    constructor(startPointF: PointF, directionPointF: PointF, width: Float) : this(Ray(Float2(startPointF.x, startPointF.y),
        calcStartingDirection(Float2(directionPointF.x, directionPointF.y)),
        Math.PI), width)


    private var animator: ValueAnimator? = null

    private var curPoint: Ray
    private var endPoint: Ray
    private var curPointCalculated: Float2Rotation
    private var pathMeasure: PathMeasure = PathMeasure()

    init {
        curPoint = startPoint
        endPoint = startPoint
        curPointCalculated = Float2Rotation(startPoint.origin.x, startPoint.origin.y, 0.0f)
    }

    fun updateEndPoint(newPoint: PointF) {
        startPoint = curPoint


        val endDirection = when {
            curPoint.origin.y > newPoint.y -> Float2(newPoint.x - 10, newPoint.y - 10)
            else -> Float2(newPoint.x + 10, newPoint.y + 10)
        }

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

        val bezierParam = when {
            (startPoint.realAngle > (Math.PI/2 + Math.PI/2) && startPoint.realAngle < (3 * Math.PI/2 + Math.PI/2)) -> {
                pointAt(startPoint, -startPoint.origin.x)
            }
            else -> {
                pointAt(startPoint, maxWidth - startPoint.origin.x)
            }
        }

        buildPath(bezierParam)

        var newAnimator = ValueAnimator.ofObject(MotionEvaluator3(bezierParam, pathMeasure), startValue, endValue).apply {
            duration = 800 * 2
            interpolator = AccelerateDecelerateInterpolator()
            //addUpdateListener { invalidate() }
            addUpdateListener {
                val interimValue = animatedValue as Ray
                curPoint = interimValue
                curPointCalculated = Float2Rotation(interimValue.origin.x, interimValue.origin.y,
                    Math.toDegrees(interimValue.realAngle).toFloat() + 90)

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

    }

    companion object {

        fun calcStartingDirection(startValue: Float2): Float2 {

            val fraction = 0.0f
            val endValue = Float2(0.0f, startValue.y - startValue.x)
            val bezierParams = Float2(startValue.x, endValue.y)

            val B = bezierParams - startValue
            val A = endValue - bezierParams - B
            val T = A * fraction + B

            val result = T + T

            return result
        }
    }

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

        val point = (startValue.origin * (1.0f - fraction) * (1.0f - fraction)
                + bezierParams * 2.0f * (1.0f - fraction) * fraction
                + endValue.origin * fraction * fraction)

        val xyDirect = recalcDirectV1(fraction, startValue, endValue)
        val xyDirect2 = recalcDirectV2(fraction, startValue, endValue)

        val xDirect = xyDirect.x
        val yDirect = xyDirect.y

        var pos: FloatArray = FloatArray(2)
        var tan: FloatArray = FloatArray(2)
        pathMeasure.getPosTan(pathMeasure.length * fraction, pos, tan)

        val angle = Math.atan2(tan[1].toDouble(), tan[0].toDouble())
        val angle2 = Math.atan2((-yDirect + y).toDouble(), (-xDirect + x).toDouble())
        val angle3 = Math.atan2(xyDirect2.y.toDouble(), xyDirect2.x.toDouble())
        val angle4 = Math.atan2(yDirect.toDouble(), xDirect.toDouble())

        Log.d("tanEq::", "x: (${xDirect} or ${xyDirect2.x} or ${tan[0]}, y: ${yDirect} or ${xyDirect2.y} or ${tan[1]}); angles: $angle or $angle3 or $angle4 or $angle2")
        //Log.d("direct&&angle::", "Direct: ($xDirect or ${xyDirect.x} or ${pos[0]} or $x, $yDirect or ${xyDirect.y} or ${pos[1]} or $y); Angle: Rad=${angle}, Deg=${Math.toDegrees(angle)}")

        return Ray(point, xyDirect2, angle)
        //return Ray(Float2(x, y), xyDirect2, angle)
    }

    private fun recalcDirectV1(fraction: Float, startValue: Ray, endValue: Ray): Float2 {
        val xDirect = 2 * (1 - fraction) * (bezierParams.x - startValue.origin.x)
        + 2 * fraction * (endValue.origin.x - bezierParams.x)

        val yDirect = 2 * (1 - fraction) * (bezierParams.y - startValue.origin.y)
        + 2 * fraction * (endValue.origin.y - bezierParams.y)

        return Float2(xDirect, yDirect) + Float2(xDirect, yDirect)
    }

    private fun recalcDirectV2(fraction: Float, startValue: Ray, endValue: Ray): Float2 {

        val B = bezierParams - startValue.origin
        val A = endValue.origin - bezierParams - B
        val T = A * fraction + B

        return T + T
    }

}