package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderTransitionView
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

/**
 * Holder of the webtoon viewer that contains a chapter transition.
 */
class WebtoonTransitionHolder(
    val layout: LinearLayout,
    viewer: WebtoonViewer,
) : WebtoonBaseHolder(layout, viewer) {

    private val scope = MainScope()
    private var stateJob: Job? = null
    private var jumpToChapterWhenLoaded = false
    private val transitionView = ReaderTransitionView(context)

    init {
        layout.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER

        // Keep the transition inside the reading flow instead of turning it into a full blank screen.
        val paddingVertical = 28.dpToPx
        val paddingHorizontal = 20.dpToPx
        layout.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        layout.addView(transitionView, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    /**
     * Binds the given [transition] with this view holder, subscribing to its state.
     */
    fun bind(transition: ChapterTransition) {
        jumpToChapterWhenLoaded = false
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

        transition.to?.let { observeStatus(it, transition) }
    }

    /**
     * Called when the view is recycled and being added to the view pool.
     */
    override fun recycle() {
        stateJob?.cancel()
        jumpToChapterWhenLoaded = false
        transitionView.clearStatus()
    }

    private fun observeStatus(chapter: ReaderChapter, transition: ChapterTransition) {
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
