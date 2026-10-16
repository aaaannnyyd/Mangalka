package eu.kanade.presentation.more.settings.screen.about

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.more.LogoHeader
import eu.kanade.presentation.more.settings.InkSettingsTopBar
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.UPDATE_CHANNEL
import eu.kanade.tachiyomi.ui.more.NewUpdateScreen
import eu.kanade.tachiyomi.util.lang.toDateTimestampString
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.isFossBuildType
import eu.kanade.tachiyomi.util.system.isNightlyBuildType
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.updaterEnabled
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import logcat.LogPriority
import mihon.app.di.appGraph
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkMangaBackdrop
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Instant

object AboutScreen : Screen() {

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val handleBack = LocalBackPress.current
        val navigator = LocalNavigator.currentOrThrow
        var isCheckingUpdates by remember { mutableStateOf(false) }
        val crashLogUtil = remember { context.appGraph.crashLogUtil }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(InkGlassTokens.BackgroundPrimary),
        ) {
            InkMangaBackdrop(intensity = 0.4f)
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    InkSettingsTopBar(
                        title = stringResource(MR.strings.pref_category_about),
                        navigateUp = if (handleBack != null) handleBack::invoke else null,
                    )
                },
            ) { contentPadding ->
                ScrollbarLazyColumn(
                    contentPadding = PaddingValues(
                        start = InkGlassTokens.ScreenHorizontal,
                        top = contentPadding.calculateTopPadding() + InkGlassTokens.Space8,
                        end = InkGlassTokens.ScreenHorizontal,
                        bottom = contentPadding.calculateBottomPadding() + InkGlassTokens.Space32,
                    ),
                ) {
                    item {
                        LogoHeader(
                            iconPadding = PaddingValues(vertical = 56.dp),
                        )
                    }

                    item {
                        TextPreferenceWidget(
                            title = stringResource(MR.strings.version),
                            subtitle = getVersionName(withBuildDate = true),
                            onPreferenceClick = {
                                val deviceInfo = crashLogUtil.getDebugInfo()
                                context.copyToClipboard("Debug information", deviceInfo)
                            },
                        )
                    }

                    if (updaterEnabled) {
                        item {
                            TextPreferenceWidget(
                                title = stringResource(MR.strings.check_for_updates),
                                widget = {
                                    AnimatedVisibility(visible = isCheckingUpdates) {
                                        androidx.compose.material3.Text(
                                            text = stringResource(MR.strings.ml_g4_about_checking),
                                            style = InkTypography.Eyebrow,
                                            color = InkGlassTokens.Accent,
                                        )
                                    }
                                },
                                onPreferenceClick = {
                                    if (!isCheckingUpdates) {
                                        scope.launch {
                                            isCheckingUpdates = true

                                            checkVersion(
                                                context = context,
                                                onAvailableUpdate = { result ->
                                                    val updateScreen = NewUpdateScreen(
                                                        versionName = result.release.version,
                                                        changelogInfo = result.release.info,
                                                        releaseLink = result.release.releaseLink,
                                                        downloadLink = result.release.downloadLink,
                                                    )
                                                    navigator.push(updateScreen)
                                                },
                                                onFinish = {
                                                    isCheckingUpdates = false
                                                },
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }

                    item {
                        TextPreferenceWidget(
                            title = stringResource(MR.strings.licenses),
                            onPreferenceClick = { navigator.push(OpenSourceLicensesScreen()) },
                        )
                    }

                    item {
                        TextPreferenceWidget(
                            title = stringResource(MR.strings.author),
                            subtitle = "anykebobd",
                        )
                    }

                    item {
                        TextPreferenceWidget(
                            title = stringResource(MR.strings.ml_g4_about_update_channel),
                            subtitle = "t.me/$UPDATE_CHANNEL",
                            onPreferenceClick = { uriHandler.openUri("https://t.me/$UPDATE_CHANNEL") },
                        )
                    }

                    item {
                        TextPreferenceWidget(
                            title = stringResource(MR.strings.ml_g4_about_source_code),
                            subtitle = "github.com/aaaannnyyd/Mangalka",
                            onPreferenceClick = { uriHandler.openUri("https://github.com/aaaannnyyd/Mangalka") },
                        )
                    }

                    item {
                        TextPreferenceWidget(
                            title = stringResource(MR.strings.ml_g4_about_upstream),
                            subtitle = stringResource(MR.strings.ml_g4_about_upstream_summary),
                            onPreferenceClick = { uriHandler.openUri("https://github.com/mihonapp/mihon") },
                        )
                    }
                }
            }
        }
    }

    /**
     * Checks version and shows a user prompt if an update is available.
     */
    private suspend fun checkVersion(
        context: Context,
        onAvailableUpdate: (GetApplicationRelease.Result.NewUpdate) -> Unit,
        onFinish: () -> Unit,
    ) {
        val updateChecker = context.appGraph.updateChecker
        withUIContext {
            try {
                when (val result = withIOContext { updateChecker.checkForUpdate(forceCheck = true) }) {
                    is GetApplicationRelease.Result.NewUpdate -> {
                        onAvailableUpdate(result)
                    }
                    is GetApplicationRelease.Result.NoNewUpdate -> {
                        context.toast(MR.strings.update_check_no_new_updates)
                    }
                    is GetApplicationRelease.Result.OsTooOld -> {
                        context.toast(MR.strings.update_check_eol)
                    }
                }
            } catch (e: Exception) {
                context.toast(e.message)
                logcat(LogPriority.ERROR, e)
            } finally {
                onFinish()
            }
        }
    }

    fun getVersionName(withBuildDate: Boolean): String {
        return when {
            BuildConfig.DEBUG -> {
                "Debug ${BuildConfig.COMMIT_SHA}".let {
                    if (withBuildDate) {
                        "$it (${getFormattedBuildTime()})"
                    } else {
                        it
                    }
                }
            }
            isNightlyBuildType -> {
                "Nightly r${BuildConfig.COMMIT_COUNT}".let {
                    if (withBuildDate) {
                        "$it (${BuildConfig.COMMIT_SHA}, ${getFormattedBuildTime()})"
                    } else {
                        "$it (${BuildConfig.COMMIT_SHA})"
                    }
                }
            }
            else -> {
                val channel = if (isFossBuildType) "FOSS" else "Stable"
                "$channel v${BuildConfig.VERSION_NAME}".let {
                    if (withBuildDate) {
                        "$it (${getFormattedBuildTime()})"
                    } else {
                        it
                    }
                }
            }
        }
    }

    internal fun getFormattedBuildTime(): String {
        return try {
            Instant.parse(BuildConfig.BUILD_TIME)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toDateTimestampString(
                    UiPreferences.dateFormat(
                        Injekt.get<Context>().appGraph.uiPreferences.dateFormat.get(),
                    ),
                )
        } catch (_: Exception) {
            BuildConfig.BUILD_TIME
        }
    }
}
