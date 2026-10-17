package eu.kanade.tachiyomi.ui.reader.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderPageTypeTest {

    @Test
    fun `unread and read-without-tag are distinct`() {
        val page = ReaderPage(0)
        page.spreadTag shouldBe ReaderPage.SpreadTag.UNKNOWN

        page.noteSpreadTag(ReaderPage.SpreadTag.NONE) shouldBe true
        page.spreadTag shouldBe ReaderPage.SpreadTag.NONE
        page.noteSpreadTag(ReaderPage.SpreadTag.NONE) shouldBe false
    }

    @Test
    fun `source size is stored idempotently`() {
        val page = ReaderPage(0)
        page.noteSourceSize(2400, 3600) shouldBe true
        page.sourceSize shouldBe ReaderPage.SourceSize(2400, 3600)
        page.sourceSize?.aspectRatio shouldBe 2400f / 3600f

        page.noteSourceSize(2400, 3600) shouldBe false
        page.noteSourceSize(0, 3600) shouldBe false
        page.sourceSize shouldBe ReaderPage.SourceSize(2400, 3600)
    }
}
