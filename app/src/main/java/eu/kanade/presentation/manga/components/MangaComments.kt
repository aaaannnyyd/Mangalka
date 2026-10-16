package eu.kanade.presentation.manga.components

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.presentation.components.AdaptiveSheet
import mangalka.comments.model.CommentFlag
import mangalka.comments.model.CommentNode
import mangalka.comments.model.CommentPage
import mangalka.comments.model.CommentSort
import mangalka.comments.model.UnifiedComment
import mangalka.comments.util.countForPage
import mangalka.comments.util.filterForPage
import mangalka.comments.util.flattenForDisplay
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassChip
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkLoadingState
import tachiyomi.presentation.core.components.InkTypography

sealed interface CommentsUiState {
    data object Idle : CommentsUiState
    data object Loading : CommentsUiState
    data class Loaded(
        val page: CommentPage,
        val providerName: String? = null,
        val sort: CommentSort = CommentSort.NEWEST,
        val loadingMore: Boolean = false,
    ) : CommentsUiState
    data class Error(
        val message: String? = null,
        val technicalDetail: String? = null,
    ) : CommentsUiState
}

/** Sources without any comments get no comment card on manga details at all. */
fun CommentsUiState.isLoadedEmpty(): Boolean =
    this is CommentsUiState.Loaded && page.comments.isEmpty() && !page.hasNextPage

@Composable
fun MangaCommentsSection(
    state: CommentsUiState,
    onRefresh: () -> Unit,
    onShowAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier.fillMaxWidth(),
        level = InkGlassLevel.Strong,
        shape = RoundedCornerShape(InkGlassTokens.RadiusCard),
        contentPadding = PaddingValues(InkGlassTokens.Space20),
        shadowElevation = 0.dp,
        prominent = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16)) {
            val loaded = state as? CommentsUiState.Loaded
            CommentsHeader(
                count = loaded?.page?.let(::commentCount),
                providerName = loaded?.providerName,
                onRefresh = onRefresh,
            )

            when (state) {
                CommentsUiState.Idle -> InkAccentButton(
                    label = "Загрузить комментарии",
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth(),
                )
                CommentsUiState.Loading -> InkLoadingState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = InkGlassTokens.Space24),
                    label = "Загружаем комментарии",
                )
                is CommentsUiState.Error -> CommentError(state.message, state.technicalDetail, onRefresh)
                is CommentsUiState.Loaded -> CommentPreview(state, onShowAll)
            }
        }
    }
}

@Composable
private fun CommentPreview(
    state: CommentsUiState.Loaded,
    onShowAll: (() -> Unit)?,
) {
    val comments = remember(state.page.comments) { state.page.comments.flattenForDisplay() }
    if (comments.isEmpty()) {
        Text(
            text = "Комментариев пока нет.",
            style = InkTypography.Body,
            color = InkGlassTokens.TextSecondary,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
        comments.take(PREVIEW_LIMIT).forEach { comment ->
            // Long posts and images would push the chapter list far down; the full text is one tap away.
            Box(Modifier.heightIn(max = PREVIEW_COMMENT_MAX_HEIGHT).clipToBounds()) {
                CommentRow(
                    comment = comment,
                    isCollapsed = false,
                    onToggleCollapse = null,
                    compact = true,
                )
            }
        }
    }

    if (onShowAll != null && (comments.size > PREVIEW_LIMIT || state.page.hasNextPage)) {
        Spacer(Modifier.height(InkGlassTokens.Space4))
        InkAccentButton(
            label = "Открыть обсуждение (${commentCount(state.page)})",
            onClick = onShowAll,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun MangaCommentsSheet(
    title: String,
    state: CommentsUiState,
    onRefresh: () -> Unit,
    onDismissRequest: () -> Unit,
    onLoadMore: (() -> Unit)? = null,
    onSortChanged: ((CommentSort) -> Unit)? = null,
) {
    val loaded = state as? CommentsUiState.Loaded
    val sort = loaded?.sort ?: CommentSort.NEWEST

    AdaptiveSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxHeight(0.92f),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = InkGlassTokens.Space8, bottom = InkGlassTokens.Space12),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(InkGlassTokens.GlassBorder),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = InkGlassTokens.Space20),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("КОММЕНТАРИИ ГЛАВЫ", style = InkTypography.Eyebrow, color = InkGlassTokens.Accent)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                    ) {
                        Text(
                            text = title,
                            style = InkTypography.Title,
                            color = InkGlassTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        loaded?.page?.let { page ->
                            Text(
                                text = commentCount(page).toString(),
                                style = InkTypography.Label,
                                color = InkGlassTokens.TextSecondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(InkGlassTokens.RadiusChip))
                                    .background(InkGlassTokens.GlassSoft)
                                    .padding(horizontal = InkGlassTokens.Space8, vertical = InkGlassTokens.Space4),
                            )
                        }
                    }
                    loaded?.providerName?.let { provider ->
                        Text(
                            text = "Источник · $provider",
                            style = InkTypography.BodySmall,
                            color = InkGlassTokens.TextTertiary,
                        )
                    }
                }
                InkGlassIconButton(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Обновить комментарии",
                    onClick = onRefresh,
                )
                InkGlassIconButton(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Закрыть",
                    onClick = onDismissRequest,
                )
            }

            if (loaded != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = InkGlassTokens.Space20,
                            end = InkGlassTokens.Space20,
                            top = InkGlassTokens.Space16,
                            bottom = InkGlassTokens.Space8,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                ) {
                    InkGlassChip(
                        label = "Популярные",
                        selected = sort == CommentSort.TOP,
                        onClick = { onSortChanged?.invoke(CommentSort.TOP) },
                    )
                    InkGlassChip(
                        label = "Новые",
                        selected = sort == CommentSort.NEWEST,
                        onClick = { onSortChanged?.invoke(CommentSort.NEWEST) },
                    )
                }
            }

            when (state) {
                CommentsUiState.Idle -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(InkGlassTokens.Space24),
                    contentAlignment = Alignment.Center,
                ) {
                    InkAccentButton("Загрузить комментарии", onRefresh)
                }
                CommentsUiState.Loading -> InkLoadingState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(InkGlassTokens.Space32),
                    label = "Загружаем комментарии",
                )
                is CommentsUiState.Error -> Box(Modifier.padding(InkGlassTokens.Space20)) {
                    CommentError(state.message, state.technicalDetail, onRefresh)
                }
                is CommentsUiState.Loaded -> CommentTreeList(
                    page = state.page,
                    sort = state.sort,
                    loadingMore = state.loadingMore,
                    onLoadMore = onLoadMore,
                )
            }
        }
    }
}

@Composable
fun ReaderCommentsOverlay(
    state: CommentsUiState,
    visible: Boolean,
    currentPage: Int,
    pageFilter: Int?,
    pageCommentsSupported: Boolean,
    onToggle: () -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onShowWholeChapter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loaded = state as? CommentsUiState.Loaded
    val exactPageCount = if (
        pageCommentsSupported && currentPage > 0 && loaded != null && !loaded.page.hasNextPage
    ) {
        loaded.page.comments.countForPage(currentPage)
    } else {
        null
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!visible) {
            InkGlassSurface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(InkGlassTokens.Space20),
                level = InkGlassLevel.Strong,
                shape = RoundedCornerShape(InkGlassTokens.RadiusChip),
                contentPadding = PaddingValues(
                    horizontal = InkGlassTokens.Space16,
                    vertical = InkGlassTokens.Space12,
                ),
                shadowElevation = 0.dp,
                prominent = true,
                onClick = onToggle,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = InkGlassTokens.Accent,
                    )
                    Text(
                        text = exactPageCount?.let { "💬 $it" } ?: "💬",
                        style = InkTypography.Label,
                        color = InkGlassTokens.TextPrimary,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            InkGlassSurface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.62f)
                    .padding(InkGlassTokens.Space12),
                level = InkGlassLevel.Strong,
                shape = RoundedCornerShape(InkGlassTokens.RadiusCard),
                contentPadding = PaddingValues(InkGlassTokens.Space12),
                shadowElevation = 0.dp,
                prominent = true,
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = pageFilter?.let { "КОММЕНТАРИИ · СТР. $it" } ?: "КОММЕНТАРИИ ГЛАВЫ",
                                style = InkTypography.Eyebrow,
                                color = InkGlassTokens.Accent,
                            )
                            loaded?.providerName?.let {
                                Text(
                                    text = "Источник · $it",
                                    style = InkTypography.BodySmall,
                                    color = InkGlassTokens.TextTertiary,
                                )
                            }
                        }
                        if (pageCommentsSupported && loaded != null) {
                            InkGlassChip(
                                label = pageFilter?.let { "Вся глава" } ?: "Стр. $currentPage",
                                selected = pageFilter != null,
                                onClick = onShowWholeChapter,
                            )
                        }
                        InkGlassIconButton(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Обновить комментарии",
                            onClick = onRefresh,
                        )
                        InkGlassIconButton(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Закрыть комментарии",
                            onClick = onClose,
                        )
                    }

                    when (state) {
                        CommentsUiState.Idle -> Box(
                            Modifier
                                .fillMaxSize()
                                .padding(InkGlassTokens.Space20),
                            contentAlignment = Alignment.Center,
                        ) {
                            InkAccentButton("Загрузить комментарии", onRefresh)
                        }
                        CommentsUiState.Loading -> InkLoadingState(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(InkGlassTokens.Space24),
                            label = "Загружаем комментарии",
                        )
                        is CommentsUiState.Error -> Box(Modifier.padding(InkGlassTokens.Space16)) {
                            CommentError(state.message, state.technicalDetail, onRefresh)
                        }
                        is CommentsUiState.Loaded -> CommentTreeList(
                            page = state.page,
                            sort = state.sort,
                            loadingMore = state.loadingMore,
                            onLoadMore = onLoadMore,
                            pageFilter = pageFilter,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentTreeList(
    page: CommentPage,
    sort: CommentSort,
    loadingMore: Boolean,
    onLoadMore: (() -> Unit)?,
    pageFilter: Int? = null,
) {
    val filteredRoots = remember(page.comments, pageFilter) {
        pageFilter?.let(page.comments::filterForPage) ?: page.comments
    }
    if (filteredRoots.isEmpty()) {
        Text(
            text = "Комментариев пока нет.",
            modifier = Modifier.padding(InkGlassTokens.Space20),
            style = InkTypography.Body,
            color = InkGlassTokens.TextSecondary,
        )
        return
    }

    var collapsed by rememberSaveable(page.providerId) { mutableStateOf(emptySet<String>()) }
    val sortedRoots = remember(filteredRoots, sort) {
        when (sort) {
            CommentSort.TOP -> filteredRoots.sortedByDescending {
                it.votes.effectiveScore ?: it.votes.up ?: 0
            }
            CommentSort.NEWEST -> filteredRoots.sortedByDescending { it.createdAt ?: 0L }
            CommentSort.OLDEST -> filteredRoots.sortedBy { it.createdAt ?: Long.MAX_VALUE }
        }
    }
    val visible = remember(sortedRoots, collapsed) { flattenVisible(sortedRoots, collapsed) }

    LazyColumn(
        contentPadding = PaddingValues(InkGlassTokens.Space20),
        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
    ) {
        items(visible, key = { it.id }) { comment ->
            CommentRow(
                comment = comment,
                isCollapsed = comment.id in collapsed,
                onToggleCollapse = if (comment.children.isNotEmpty()) {
                    {
                        collapsed = if (comment.id in collapsed) {
                            collapsed - comment.id
                        } else {
                            collapsed + comment.id
                        }
                    }
                } else {
                    null
                },
            )
        }
        if (page.hasNextPage && onLoadMore != null) {
            item(key = "comments_load_more") {
                InkGlassButton(
                    label = if (loadingMore) "Загружаем…" else "Загрузить ещё",
                    onClick = onLoadMore,
                    enabled = !loadingMore,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: UnifiedComment,
    isCollapsed: Boolean,
    onToggleCollapse: (() -> Unit)?,
    compact: Boolean = false,
) {
    val indent = if (compact) 0.dp else (comment.depth.coerceAtMost(MAX_INDENT_DEPTH) * 12).dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent),
    ) {
        if (!compact && comment.depth > 0) {
            Box(
                Modifier
                    .width(2.dp)
                    .heightIn(min = 54.dp)
                    .background(InkGlassTokens.GlassBorder, RoundedCornerShape(1.dp)),
            )
            Spacer(Modifier.width(InkGlassTokens.Space8))
        }

        InkGlassSurface(
            modifier = Modifier.weight(1f),
            level = InkGlassLevel.Soft,
            shape = RoundedCornerShape(InkGlassTokens.RadiusSmall),
            contentPadding = PaddingValues(InkGlassTokens.Space12),
            shadowElevation = 0.dp,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
                ) {
                    CommentAvatar(comment)
                    Column(Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4),
                        ) {
                            Text(
                                text = comment.author.name.ifBlank { "Аноним" },
                                style = InkTypography.TitleSmall,
                                color = InkGlassTokens.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            comment.author.badge?.let { badge ->
                                Text(
                                    text = badge,
                                    style = InkTypography.Eyebrow,
                                    color = InkGlassTokens.Accent,
                                )
                            }
                            if (CommentFlag.PINNED in comment.flags) {
                                Text("PIN", style = InkTypography.Eyebrow, color = InkGlassTokens.Warning)
                            }
                        }
                        val metadata = buildList {
                            comment.createdAt?.let { add(relativeTime(it)) }
                            comment.pageNumber?.let { add("стр. $it") }
                            if (CommentFlag.EDITED in comment.flags) add("изменён")
                        }.joinToString(" · ")
                        if (metadata.isNotBlank()) {
                            Text(
                                text = metadata,
                                style = InkTypography.Eyebrow,
                                color = InkGlassTokens.TextTertiary,
                            )
                        }
                    }
                }

                CommentBody(comment.body, comment.id)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
                ) {
                    comment.votes.up?.let { up ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ThumbUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = InkGlassTokens.TextTertiary,
                            )
                            Text(up.toString(), style = InkTypography.BodySmall, color = InkGlassTokens.TextSecondary)
                        }
                    }
                    comment.votes.down?.let { down ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ThumbDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = InkGlassTokens.TextTertiary,
                            )
                            Text(down.toString(), style = InkTypography.BodySmall, color = InkGlassTokens.TextSecondary)
                        }
                    }
                    if (comment.votes.up == null && comment.votes.down == null) {
                        comment.votes.effectiveScore?.let { score ->
                            Text(
                                "Рейтинг $score",
                                style = InkTypography.BodySmall,
                                color = InkGlassTokens.TextSecondary,
                            )
                        }
                    }
                    if (onToggleCollapse != null) {
                        Text(
                            text = if (isCollapsed) {
                                "Ответы (${comment.descendantCount})"
                            } else {
                                "Свернуть ветку"
                            },
                            style = InkTypography.Label,
                            color = InkGlassTokens.Accent,
                            modifier = Modifier.clickable(onClick = onToggleCollapse),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentAvatar(comment: UnifiedComment) {
    val avatar = comment.author.avatarUrl
    if (avatar != null) {
        AsyncImage(
            model = avatar,
            contentDescription = null,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(InkGlassTokens.AccentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = comment.author.name.firstOrNull()?.uppercase() ?: "?",
                style = InkTypography.Label,
                color = InkGlassTokens.Accent,
            )
        }
    }
}

@Composable
private fun CommentBody(
    nodes: List<CommentNode>,
    commentId: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space4)) {
        val inline = mutableListOf<CommentNode>()

        @Composable
        fun FlushInline() {
            if (inline.isEmpty()) return
            val text = buildAnnotatedString { inline.forEach { appendInlineNode(it) } }
            if (text.isNotEmpty()) {
                Text(text = text, style = InkTypography.Body, color = InkGlassTokens.TextSecondary)
            }
            inline.clear()
        }

        nodes.forEachIndexed { index, node ->
            when (node) {
                is CommentNode.Spoiler -> {
                    FlushInline()
                    SpoilerBlock(node, "$commentId-$index")
                }
                is CommentNode.Quote -> {
                    FlushInline()
                    QuoteBlock(node, "$commentId-$index")
                }
                is CommentNode.Image -> {
                    FlushInline()
                    AsyncImage(
                        model = node.url,
                        contentDescription = node.alt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(InkGlassTokens.RadiusChip)),
                    )
                }
                is CommentNode.Paragraph -> {
                    FlushInline()
                    CommentBody(node.children, "$commentId-$index")
                }
                else -> inline += node
            }
        }
        FlushInline()
    }
}

@Composable
private fun SpoilerBlock(node: CommentNode.Spoiler, key: String) {
    var revealed by rememberSaveable(key) { mutableStateOf(false) }
    InkGlassSurface(
        modifier = Modifier.fillMaxWidth(),
        level = InkGlassLevel.Accent,
        shape = RoundedCornerShape(InkGlassTokens.RadiusChip),
        contentPadding = PaddingValues(InkGlassTokens.Space12),
        shadowElevation = 0.dp,
        onClick = { revealed = !revealed },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8)) {
            Text(
                text = node.title?.takeIf { it.isNotBlank() } ?: "Спойлер",
                style = InkTypography.Label,
                color = InkGlassTokens.Accent,
            )
            AnimatedVisibility(revealed) {
                CommentBody(node.children, "$key-body")
            }
            if (!revealed) {
                Text("Нажмите, чтобы показать", style = InkTypography.BodySmall, color = InkGlassTokens.TextTertiary)
            }
        }
    }
}

@Composable
private fun QuoteBlock(node: CommentNode.Quote, commentId: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InkGlassTokens.GlassSoft, RoundedCornerShape(InkGlassTokens.RadiusChip))
            .padding(InkGlassTokens.Space12),
    ) {
        Box(
            Modifier
                .width(2.dp)
                .heightIn(min = 28.dp)
                .background(InkGlassTokens.AccentBorder, RoundedCornerShape(1.dp)),
        )
        Spacer(Modifier.width(InkGlassTokens.Space8))
        Column(Modifier.weight(1f)) {
            node.author?.let {
                Text(it, style = InkTypography.Eyebrow, color = InkGlassTokens.TextTertiary)
            }
            CommentBody(node.children, commentId)
        }
    }
}

private fun AnnotatedString.Builder.appendInlineNode(node: CommentNode) {
    when (node) {
        is CommentNode.Text -> append(node.text)
        CommentNode.LineBreak -> append('\n')
        is CommentNode.Mention -> withStyle(SpanStyle(color = InkGlassTokens.Accent)) {
            append('@')
            append(node.name)
        }
        is CommentNode.Link -> withStyle(
            SpanStyle(
                color = InkGlassTokens.Accent,
                textDecoration = TextDecoration.Underline,
            ),
        ) {
            node.children.forEach { appendInlineNode(it) }
        }
        is CommentNode.Styled -> withStyle(
            SpanStyle(
                fontWeight = if (node.bold) FontWeight.Bold else null,
                fontStyle = if (node.italic) FontStyle.Italic else null,
                fontFamily = if (node.code) FontFamily.Monospace else null,
                textDecoration = when {
                    node.strikethrough && node.underline -> TextDecoration.combine(
                        listOf(TextDecoration.LineThrough, TextDecoration.Underline),
                    )
                    node.strikethrough -> TextDecoration.LineThrough
                    node.underline -> TextDecoration.Underline
                    else -> null
                },
            ),
        ) {
            node.children.forEach { appendInlineNode(it) }
        }
        is CommentNode.Paragraph -> node.children.forEach { appendInlineNode(it) }
        is CommentNode.Quote -> node.children.forEach { appendInlineNode(it) }
        is CommentNode.Spoiler -> Unit
        is CommentNode.Image -> node.alt?.let(::append)
    }
}

@Composable
private fun CommentsHeader(
    count: Int?,
    providerName: String?,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
    ) {
        Icon(
            imageVector = Icons.Outlined.Forum,
            contentDescription = null,
            tint = InkGlassTokens.Accent,
        )
        Column(Modifier.weight(1f)) {
            Text("КОММЕНТАРИИ", style = InkTypography.Eyebrow, color = InkGlassTokens.Accent)
            Text(
                text = count?.let { "$it сообщений" } ?: "Обсуждение тайтла",
                style = InkTypography.TitleSmall,
                color = InkGlassTokens.TextPrimary,
            )
            providerName?.let {
                Text("Источник · $it", style = InkTypography.BodySmall, color = InkGlassTokens.TextTertiary)
            }
        }
        InkGlassIconButton(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = "Обновить комментарии",
            onClick = onRefresh,
        )
    }
}

@Composable
private fun CommentError(message: String?, technicalDetail: String?, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12)) {
        Text(
            text = "Не удалось загрузить комментарии",
            style = InkTypography.TitleSmall,
            color = InkGlassTokens.TextPrimary,
        )
        message?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = InkTypography.BodySmall, color = InkGlassTokens.TextSecondary)
        }
        technicalDetail?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = InkTypography.Eyebrow, color = InkGlassTokens.TextTertiary)
        }
        InkAccentButton("Повторить", onRetry)
    }
}

private fun flattenVisible(
    roots: List<UnifiedComment>,
    collapsed: Set<String>,
): List<UnifiedComment> = buildList {
    fun walk(nodes: List<UnifiedComment>) {
        nodes.forEach { comment ->
            add(comment)
            if (comment.id !in collapsed) walk(comment.children)
        }
    }
    walk(roots)
}

private fun commentCount(page: CommentPage): Int = page.totalCount ?: page.comments.flattenForDisplay().size

private fun relativeTime(timestamp: Long): String = DateUtils.getRelativeTimeSpanString(
    timestamp,
    System.currentTimeMillis(),
    DateUtils.MINUTE_IN_MILLIS,
    DateUtils.FORMAT_ABBREV_RELATIVE,
).toString()

private const val PREVIEW_LIMIT = 2
private val PREVIEW_COMMENT_MAX_HEIGHT = 132.dp
private const val MAX_INDENT_DEPTH = 5
