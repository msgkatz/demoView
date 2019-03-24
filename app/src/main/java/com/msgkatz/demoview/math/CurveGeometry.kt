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
fun evalQuadAt(fraction: Float, startValue: Float2, midValue: Float2, endValue: Float2): Float2 {

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
fun evalQuadTangentAt(fraction: Float, startValue: Float2, midValue: Float2, endValue: Float2): Float2 {

    val B = midValue - startValue
    val A = endValue - midValue - B
    val T = A * fraction + B

    return T + T
}

/**
 * Calc tangent vector at a given fraction for quad bezier curve, which is set with given points start/mid/end
 * The derivative equation is 2(b - a +(a - 2b +c)t)
 *
 * 2nd version - seems not working
 *
 * @param fraction - t, time fraction
 * @param startValue - start point a of a curve
 * @param midValue - helper point b of a curve
 * @param endValue - end point c of a curve
 *
 * */
fun evalQuadTangentAtV2(fraction: Float, startValue: Float2, midValue: Float2, endValue: Float2): Float2 {

    val xDirect = 2 * (1 - fraction) * (midValue.x - startValue.x)
    + 2 * fraction * (endValue.x - midValue.x)

    val yDirect = 2 * (1 - fraction) * (midValue.y - startValue.y)
    + 2 * fraction * (endValue.y - midValue.y)

    return Float2(xDirect, yDirect)
}