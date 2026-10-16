package eu.kanade.tachiyomi.ui.download

import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.more.settings.InkSettingsTopBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkEmptyState
import tachiyomi.presentation.core.components.InkGlassDropdownMenu
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkMangaBackdrop
import tachiyomi.presentation.core.components.Pill
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

object DownloadQueueScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val viewModel = metroViewModel<DownloadQueueViewModel>()
        val downloadList by viewModel.state.collectAsStateWithLifecycle()
        val downloadCount by remember {
            derivedStateOf { downloadList.sumOf { it.subItems.size } }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(InkGlassTokens.BackgroundPrimary),
        ) {
            InkMangaBackdrop(intensity = 0.45f)
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    InkSettingsTopBar(
                        title = stringResource(MR.strings.label_download_queue),
                        navigateUp = navigator::pop,
                        actions = {
                            if (downloadCount > 0) {
                                Pill(
                                    text = "$downloadCount",
                                    color = InkGlassTokens.AccentSoft,
                                    fontSize = 13.sp,
                                )
                            }
                            if (downloadList.isNotEmpty()) {
                                var sortExpanded by remember { mutableStateOf(false) }
                                var moreExpanded by remember { mutableStateOf(false) }

                                Box {
                                    InkGlassIconButton(
                                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                                        contentDescription = stringResource(MR.strings.action_sort),
                                        onClick = { sortExpanded = true },
                                    )
                                    InkGlassDropdownMenu(
                                        expanded = sortExpanded,
                                        onDismissRequest = { sortExpanded = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(MR.strings.action_newest)) },
                                            onClick = {
                                                viewModel.reorderQueue({ it.download.chapter.dateUpload }, true)
                                                sortExpanded = false
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(MR.strings.action_oldest)) },
                                            onClick = {
                                                viewModel.reorderQueue({ it.download.chapter.dateUpload }, false)
                                                sortExpanded = false
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(MR.strings.action_asc)) },
                                            onClick = {
                                                viewModel.reorderQueue({ it.download.chapter.chapterNumber }, false)
                                                sortExpanded = false
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(MR.strings.action_desc)) },
                                            onClick = {
                                                viewModel.reorderQueue({ it.download.chapter.chapterNumber }, true)
                                                sortExpanded = false
                                            },
                                        )
                                    }
                                }

                                Box {
                                    InkGlassIconButton(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = "Ещё",
                                        onClick = { moreExpanded = true },
                                    )
                                    InkGlassDropdownMenu(
                                        expanded = moreExpanded,
                                        onDismissRequest = { moreExpanded = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(MR.strings.action_cancel_all)) },
                                            onClick = {
                                                moreExpanded = false
                                                viewModel.clearQueue()
                                            },
                                        )
                                    }
                                }
                            }
                        },
                    )
                },
                floatingActionButton = {
                    if (downloadList.isNotEmpty()) {
                        val isRunning by viewModel.isDownloaderRunning.collectAsStateWithLifecycle()
                        val id = if (isRunning) MR.strings.action_pause else MR.strings.action_resume
                        val icon = if (isRunning) Icons.Outlined.Pause else Icons.Filled.PlayArrow
                        InkAccentButton(
                            label = stringResource(id),
                            icon = icon,
                            onClick = {
                                if (isRunning) viewModel.pauseDownloads() else viewModel.startDownloads()
                            },
                        )
                    }
                },
            ) { contentPadding ->
                if (downloadList.isEmpty()) {
                    InkEmptyState(
                        title = "Очередь пуста",
                        message = "Загруженные главы и активные скачивания появятся здесь.",
                        modifier = Modifier
                            .padding(contentPadding)
                            .padding(InkGlassTokens.Space20),
                    )
                    return@Scaffold
                }

                val density = LocalDensity.current
                val layoutDirection = LocalLayoutDirection.current
                val left = with(density) { contentPadding.calculateLeftPadding(layoutDirection).toPx().roundToInt() }
                val top = with(density) { contentPadding.calculateTopPadding().toPx().roundToInt() }
                val right = with(density) { contentPadding.calculateRightPadding(layoutDirection).toPx().roundToInt() }
                val bottom = with(density) { contentPadding.calculateBottomPadding().toPx().roundToInt() }

                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        viewModel.controllerBinding = DownloadListBinding.inflate(LayoutInflater.from(context))
                        viewModel.adapter = DownloadAdapter(viewModel.listener)
                        viewModel.controllerBinding.root.adapter = viewModel.adapter
                        viewModel.adapter?.isHandleDragEnabled = true
                        viewModel.controllerBinding.root.layoutManager = LinearLayoutManager(context)
                        ViewCompat.setNestedScrollingEnabled(viewModel.controllerBinding.root, true)

                        scope.launchUI {
                            viewModel.getDownloadStatusFlow().collect(viewModel::onStatusChange)
                        }
                        scope.launchUI {
                            viewModel.getDownloadProgressFlow().collect(viewModel::onUpdateDownloadedPages)
                        }
                        viewModel.controllerBinding.root
                    },
                    update = {
                        viewModel.controllerBinding.root.updatePadding(
                            left = left,
                            top = top,
                            right = right,
                            bottom = bottom,
                        )
                        viewModel.adapter?.updateDataSet(downloadList)
                    },
                )
            }
        }
    }
}
