package code.name.monkey.retromusic.fragments

import androidx.annotation.LayoutRes
import code.name.monkey.retromusic.R

enum class GridStyle(
    @param:LayoutRes @field:LayoutRes val layoutResId: Int,
    val id: Int,
) {
    Grid(layoutResId = R.layout.item_grid, id = 0),
    Card(layoutResId = R.layout.item_card, id = 1),
    ColoredCard(layoutResId = R.layout.item_card_color, id = 2),
    Circular(layoutResId = R.layout.item_grid_circle, id = 3),
    Image(layoutResId = R.layout.image, id = 4),
    GradientImage(layoutResId = R.layout.item_image_gradient, id = 5)
}
