package eu.kanade.tachiyomi.ui.reader.viewer.webgpu

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlin.math.abs

internal enum class SpreadPosition { LEFT, RIGHT, SINGLE }

internal object WebGpuSpreadRules {

    /** Above this, an untagged page is a spread already, not half of one. */
    const val WIDE_ASPECT = 1.2f

    /** How far two untagged pages' aspect ratios may differ and still pair. */
    const val PAIR_ASPECT_TOLERANCE = 0.1f

    fun taggedPosition(tag: ReaderPage.SpreadTag): SpreadPosition? = when (tag) {
        ReaderPage.SpreadTag.LEFT -> SpreadPosition.LEFT
        ReaderPage.SpreadTag.RIGHT -> SpreadPosition.RIGHT
        ReaderPage.SpreadTag.SINGLE -> SpreadPosition.SINGLE
        ReaderPage.SpreadTag.UNKNOWN, ReaderPage.SpreadTag.NONE -> null
    }

    fun isWide(size: ReaderPage.SourceSize?): Boolean =
        (size?.aspectRatio ?: 0f) > WIDE_ASPECT

    fun standsAlone(tag: ReaderPage.SpreadTag, size: ReaderPage.SourceSize?): Boolean =
        when (taggedPosition(tag)) {
            SpreadPosition.SINGLE -> true
            SpreadPosition.LEFT, SpreadPosition.RIGHT -> false
            null -> isWide(size)
        }

    fun startIndex(loneBefore: Int?): Int = loneBefore?.plus(1) ?: 1

    fun positionFor(
        index: Int,
        startIndex: Int,
        anchor: SpreadPosition,
        partner: SpreadPosition,
    ): SpreadPosition {
        val offset = index - startIndex
        return if (offset >= 0 && offset % 2 == 0) anchor else partner
    }

    fun canPairShapes(anchor: ReaderPage, partner: ReaderPage): Boolean {
        val anchorTagged = taggedPosition(anchor.spreadTag) != null
        val partnerTagged = taggedPosition(partner.spreadTag) != null
        if (anchorTagged && partnerTagged) return true
        val a = anchor.sourceSize?.aspectRatio ?: return true
        val b = partner.sourceSize?.aspectRatio ?: return true
        return abs(a - b) <= PAIR_ASPECT_TOLERANCE
    }

    fun effectivePosition(raw: SpreadPosition, hasPartner: Boolean): SpreadPosition =
        if (raw == SpreadPosition.SINGLE || hasPartner) raw else SpreadPosition.SINGLE
}
