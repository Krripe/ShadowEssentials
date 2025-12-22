package org.krripe.shadowessentials.modules.home

data class HomeLocation(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val createdAt: Long
)
