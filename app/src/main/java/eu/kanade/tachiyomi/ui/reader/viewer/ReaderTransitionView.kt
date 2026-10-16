package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.reader.ChapterTransition
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkCircularProgress
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.isLocal

class ReaderTransitionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AbstractComposeView(context, attrs) {

    private var data: Data? by mutableStateOf(null)
    private var status: TransitionStatus by mutableStateOf(TransitionStatus.Idle)

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    fun bind(
        transition: ChapterTransition,
        downloadManager: DownloadManager,
        manga: Manga?,
        onNextChapter: (() -> Unit)? = null,
        onChapterComments: (() -> Unit)? = null,
    ) {
        status = TransitionStatus.Idle
        data = if (manga != null) {
            Data(
                transition = transition,
                currChapterDownloaded = transition.from.pageLoader?.isLocal == true,
                goingToChapterDownloaded = manga.isLocal() ||
                    transition.to?.chapter?.let { goingToChapter ->
                        downloadManager.isChapterDownloaded(
                            chapterName = goingToChapter.name,
                            chapterScanlator = goingToChapter.scanlator,
                            chapterUrl = goingToChapter.url,
                            mangaTitle = manga.title,
                            sourceId = manga.source,
                            skipCache = true,
                        )
                    } ?: false,
                onNextChapter = onNextChapter,
                onChapterComments = onChapterComments,
            )
        } else {
            null
        }
    }

    fun showLoading() {
        status = TransitionStatus.Loading
    }

    fun showError(message: String, onRetry: () -> Unit) {
        status = TransitionStatus.Error(message, onRetry)
    }

    fun clearStatus() {
        status = TransitionStatus.Idle
    }

    @Composable
    override fun Content() {
        data?.let { current ->
            TachiyomiTheme {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodySmall,
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
                    ) {
                        ChapterTransition(
                            transition = current.transition,
                            currChapterDownloaded = current.currChapterDownloaded,
                            goingToChapterDownloaded = current.goingToChapterDownloaded,
                            onNextChapter = current.onNextChapter,
                            onChapterComments = current.onChapterComments,
                        )
                        TransitionStatusContent(status)
                    }
                }
            }
        }
    }

    @Composable
    private fun TransitionStatusContent(status: TransitionStatus) {
        when (status) {
            TransitionStatus.Idle -> Unit
            TransitionStatus.Loading -> {
                InkGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    level = InkGlassLevel.Soft,
                    shape = RoundedCornerShape(InkGlassTokens.RadiusMedium),
                    contentPadding = PaddingValues(InkGlassTokens.Space16),
                    shadowElevation = 0.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12)) {
                        InkCircularProgress()
                        Text(
                            text = stringResource(MR.strings.transition_pages_loading),
                            style = InkTypography.BodySmall,
                            color = InkGlassTokens.TextSecondary,
                        )
                    }
                }
            }
            is TransitionStatus.Error -> {
                InkGlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    level = InkGlassLevel.Accent,
                    shape = RoundedCornerShape(InkGlassTokens.RadiusMedium),
                    contentPadding = PaddingValues(InkGlassTokens.Space16),
                    shadowElevation = 0.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12)) {
                        Text(
                            text = status.message,
                            style = InkTypography.BodySmall,
                            color = InkGlassTokens.TextSecondary,
                        )
                        InkGlassButton(
                            label = stringResource(MR.strings.action_retry),
                            onClick = status.onRetry,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    private data class Data(
        val transition: ChapterTransition,
        val currChapterDownloaded: Boolean,
        val goingToChapterDownloaded: Boolean,
        val onNextChapter: (() -> Unit)?,
        val onChapterComments: (() -> Unit)?,
    )

    private sealed interface TransitionStatus {
        data object Idle : TransitionStatus
        data object Loading : TransitionStatus
        data class Error(val message: String, val onRetry: () -> Unit) : TransitionStatus
    }
}
