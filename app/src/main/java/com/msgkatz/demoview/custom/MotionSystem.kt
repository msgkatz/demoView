package com.msgkatz.demoview.custom

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.graphics.Path
import android.graphics.PathMeasure
import android.view.animation.AccelerateDecelerateInterpolator
import com.msgkatz.demoview.math.*
import com.msgkatz.demoview.utils.Logs

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
        Logs.d("new points::", "curPoint: ${curPoint}, endPoint: ${endPoint}")
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

        val xyPoint = evalQuadAt(fraction, startValue.origin, bezierParams, endValue.origin)
        val xyDirection = evalQuadTangentAt(fraction, startValue.origin, bezierParams, endValue.origin)

        var pos: FloatArray = FloatArray(2)
        var tan: FloatArray = FloatArray(2)
        pathMeasure.getPosTan(pathMeasure.length * fraction, pos, tan)

        /**
         * xyDirection-based angle calculation works ok,
         * but PathMeasure-based angle calculation gives little bit smoother result
         */
        val angle = Math.atan2(tan[1].toDouble(), tan[0].toDouble())
        val angle2 = Math.atan2(xyDirection.y.toDouble(), xyDirection.x.toDouble())

        Logs.d("tanEq::", "x: (${xyDirection.x} or ${tan[0]}, y: ${xyDirection.y} or ${tan[1]}); angles: $angle or $angle2")

        return Ray(xyPoint, xyDirection, angle)
    }

}