package com.msgkatz.demoview.custom

import android.graphics.Matrix
import android.graphics.Path

object DrawablePathHelper {

    private val carPath: Path
        get() = Path().apply {

            moveTo(0f, 42f)
            lineTo(0f, 12f)
            quadTo(0f, 0f, 6f, 0f)
            lineTo(15f, 0f)
            quadTo(21f, 0f, 21f, 12f)
            lineTo(21f, 42f)
            quadTo(21f, 50f, 15f, 50f)
            lineTo(6f, 50f)
            quadTo(0f, 50f, 0f, 42f)
            close()


            moveTo(3f, 16f)
            quadTo(10.5f, 14f, 18f, 16f)
            lineTo(15f, 28f)
            lineTo(6f, 28f)
            close()

            moveTo(6f, 30f)
            lineTo(15f, 30f)
            lineTo(15f, 40f)
            lineTo(6f, 40f)
            close()

        }

    private val arrowPath: Path
        get() = Path().apply {

            moveTo(0f, 0f)
            lineTo(10f, 10f)
            lineTo(0f, -20f)
            lineTo(-10f, 10f)
            close()
        }

    fun getCarOfSize(size: Int): Path {
        return carPath.apply {
            transform(uniformScaleMatrix(size.toFloat()))
        }
    }

    fun getArrowOfSize(size: Int): Path {
        return arrowPath.apply {
            transform(uniformScaleMatrix(size.toFloat()))
        }
    }

    /**
     * Creates a scale matrix with the scale factor [scale] on both the 'x' and 'y' axis
     */
    fun uniformScaleMatrix(scale: Float = 1.0f) = Matrix().apply { setScale(scale, scale) }



}