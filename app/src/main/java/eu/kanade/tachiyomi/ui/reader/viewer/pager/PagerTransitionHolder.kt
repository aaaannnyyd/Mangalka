package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderTransitionView
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

/**
 * View of the ViewPager that contains a chapter transition.
 */
@SuppressLint("ViewConstructor")
class PagerTransitionHolder(
    readerThemedContext: Context,
    val viewer: PagerViewer,
    val transition: ChapterTransition,
) : LinearLayout(readerThemedContext), ViewPagerAdapter.PositionableView {

    private val scope = MainScope()
    private var stateJob: Job? = null
    private var jumpToChapterWhenLoaded = false
    private val transitionView = ReaderTransitionView(context)

    override val item: Any
        get() = transition

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val sidePadding = 40.dpToPx
        setPadding(sidePadding, 0, sidePadding, 0)
        addView(transitionView, LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val onChapterComments = if (
            transition is ChapterTransition.Next &&
            viewer.readerPreferences.showChapterComments.get() &&
            viewer.activity.viewModel.supportsChapterComments()
        ) {
            { viewer.activity.openChapterComments(transition.from) }
        } else {
            null
        }
        val onNextChapter = (transition as? ChapterTransition.Next)?.to?.let { nextChapter ->
            {
                val firstPage = nextChapter.pages?.firstOrNull()
                if (firstPage != null) {
                    viewer.moveToPage(firstPage)
                } else {
                    jumpToChapterWhenLoaded = true
                    viewer.activity.requestPreloadChapter(nextChapter)
                }
            }
        }

        transitionView.bind(
            transition = transition,
            downloadManager = viewer.downloadManager,
            manga = viewer.activity.viewModel.manga,
            onNextChapter = onNextChapter,
            onChapterComments = onChapterComments,
        )

        transition.to?.let(::observeStatus)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stateJob?.cancel()
        jumpToChapterWhenLoaded = false
    }

    private fun observeStatus(chapter: ReaderChapter) {
        stateJob?.cancel()
        stateJob = scope.launch {
            chapter.stateFlow.collectLatest { state ->
                when (state) {
                    is ReaderChapter.State.Loading -> transitionView.showLoading()
                    is ReaderChapter.State.Error -> {
                        val message = context.stringResource(
                            MR.strings.transition_pages_error,
                            state.error.message ?: "",
                        )
                        transitionView.showError(message) {
                            transition.to?.let(viewer.activity::requestPreloadChapter)
                        }
                    }
                    is ReaderChapter.State.Wait -> transitionView.clearStatus()
                    is ReaderChapter.State.Loaded -> {
                        transitionView.clearStatus()
                        if (jumpToChapterWhenLoaded) {
                            jumpToChapterWhenLoaded = false
                            chapter.pages?.firstOrNull()?.let(viewer::moveToPage)
                        }
                    }
                }
            }
        }
    }
}
