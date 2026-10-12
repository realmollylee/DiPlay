// SPDX-License-Identifier: AGPL-3.0-only
package com.shilapi.xcertplay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup

/**
 * Paints the four corners a rounded picture cannot use, black, over CarPlay's picture or the parked
 * video player.
 *
 * The iPhone keeps drawing a rectangle; only the corners outside the rounded shape are covered, so
 * no frame is copied or scaled and the picture itself is untouched. The view takes no touches and
 * stays out of the accessibility tree, and on a panel that is already black behind the picture the
 * painted corners are indistinguishable from the bezel.
 */
internal class DisplayCornerMask(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val path = Path()

    /** The radius to paint in pixels; 0 paints nothing. */
    var radiusPx: Int = 0
        set(value) {
            val clamped = value.coerceAtLeast(0)
            if (field == clamped) return
            field = clamped
            invalidate()
        }

    /**
     * The rectangle whose corners are rounded, in this view's coordinates. A picture fitted inside a
     * larger window leaves the letterbox bars alone; only its own corners are covered.
     */
    private var contentLeft = 0f
    private var contentTop = 0f
    private var contentRight = 0f
    private var contentBottom = 0f

    fun setContent(left: Float, top: Float, right: Float, bottom: Float) {
        if (left == contentLeft && top == contentTop && right == contentRight && bottom == contentBottom) {
            return
        }
        contentLeft = left
        contentTop = top
        contentRight = right
        contentBottom = bottom
        invalidate()
    }

    init {
        // The picture underneath keeps every gesture.
        isClickable = false
        isFocusable = false
        isHapticFeedbackEnabled = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = radiusPx
        if (radius <= 0) return
        val left = contentLeft
        val top = contentTop
        val right = contentRight
        val bottom = contentBottom
        if (right - left <= 0f || bottom - top <= 0f) return

        // The rectangle covers the whole picture and the rounded shape is cut out of it, so only the
        // four corners are left to fill.
        path.reset()
        path.fillType = Path.FillType.EVEN_ODD
        path.addRect(left, top, right, bottom, Path.Direction.CW)
        val outer = RectF(left, top, right, bottom)
        path.addRoundRect(outer, radius.toFloat(), radius.toFloat(), Path.Direction.CW)
        canvas.drawPath(path, paint)
    }

    companion object {
        /**
         * Adds a mask over the picture in [parent] as its last child, so no bar or panel the host adds
         * draws below it, and returns the mask already there rather than adding a second one.
         *
         * The host keeps it last with [ViewGroup.bringChildToFront] whenever it lays the picture out
         * again, which is also what keeps it above a video surface the host replaces.
         */
        fun attach(parent: ViewGroup): DisplayCornerMask {
            for (index in 0 until parent.childCount) {
                val child = parent.getChildAt(index)
                if (child is DisplayCornerMask) return child
            }
            val mask = DisplayCornerMask(parent.context)
            parent.addView(mask, ViewGroup.LayoutParams(-1, -1))
            return mask
        }
    }
}
