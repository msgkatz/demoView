package com.msgkatz.demoview.math


data class Ray(var origin: Float2 = Float2(), var direction: Float2, var realAngle: Double = 0.0)

fun pointAt(r: Ray, t: Float) = r.origin + r.direction * t