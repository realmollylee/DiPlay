// SPDX-License-Identifier: AGPL-3.0-only
package com.shilapi.xcertplay

import android.content.Context
import kotlin.math.roundToInt

/**
 * The display's rounded corners, used by CarPlay's picture and the parked video player.
 *
 * CarPlay draws its own rounded corners over the wallpaper colour, so a screen without a declared
 * corner radius shows a coloured wedge in each corner of an otherwise black picture. A radius tells
 * the iPhone the picture may use the whole panel, and [DisplayCornerMask] then paints the corners
 * the panel cannot show black, so the picture meets the head unit's own bezel.
 *
 * The radius is a percentage of the display's shorter side rather than a pixel or dp value: it
 * follows the window size the way a physical bezel does, and it does not depend on the density a
 * head unit reports.
 */
internal object DisplayCornerRadius {
    /** Rounded by default: the corners of a head unit's panel are not square. */
    const val DEFAULT_PERCENT = 4

    /** 0 disables the mask and lets the iPhone keep its own rounded corners. */
    const val MIN_PERCENT = 0

    /**
     * The slider's largest value. 12% of the shorter side is about 86 px on a 720-pixel-tall panel,
     * which leaves room to match a bezel without the picture reading as a picture frame; the whole
     * range also stays far short of the half-shorter-side point where two opposite corners would
     * meet, so a stored value never needs clamping to the display it is drawn on.
     */
    const val MAX_PERCENT = 12

    fun clampPercent(percent: Int): Int = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)

    fun loadPercent(context: Context): Int {
        // Before this setting existed, the experimental "Square corners, no safe area" switch was the
        // only way to ask for either. Its state carries over: it asked for square corners, and a
        // driver who turned it off keeps the rounded corners they already had.
        if (!AirPlayPersistence.hasDisplayCornerRadiusPercent(context)) {
            return if (AirPlayPersistence.loadDisplayCornerRadiusLegacySquare(context)) 0
            else DEFAULT_PERCENT
        }
        return clampPercent(AirPlayPersistence.loadDisplayCornerRadiusPercent(context))
    }

    fun savePercent(context: Context, percent: Int) {
        AirPlayPersistence.saveDisplayCornerRadiusPercent(context, clampPercent(percent))
    }

    /**
     * The corner radius in pixels for one display area. A zero result means no mask is drawn.
     *
     * The shorter side keeps a wide panel and a turned one rounded by the same amount. [MAX_PERCENT]
     * is small enough that two opposite corners cannot meet in the middle of a picture.
     */
    fun radiusPx(percent: Int, widthPx: Int, heightPx: Int): Int {
        val shortSide = minOf(widthPx, heightPx)
        if (shortSide <= 0) return 0
        return (clampPercent(percent) / 100f * shortSide).roundToInt()
    }
}
