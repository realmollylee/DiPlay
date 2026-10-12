package com.shilapi.xcertplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayCornerRadiusTest {
    @Test fun theRadiusFollowsTheShorterSideSoATurnedPanelStaysRounded() {
        // A 1920x720 panel and the same panel turned: the shorter side sets the curve in both.
        assertEquals(29, DisplayCornerRadius.radiusPx(DisplayCornerRadius.DEFAULT_PERCENT, 1920, 720))
        assertEquals(29, DisplayCornerRadius.radiusPx(DisplayCornerRadius.DEFAULT_PERCENT, 720, 1920))
    }

    @Test fun zeroDisablesTheMask() {
        assertEquals(0, DisplayCornerRadius.radiusPx(0, 1920, 720))
    }

    @Test fun aRequestedRadiusIsClampedToTheStoredRange() {
        assertEquals(DisplayCornerRadius.MAX_PERCENT, DisplayCornerRadius.clampPercent(99))
        assertEquals(DisplayCornerRadius.MIN_PERCENT, DisplayCornerRadius.clampPercent(-4))
        // radiusPx clamps too, so a stale preference cannot over-round the picture: 99% of 720 would
        // be 713 px, and the whole store is read through these two entry points.
        assertEquals(
            DisplayCornerRadius.radiusPx(DisplayCornerRadius.MAX_PERCENT, 1920, 720),
            DisplayCornerRadius.radiusPx(99, 1920, 720),
        )
    }

    @Test fun noAllowedRadiusCanBendPastTheMiddleOfThePicture() {
        // Two opposite corners meet once the radius reaches half the shorter side. The stored range
        // stays well short of that on every panel size, so no clamp is needed; this fails if a later
        // change raises MAX_PERCENT that far.
        for (shortSide in listOf(200, 480, 720, 1080, 2160)) {
            val radius = DisplayCornerRadius.radiusPx(DisplayCornerRadius.MAX_PERCENT, shortSide * 3, shortSide)
            assertTrue("radius $radius on a $shortSide-pixel side", radius <= shortSide / 2)
        }
    }

    @Test fun aDisplayWithoutAreaHasNoRadius() {
        assertEquals(0, DisplayCornerRadius.radiusPx(DisplayCornerRadius.DEFAULT_PERCENT, 0, 0))
        assertEquals(0, DisplayCornerRadius.radiusPx(DisplayCornerRadius.DEFAULT_PERCENT, 1920, 0))
    }

    @Test fun theMaxRadiusStaysSubtleOnA720PixelPanel() {
        // The largest value the slider offers on a 1920x720 panel: a visible curve rather than a frame.
        assertEquals(86, DisplayCornerRadius.radiusPx(DisplayCornerRadius.MAX_PERCENT, 1920, 720))
    }
}
