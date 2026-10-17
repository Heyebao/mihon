package eu.kanade.tachiyomi.ui.reader.viewer.webgpu

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class WebGpuSpreadRulesTest {

    @Test
    fun `wide uses a strict aspect threshold`() {
        WebGpuSpreadRules.isWide(size(110, 100)) shouldBe false
        WebGpuSpreadRules.isWide(size(120, 100)) shouldBe false
        WebGpuSpreadRules.isWide(size(121, 100)) shouldBe true
        WebGpuSpreadRules.isWide(size(140, 100)) shouldBe true
        WebGpuSpreadRules.isWide(size(100, 100)) shouldBe false
        WebGpuSpreadRules.isWide(null) shouldBe false
    }

    @Test
    fun `a 1_1 aspect page stays pairable rather than wide`() {
        val oneOne = size(110, 100)
        WebGpuSpreadRules.isWide(oneOne) shouldBe false
        WebGpuSpreadRules.standsAlone(ReaderPage.SpreadTag.NONE, oneOne) shouldBe false

        layout(
            listOf(
                page(0, portrait),
                page(1, oneOne),
                page(2, oneOne),
            ),
        ) shouldBe listOf(
            SpreadPosition.RIGHT,
            SpreadPosition.LEFT,
            SpreadPosition.RIGHT,
        )
    }

    @Test
    fun `explicit single tag wins and directional tags are not lone`() {
        WebGpuSpreadRules.standsAlone(ReaderPage.SpreadTag.SINGLE, size(80, 100)) shouldBe true
        WebGpuSpreadRules.standsAlone(ReaderPage.SpreadTag.LEFT, size(140, 100)) shouldBe false
        WebGpuSpreadRules.standsAlone(ReaderPage.SpreadTag.RIGHT, size(140, 100)) shouldBe false
        WebGpuSpreadRules.standsAlone(ReaderPage.SpreadTag.NONE, size(140, 100)) shouldBe true
        WebGpuSpreadRules.standsAlone(ReaderPage.SpreadTag.UNKNOWN, size(80, 100)) shouldBe false
    }

    @Test
    fun `pair tolerance is independent of the wide threshold`() {
        WebGpuSpreadRules.canPairShapes(page(1, size(100, 100)), page(2, size(105, 100))) shouldBe true
        WebGpuSpreadRules.canPairShapes(page(1, size(100, 100)), page(2, size(111, 100))) shouldBe false
        WebGpuSpreadRules.canPairShapes(page(1, size(130, 100)), page(2, size(160, 100))) shouldBe false
        WebGpuSpreadRules.canPairShapes(page(1, size(130, 100)), page(2, size(135, 100))) shouldBe true
    }

    @Test
    fun `explicit directional tags pair despite shape differences`() {
        WebGpuSpreadRules.canPairShapes(
            page(1, size(100, 100), ReaderPage.SpreadTag.LEFT),
            page(2, size(160, 100), ReaderPage.SpreadTag.RIGHT),
        ) shouldBe true
    }

    @Test
    fun `an unknown shape is not the same as a known portrait`() {
        WebGpuSpreadRules.canPairShapes(page(1), page(2, size(111, 100))) shouldBe true
        WebGpuSpreadRules.canPairShapes(
            page(1, size(100, 100)),
            page(2, size(111, 100)),
        ) shouldBe false
    }

    @Test
    fun `directional tags keep wide source pages paired`() {
        layout(
            listOf(
                page(0, portrait),
                page(1, wide, ReaderPage.SpreadTag.LEFT),
                page(2, wide, ReaderPage.SpreadTag.RIGHT),
                page(3, portrait),
                page(4, portrait),
            ),
        ) shouldBe listOf(
            SpreadPosition.RIGHT,
            SpreadPosition.LEFT,
            SpreadPosition.RIGHT,
            SpreadPosition.LEFT,
            SpreadPosition.RIGHT,
        )
    }

    @Test
    fun `positions restart after a lone page`() {
        WebGpuSpreadRules.startIndex(null) shouldBe 1
        WebGpuSpreadRules.startIndex(4) shouldBe 5

        WebGpuSpreadRules.positionFor(0, 1, SpreadPosition.LEFT, SpreadPosition.RIGHT) shouldBe
            SpreadPosition.RIGHT
        WebGpuSpreadRules.positionFor(1, 1, SpreadPosition.LEFT, SpreadPosition.RIGHT) shouldBe
            SpreadPosition.LEFT
        WebGpuSpreadRules.positionFor(2, 1, SpreadPosition.LEFT, SpreadPosition.RIGHT) shouldBe
            SpreadPosition.RIGHT
        WebGpuSpreadRules.positionFor(5, 5, SpreadPosition.LEFT, SpreadPosition.RIGHT) shouldBe
            SpreadPosition.LEFT
    }

    @Test
    fun `a derived side without a partner becomes single`() {
        WebGpuSpreadRules.effectivePosition(SpreadPosition.SINGLE, hasPartner = false) shouldBe
            SpreadPosition.SINGLE
        WebGpuSpreadRules.effectivePosition(SpreadPosition.LEFT, hasPartner = false) shouldBe
            SpreadPosition.SINGLE
        WebGpuSpreadRules.effectivePosition(SpreadPosition.RIGHT, hasPartner = false) shouldBe
            SpreadPosition.SINGLE
        WebGpuSpreadRules.effectivePosition(SpreadPosition.LEFT, hasPartner = true) shouldBe
            SpreadPosition.LEFT
        WebGpuSpreadRules.effectivePosition(SpreadPosition.RIGHT, hasPartner = true) shouldBe
            SpreadPosition.RIGHT
    }

    @Test
    fun `a wide page separates the page before it and restarts pairing after it`() {
        val pages = listOf(
            page(0, portrait),
            page(1, portrait),
            page(2, portrait),
            page(3, portrait),
            page(4, wide),
            page(5, portrait),
            page(6, portrait),
        )

        layout(pages) shouldBe listOf(
            SpreadPosition.RIGHT,
            SpreadPosition.LEFT,
            SpreadPosition.RIGHT,
            SpreadPosition.SINGLE,
            SpreadPosition.SINGLE,
            SpreadPosition.LEFT,
            SpreadPosition.RIGHT,
        )
    }

    @Test
    fun `wide pages back to back are each single`() {
        val pages = listOf(
            page(0, portrait),
            page(1, portrait),
            page(2, wide),
            page(3, wide),
            page(4, portrait),
            page(5, portrait),
        )

        layout(pages) shouldBe listOf(
            SpreadPosition.RIGHT,
            SpreadPosition.SINGLE,
            SpreadPosition.SINGLE,
            SpreadPosition.SINGLE,
            SpreadPosition.LEFT,
            SpreadPosition.RIGHT,
        )
    }

    @Test
    fun `an odd trailing page is single`() {
        layout(
            listOf(
                page(0, portrait),
                page(1, portrait),
                page(2, portrait),
                page(3, portrait),
            ),
        ) shouldBe listOf(
            SpreadPosition.RIGHT,
            SpreadPosition.LEFT,
            SpreadPosition.RIGHT,
            SpreadPosition.SINGLE,
        )
    }

    @Test
    fun `reading direction swaps sides without changing groups`() {
        val pages = listOf(
            page(0, portrait),
            page(1, portrait),
            page(2, portrait),
            page(3, wide),
            page(4, portrait),
            page(5, portrait),
        )

        layout(pages, reversed = true) shouldBe listOf(
            SpreadPosition.LEFT,
            SpreadPosition.RIGHT,
            SpreadPosition.LEFT,
            SpreadPosition.SINGLE,
            SpreadPosition.RIGHT,
            SpreadPosition.LEFT,
        )
    }

    private fun layout(
        pages: List<ReaderPage>,
        reversed: Boolean = false,
    ): List<SpreadPosition> {
        val anchor = if (reversed) SpreadPosition.RIGHT else SpreadPosition.LEFT
        val partner = if (reversed) SpreadPosition.LEFT else SpreadPosition.RIGHT
        val lone = pages
            .filter { WebGpuSpreadRules.standsAlone(it.spreadTag, it.sourceSize) }
            .map { it.index }
            .toSortedSet()

        val raw = pages.map { page ->
            WebGpuSpreadRules.taggedPosition(page.spreadTag)
                ?: if (page.index in lone) {
                    SpreadPosition.SINGLE
                } else {
                    val loneBefore = lone.headSet(page.index).lastOrNull()
                    WebGpuSpreadRules.positionFor(
                        page.index,
                        WebGpuSpreadRules.startIndex(loneBefore),
                        anchor,
                        partner,
                    )
                }
        }

        return pages.indices.map { i ->
            val position = raw[i]
            if (pages[i].index == 0) return@map position
            val hasPartner = when (position) {
                anchor -> raw.getOrNull(i + 1) == partner &&
                    WebGpuSpreadRules.canPairShapes(pages[i], pages[i + 1])

                partner -> i > 0 && raw[i - 1] == anchor &&
                    WebGpuSpreadRules.canPairShapes(pages[i - 1], pages[i])

                SpreadPosition.SINGLE -> false
            }
            WebGpuSpreadRules.effectivePosition(position, hasPartner)
        }
    }

    private fun size(width: Int, height: Int) = ReaderPage.SourceSize(width, height)

    private val portrait get() = size(100, 150)

    private val wide get() = size(160, 100)

    private fun page(
        index: Int,
        size: ReaderPage.SourceSize? = null,
        tag: ReaderPage.SpreadTag = ReaderPage.SpreadTag.UNKNOWN,
    ): ReaderPage = ReaderPage(index).apply {
        size?.let { noteSourceSize(it.width, it.height) }
        noteSpreadTag(tag)
    }
}
