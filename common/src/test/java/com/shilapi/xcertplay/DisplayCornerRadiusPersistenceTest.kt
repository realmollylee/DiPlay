package com.shilapi.xcertplay

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], manifest = Config.NONE)
class DisplayCornerRadiusPersistenceTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test fun aFreshInstallRoundsTheCorners() {
        // The corners of a head unit's panel are not square, so the default is the fix: a screen that
        // needs square corners sets 0.
        assertEquals(DisplayCornerRadius.DEFAULT_PERCENT, DisplayCornerRadius.loadPercent(context))
    }

    @Test fun aSavedRadiusIsReadBack() {
        DisplayCornerRadius.savePercent(context, 7)
        assertEquals(7, DisplayCornerRadius.loadPercent(context))
    }

    @Test fun aSavedRadiusIsClampedToTheAllowedRange() {
        DisplayCornerRadius.savePercent(context, 999)
        assertEquals(DisplayCornerRadius.MAX_PERCENT, DisplayCornerRadius.loadPercent(context))
        DisplayCornerRadius.savePercent(context, -3)
        assertEquals(DisplayCornerRadius.MIN_PERCENT, DisplayCornerRadius.loadPercent(context))
    }

    @Test fun aStoredValueFromAnotherBuildIsClampedOnRead() {
        AirPlayPersistence.saveDisplayCornerRadiusPercent(context, 5000)
        assertEquals(DisplayCornerRadius.MAX_PERCENT, DisplayCornerRadius.loadPercent(context))
    }

    @Test fun theRetiredSquareCornersSwitchCarriesOverToARadiusOfZero() {
        // A driver who had turned on "Square corners, no safe area" asked for square corners, and must
        // not silently get rounded ones after updating. The stored radius then takes over.
        writeRetiredSquareCornersSwitch(true)

        assertEquals(0, DisplayCornerRadius.loadPercent(context))

        DisplayCornerRadius.savePercent(context, 6)
        assertEquals(6, DisplayCornerRadius.loadPercent(context))
    }

    @Test fun theLegacySwitchIsIgnoredOnceARadiusIsSaved() {
        writeRetiredSquareCornersSwitch(true)
        DisplayCornerRadius.savePercent(context, 0)
        assertEquals(0, DisplayCornerRadius.loadPercent(context))

        // Turning the legacy value off cannot change a saved radius.
        writeRetiredSquareCornersSwitch(false)
        assertEquals(0, DisplayCornerRadius.loadPercent(context))
    }

    /**
     * Writes the retired switch's key the way the shipped app wrote it, so the migration is exercised
     * through the stored value rather than the app's own writer (which no longer exists). The key is
     * written literally on purpose: if the reader's key changes, this test must fail rather than keep
     * passing against a file nothing reads.
     */
    private fun writeRetiredSquareCornersSwitch(enabled: Boolean) {
        context.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE).edit()
            .putBoolean("main_display_disable_safe_area", enabled)
            .apply()
    }
}
