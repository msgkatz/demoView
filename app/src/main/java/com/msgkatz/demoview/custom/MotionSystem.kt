package com.msgkatz.demoview.custom

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import com.msgkatz.demoview.math.*

class MotionSystem(private var startPoint: Ray,
                   private val maxWidth: Float
) {
    constructor(startPoint: Float2, width: Float) : this(
        Ray(startPoint, calcStartingDirection(startPoint), Math.PI), width)

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

    fun updateEndPoint(newPoint: Float2) {
        startPoint = curPoint

        val endDirection = when {
            curPoint.origin.y > newPoint.y -> Float2(newPoint.x - 10, newPoint.y - 10)
            else -> Float2(newPoint.x + 10, newPoint.y + 10)
        }

        endPoint = Ray(newPoint, endDirection)
        updateAnimator()
        Log.d("new points::", "curPoint: ${curPoint}, endPoint: ${endPoint}")
    }

    fun getNextPoint(): Float2Rotation {
        return curPointCalculated
    }

    fun getNextRay(): Ray {
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

        var newAnimator = ValueAnimator.ofObject(MotionEvaluator(bezierParam, pathMeasure), startValue, endValue).apply {
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

            return evalQuadTangentAt(fraction, startValue, bezierParams, endValue)
        }
    }

}

class MotionEvaluator(private val bezierParams: Float2, private val pathMeasure: PathMeasure): TypeEvaluator<Ray> {


    override fun evaluate(fraction: Float, startValue: Ray, endValue: Ray): Ray {

        val point = evalQuadAt(fraction, startValue.origin, bezierParams, endValue.origin)

        val xyDirect = recalcDirectV1(fraction, startValue, endValue)
        val xyDirect2 = recalcDirectV2(fraction, startValue, endValue)

        val xDirect = xyDirect.x
        val yDirect = xyDirect.y

        var pos: FloatArray = FloatArray(2)
        var tan: FloatArray = FloatArray(2)
        pathMeasure.getPosTan(pathMeasure.length * fraction, pos, tan)

        val angle = Math.atan2(tan[1].toDouble(), tan[0].toDouble())
        val angle3 = Math.atan2(xyDirect2.y.toDouble(), xyDirect2.x.toDouble())

        val angle2 = Math.atan2((-yDirect + point.y).toDouble(), (-xDirect + point.x).toDouble())
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

        return Float2(xDirect, yDirect) //+ Float2(xDirect, yDirect)
    }

    private fun recalcDirectV2(fraction: Float, startValue: Ray, endValue: Ray): Float2
        = evalQuadTangentAt(fraction, startValue.origin, bezierParams, endValue.origin)

}