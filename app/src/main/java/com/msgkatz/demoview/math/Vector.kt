package com.msgkatz.demoview.math

import kotlin.math.abs
import kotlin.math.sqrt

data class Float2(var x: Float = 0.0f, var y: Float = 0.0f) {
    constructor(v: Float) : this(v, v)
    constructor(v: Float2) : this(v.x, v.y)

    inline operator fun plus(v: Float) = Float2(x + v, y + v)
    inline operator fun minus(v: Float) = Float2(x - v, y - v)
    inline operator fun times(v: Float) = Float2(x * v, y * v)
    inline operator fun div(v: Float) = Float2(x / v, y / v)

    inline operator fun plus(v: Float2) = Float2(x + v.x, y + v.y)
    inline operator fun minus(v: Float2) = Float2(x - v.x, y - v.y)
    inline operator fun times(v: Float2) = Float2(x * v.x, y * v.y)
    inline operator fun div(v: Float2) = Float2(x / v.x, y / v.y)
}

inline fun abs(v: Float2) = Float2(abs(v.x), abs(v.y))
inline fun length(v: Float2) = sqrt(v.x * v.x + v.y * v.y)
inline fun length2(v: Float2) = v.x * v.x + v.y * v.y
inline fun distance(a: Float2, b: Float2) = length(a - b)
inline fun dot(a: Float2, b: Float2) = a.x * b.x + a.y * b.y

fun normalize(v: Float2): Float2 {
    val l = 1.0f / length(v)
    return Float2(v.x * l, v.y * l)
}