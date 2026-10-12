package com.shilapi.xcertplay

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The mask is checked by painting onto a real bitmap rather than by reading its fields: the pixels a
 * driver sees are the contract.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], qualifiers = "en-w1280dp-h720dp", manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DisplayCornerMaskTest {

    private fun activity(): Activity = Robolectric.buildActivity(Activity::class.java).setup().get()

    /** The mask, laid out over a [width] x [height] area, as the host places it. */
    private fun mask(
        activity: Activity,
        width: Int,
        height: Int,
        radiusPx: Int,
        content: RectF = RectF(0f, 0f, width.toFloat(), height.toFloat()),
    ): DisplayCornerMask = DisplayCornerMask(activity).apply {
        this.radiusPx = radiusPx
        setContent(content.left, content.top, content.right, content.bottom)
        measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        layout(0, 0, width, height)
    }

    private fun render(view: View, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    @Test fun theFourCornersArePaintedBlackAndThePictureIsLeftAlone() {
        val activity = activity()
        val view = mask(activity, 400, 200, radiusPx = 40)
        val bitmap = render(view, 400, 200)

        // Just inside each corner along the diagonal is outside the rounded shape, so it is covered.
        assertEquals(Color.BLACK, bitmap.getPixel(2, 2))
        assertEquals(Color.BLACK, bitmap.getPixel(397, 2))
        assertEquals(Color.BLACK, bitmap.getPixel(2, 197))
        assertEquals(Color.BLACK, bitmap.getPixel(397, 197))
        // The middle and the middle of each edge stay clear, so nothing but the corners is hidden.
        assertEquals(0, bitmap.getPixel(200, 100))
        assertEquals(0, bitmap.getPixel(200, 1))
        assertEquals(0, bitmap.getPixel(200, 198))
        assertEquals(0, bitmap.getPixel(1, 100))
        assertEquals(0, bitmap.getPixel(398, 100))
        // A point the rounded shape contains is not covered either. At 40 px the curve crosses the
        // diagonal about 12 px from the corner, so from 20 px in the picture is untouched.
        assertEquals(0, bitmap.getPixel(20, 20))
    }

    @Test fun aSquareCornerSettingPaintsNothing() {
        val activity = activity()
        val bitmap = render(mask(activity, 200, 100, radiusPx = 0), 200, 100)
        for (x in intArrayOf(0, 1, 199)) {
            for (y in intArrayOf(0, 1, 99)) {
                assertEquals("pixel $x,$y", 0, bitmap.getPixel(x, y))
            }
        }
    }

    @Test fun aLargerRadiusCoversMoreOfEachCorner() {
        val activity = activity()
        val small = render(mask(activity, 400, 200, radiusPx = 20), 400, 200)
        val large = render(mask(activity, 400, 200, radiusPx = 60), 400, 200)
        // The width of the black band along the top edge and along the corner diagonal is the radius's
        // visible effect, so it is measured rather than assumed: the exact pixel depends on the
        // rasterizer, the direction of the change does not.
        val smallRow = blackRun(small, y = 2)
        val largeRow = blackRun(large, y = 2)
        val smallDiagonal = blackRun(small, diagonal = true)
        val largeDiagonal = blackRun(large, diagonal = true)

        assertTrue("row bands: $smallRow then $largeRow", largeRow > smallRow)
        assertTrue("diagonal bands: $smallDiagonal then $largeDiagonal", largeDiagonal > smallDiagonal)
        // A 20 px radius cannot cover more than about a sixth of a 200-pixel-tall picture's corner.
        assertTrue("row band $smallRow", smallRow in 1..20)
    }

    /** How many pixels from the corner are still covered, along the top edge or the diagonal. */
    private fun blackRun(bitmap: Bitmap, y: Int = 0, diagonal: Boolean = false): Int {
        var run = 0
        while (run < 200) {
            val pixel = if (diagonal) bitmap.getPixel(run, run) else bitmap.getPixel(run, y)
            if (pixel != Color.BLACK) break
            run++
        }
        return run
    }

    @Test fun aLetterboxedPictureIsRoundedAtItsOwnCornersOnly() {
        val activity = activity()
        // A 1920x1080 picture fitted into a 1280x800 window: bars above and below.
        val content = RectF(0f, 40f, 1280f, 760f)
        val bitmap = render(mask(activity, 1280, 800, radiusPx = 40, content = content), 1280, 800)

        assertEquals(Color.BLACK, bitmap.getPixel(2, 42))
        assertEquals(Color.BLACK, bitmap.getPixel(1277, 757))
        // The bars themselves are never painted over.
        assertEquals(0, bitmap.getPixel(1, 20))
        assertEquals(0, bitmap.getPixel(1278, 780))
        assertEquals(0, bitmap.getPixel(640, 400))
    }

    @Test fun aContentRectangleWithoutAreaIsSkipped() {
        val activity = activity()
        val bitmap = render(mask(activity, 200, 100, radiusPx = 40, content = RectF(10f, 10f, 10f, 10f)), 200, 100)
        assertEquals(0, bitmap.getPixel(1, 1))
        assertEquals(0, bitmap.getPixel(100, 50))
    }

    @Test fun aNegativeRadiusIsTreatedAsOff() {
        val view = mask(activity(), 200, 100, radiusPx = -5)
        assertEquals(0, view.radiusPx)
        val bitmap = render(view, 200, 100)
        assertEquals(0, bitmap.getPixel(1, 1))
    }

    @Test fun thePaintIsOpaqueBlack() {
        // Read from the live object: a translucent or non-black paint would let the iPhone's own
        // rounded corner show through the mask.
        val view = mask(activity(), 400, 200, radiusPx = 40)
        val field = DisplayCornerMask::class.java.getDeclaredField("paint").apply { isAccessible = true }
        val paint = field.get(view) as android.graphics.Paint
        assertEquals(Color.BLACK, paint.color)
        assertEquals(255, paint.alpha)
        assertTrue(paint.isAntiAlias)
        assertEquals(android.graphics.Paint.Style.FILL, paint.style)
    }

    @Test fun theMaskNeverTakesTouchesOrAccessibilityFocus() {
        val view = mask(activity(), 400, 200, radiusPx = 40)
        assertFalse(view.isClickable)
        assertFalse(view.isFocusable)
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, view.importantForAccessibility)
    }

    @Test fun theMaskIsAddedLastAndIsReused() {
        val activity = activity()
        val root = FrameLayout(activity)
        val video = View(activity)
        root.addView(video, FrameLayout.LayoutParams(-1, -1))
        val first = DisplayCornerMask.attach(root)
        // Last, so nothing the content view built earlier draws below it.
        assertEquals(1, root.indexOfChild(first))
        root.addView(View(activity), FrameLayout.LayoutParams(-1, -1))
        assertEquals(1, root.indexOfChild(first))

        assertSame(first, DisplayCornerMask.attach(root))
        assertEquals(3, root.childCount)
    }

    @Test fun theSameMaskIsKeptInFrontWhenTheVideoSurfaceIsReplaced() {
        // A SurfaceView fallback replaces the TextureView, so the video child changes. The host brings
        // the same mask to the front instead of a second one being added for a surface that is gone.
        val activity = activity()
        val root = FrameLayout(activity)
        val texture = View(activity)
        root.addView(texture, FrameLayout.LayoutParams(-1, -1))
        val mask = DisplayCornerMask.attach(root)
        val viewport = FrameLayout(activity)
        root.addView(viewport, FrameLayout.LayoutParams(-1, -1))
        root.bringChildToFront(mask)

        val reattached = DisplayCornerMask.attach(root)
        assertSame(mask, reattached)
        assertEquals(2, root.indexOfChild(reattached))
        assertEquals(3, root.childCount)
    }
}
