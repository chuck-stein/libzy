package io.libzy.ui.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.chip.ChipGroup

class DynamicChipGroup(context: Context, attributes: AttributeSet) : ChipGroup(context, attributes) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, MeasureSpec.getSize(heightMeasureSpec))
    }

    override fun onLayout(sizeChanged: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(sizeChanged, left, top, right, bottom)

        fun chipIsOffScreen(chipIndex: Int) =
            chipIndex > 0 && getChildAt(chipIndex).bottom > measuredHeight

        // remove any chips beyond the bottom of the scroll view window, so they fill up the whole space but no more
        var chipIndex = childCount - 1
        while (chipIsOffScreen(chipIndex)) {
            removeViewAt(chipIndex)
            chipIndex--
        }
    }
}
