package org.krripe.shadowessentials.core.gui

import net.minecraft.screen.ScreenHandlerType

object GuiTypes {

    fun fromSize(size: Int): ScreenHandlerType<*> =
        when (size) {
            9  -> ScreenHandlerType.GENERIC_9X1
            18 -> ScreenHandlerType.GENERIC_9X2
            27 -> ScreenHandlerType.GENERIC_9X3
            36 -> ScreenHandlerType.GENERIC_9X4
            45 -> ScreenHandlerType.GENERIC_9X5
            54 -> ScreenHandlerType.GENERIC_9X6
            else -> throw IllegalArgumentException(
                "Invalid GUI size: $size (must be 9..54 and multiple of 9)"
            )
        }
}