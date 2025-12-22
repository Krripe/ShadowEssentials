package org.krripe.shadowessentials.modules.spawn

data class SpawnLocation(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float
)