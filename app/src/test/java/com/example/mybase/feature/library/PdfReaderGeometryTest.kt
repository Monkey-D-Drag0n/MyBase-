package com.example.mybase.feature.library

import androidx.compose.ui.geometry.Offset
import com.example.mybase.core.model.PdfTextHighlightBounds
import com.example.mybase.data.document.PdfPageSize
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfReaderGeometryTest {
    @Test fun legacyPagePointBoundsNormalizeAndStayAlignedAcrossRenderSizes() {
        val normalized = normalizedTextHighlightBounds(
            listOf(PdfTextHighlightBounds(72f, 144f, 216f, 288f)),
            PdfPageSize(612, 792)
        ).single()
        assertEquals(72f / 612f, normalized.left, .0001f)
        assertEquals(144f / 792f, normalized.top, .0001f)
        val atOneX = textHighlightBoundsAtRenderSize(normalized, 612, 792)
        val atTwoX = textHighlightBoundsAtRenderSize(normalized, 1224, 1584)
        assertEquals(atOneX.left * 2, atTwoX.left, .001f)
        assertEquals(atOneX.bottom * 2, atTwoX.bottom, .001f)
    }

    @Test fun alreadyNormalizedTextBoundsAreNotScaledAsPagePoints() {
        val source = PdfTextHighlightBounds(.12f, .2f, .6f, .31f)
        assertEquals(listOf(source), normalizedTextHighlightBounds(listOf(source), PdfPageSize(612, 792)))
    }

    @Test fun zoomPanStayWithinContentBoundsAndFitResetsBothAxes() {
        val zoomed = PdfPageTransform().zoomBy(3f)
        val panned = zoomed.panBy(Offset(10_000f, -10_000f), 1000f, 1600f)
        assertEquals(1f, panned.panXFraction)
        assertEquals(-1f, panned.panYFraction)
        assertEquals(1000f, panned.translationX(1000f), .01f)
        assertEquals(-1600f, panned.translationY(1600f), .01f)
        assertEquals(PdfPageTransform(), panned.fit())
    }

    @Test fun inverseViewportMappingTracksPageAcrossZoomPan() {
        val transform = PdfPageTransform(zoom = 2f, panXFraction = .25f, panYFraction = -.25f)
        val normalized = pagePointFromViewport(Offset(500f, 400f), 1000f, 800f, transform)
        assertEquals(.4375f, normalized.x, .001f)
        assertEquals(.5625f, normalized.y, .001f)
    }

    @Test fun savedAnnotationCanBeCenteredInsideZoomedPage() {
        assertEquals(-.8f, panFractionToRevealPageY(.8f, 4f), .001f)
        assertEquals(1f, panFractionToRevealPageY(0f, 4f), .001f)
        assertEquals(0f, panFractionToRevealPageY(.8f, 1f), .001f)
    }
}
