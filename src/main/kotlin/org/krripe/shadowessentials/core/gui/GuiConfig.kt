package org.krripe.shadowessentials.core.gui

object GuiConfig {

    data class Button(
        val slot: Int,
        val icon: String,
        val name: String,
        val lore: List<String> = emptyList(),
        val modelData: Int? = null,
        val glow: Boolean = false,
        val hideAdditionalTooltip: Boolean = true,
        val hideTooltip: Boolean = false
    )

    data class Filler(
        val name: String = " ",
        val lore: List<String> = emptyList(),
        val icon: String = "minecraft:gray_stained_glass_pane",
        val slots: List<String> = emptyList(),
        val modelData: Int? = null,
        val glow: Boolean = false,
        val hideAdditionalTooltip: Boolean = true,
        val hideTooltip: Boolean = false
    )

    data class PagedScreen(
        val title: String,
        val size: Int = 54,
        val fillersEnabled: Boolean = true,
        val fillers: MutableList<Filler> = mutableListOf(),
        val nextPageButton: Button? = null,
        val prevPageButton: Button? = null,
        val closeButton: Button? = null
    )
}
