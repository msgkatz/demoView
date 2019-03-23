package com.msgkatz.demoview.math


/**
 * Calc curve point at given fraction for quad bezier curve, which is set with given points start/mid/end
 * The quad bezier curve equation is Quad(t) = a(1 - t)^2 + 2b(1 - t)t + ct^2
 *
 * @param fraction - t, time fraction
 * @param startValue - start point a of a curve
 * @param midValue - helper point b of a curve
 * @param endValue - end point c of a curve
 *
 * */
private fun EvalQuadAt(fraction: Float, startValue: Float2, midValue: Float2, endValue: Float2): Float2 {

    return (startValue * (1.0f - fraction) * (1.0f - fraction)
            + midValue * 2.0f * (1.0f - fraction) * fraction
            + endValue * fraction * fraction)
}

/**
 * Calc tangent vector at a given fraction for quad bezier curve, which is set with given points start/mid/end
 * The derivative equation is 2(b - a +(a - 2b +c)t)
 *
 * Quad'(t) = 2(At + B), where
 *   A = (a - 2b + c)
 *   B = (b - a)
 *   Solve for t, only if it fits between 0 < t < 1
 *
 * @param fraction - t, time fraction
 * @param startValue - start point a of a curve
 * @param midValue - helper point b of a curve
 * @param endValue - end point c of a curve
 *
 * */
private fun EvalQuadTangentAt(fraction: Float, startValue: Float2, midValue: Float2, endValue: Float2): Float2 {

    val B = midValue - startValue
    val A = endValue - midValue - B
    val T = A * fraction + B

    return T + T
}