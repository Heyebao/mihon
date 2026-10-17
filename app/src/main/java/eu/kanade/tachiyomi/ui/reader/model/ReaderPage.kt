package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.source.model.Page
import java.io.InputStream

open class ReaderPage(
    index: Int,
    url: String = "",
    imageUrl: String? = null,
    var stream: (() -> InputStream)? = null,
) : Page(index, url, imageUrl, null) {

    open lateinit var chapter: ReaderChapter

    /** Set by a viewer before the download; filled during [Page.State.DownloadImage]. */
    @Volatile
    var downloadStream: DownloadStream? = null

    /** Orientation-corrected source canvas, recorded from the decoder header. */
    @Volatile
    var sourceSize: SourceSize? = null

    /** The file's PageName tag, or [SpreadTag.UNKNOWN] until it has been read. */
    @Volatile
    var spreadTag: SpreadTag = SpreadTag.UNKNOWN

    /** Records [width]x[height]; returns true when the known canvas changed. */
    fun noteSourceSize(width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0) return false
        val size = SourceSize(width, height)
        if (sourceSize == size) return false
        sourceSize = size
        return true
    }

    /** Records a read tag; returns true when the known tag changed. */
    fun noteSpreadTag(tag: SpreadTag): Boolean {
        if (tag == SpreadTag.UNKNOWN || spreadTag == tag) return false
        spreadTag = tag
        return true
    }

    data class SourceSize(val width: Int, val height: Int) {
        val aspectRatio: Float
            get() = width.toFloat() / height
    }

    enum class SpreadTag {
        UNKNOWN,
        NONE,
        LEFT,
        RIGHT,
        SINGLE,
    }
}
