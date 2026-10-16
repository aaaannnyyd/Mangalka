package eu.kanade.presentation.browse

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.DisplayMetrics
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.domain.extension.interactor.ExtensionSourceItem
import eu.kanade.presentation.browse.components.ExtensionIcon
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.more.settings.InkSettingsTopBar
import eu.kanade.presentation.more.settings.widget.InkPreferenceSwitch
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TrailingWidgetBuffer
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsViewModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkAccentButton
import tachiyomi.presentation.core.components.InkAlertDialog
import tachiyomi.presentation.core.components.InkGlassButton
import tachiyomi.presentation.core.components.InkGlassDropdownMenu
import tachiyomi.presentation.core.components.InkGlassIconButton
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ExtensionDetailsScreen(
    navigateUp: () -> Unit,
    state: ExtensionDetailsViewModel.State.Success,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickEnableAll: () -> Unit,
    onClickDisableAll: () -> Unit,
    onClickClearCookies: () -> Unit,
    onClickUninstall: () -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    onClickIncognito: (Boolean) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val url = remember(state.extension) {
        val regex = """https://raw.githubusercontent.com/(.+?)/(.+?)/.+""".toRegex()
        regex.find(state.extension.store?.indexUrl.orEmpty())
            ?.let {
                val (user, repo) = it.destructured
                "https://github.com/$user/$repo"
            }
            ?: state.extension.store?.indexUrl
    }

    var menuExpanded by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = InkGlassTokens.BackgroundPrimary,
        topBar = {
            InkSettingsTopBar(
                title = stringResource(MR.strings.label_extension_info),
                navigateUp = navigateUp,
                actions = {
                    if (url != null) {
                        InkGlassIconButton(
                            imageVector = Icons.AutoMirrored.Outlined.Launch,
                            contentDescription = stringResource(MR.strings.action_open_repo),
                            onClick = { uriHandler.openUri(url) },
                        )
                    }
                    androidx.compose.foundation.layout.Box {
                        InkGlassIconButton(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = null,
                            onClick = { menuExpanded = true },
                        )
                        InkGlassDropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(MR.strings.action_enable_all)) },
                                onClick = {
                                    menuExpanded = false
                                    onClickEnableAll()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(MR.strings.action_disable_all)) },
                                onClick = {
                                    menuExpanded = false
                                    onClickDisableAll()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(MR.strings.pref_clear_cookies)) },
                                onClick = {
                                    menuExpanded = false
                                    onClickClearCookies()
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        ExtensionDetails(
            contentPadding = paddingValues,
            extension = state.extension,
            sources = state.sources,
            incognitoMode = state.isIncognito,
            onClickSourcePreferences = onClickSourcePreferences,
            onClickUninstall = onClickUninstall,
            onClickSource = onClickSource,
            onClickIncognito = onClickIncognito,
        )
    }
}

@Composable
private fun ExtensionDetails(
    contentPadding: PaddingValues,
    extension: Extension.Installed,
    sources: List<ExtensionSourceItem>,
    incognitoMode: Boolean,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickUninstall: () -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    onClickIncognito: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var showNsfwWarning by remember { mutableStateOf(false) }

    ScrollbarLazyColumn(
        contentPadding = contentPadding,
    ) {
        if (extension.isObsolete) {
            item {
                WarningBanner(MR.strings.obsolete_extension_message)
            }
        }

        item {
            DetailsHeader(
                extension = extension,
                extIncognitoMode = incognitoMode,
                onClickUninstall = onClickUninstall,
                onClickAppInfo = {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", extension.pkgName, null)
                        context.startActivity(this)
                    }
                    Unit
                }.takeIf { extension.isShared },
                onClickAgeRating = {
                    showNsfwWarning = true
                },
                onExtIncognitoChange = onClickIncognito,
            )
        }

        items(
            items = sources,
            key = { it.source.id },
        ) { source ->
            SourceSwitchPreference(
                modifier = Modifier.animateItem(),
                source = source,
                onClickSourcePreferences = onClickSourcePreferences,
                onClickSource = onClickSource,
            )
        }
    }
    if (showNsfwWarning) {
        NsfwWarningDialog(
            onClickConfirm = {
                showNsfwWarning = false
            },
        )
    }
}

@Composable
private fun DetailsHeader(
    extension: Extension,
    extIncognitoMode: Boolean,
    onClickAgeRating: () -> Unit,
    onClickUninstall: () -> Unit,
    onClickAppInfo: (() -> Unit)?,
    onExtIncognitoChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.medium)
                .padding(
                    top = MaterialTheme.padding.medium,
                    bottom = MaterialTheme.padding.small,
                )
                .clickable {
                    val extDebugInfo = buildString {
                        append(
                            """
                            Extension name: ${extension.name} (lang: ${extension.lang}; package: ${extension.pkgName})
                            Extension version: ${extension.versionName} (lib: ${extension.libVersion}; version code: ${extension.versionCode})
                            NSFW: ${extension.isNsfw}
                            """.trimIndent(),
                        )

                        if (extension is Extension.Installed) {
                            append("\n\n")
                            appendLine(
                                """
                                Update available: ${extension.hasUpdate}
                                Orphaned: ${extension.isObsolete}
                                Shared: ${extension.isShared}
                                """.trimIndent(),
                            )
                            val store = extension.store
                            if (store != null) {
                                append("Repository: ${store.indexUrl}")
                            }
                        }
                    }
                    context.copyToClipboard("Extension Debug information", extDebugInfo)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ExtensionIcon(
                modifier = Modifier
                    .size(112.dp),
                extension = extension,
                density = DisplayMetrics.DENSITY_XXXHIGH,
            )

            Text(
                text = extension.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )

            val strippedPkgName = extension.pkgName.substringAfter("eu.kanade.tachiyomi.extension.")

            Text(
                text = strippedPkgName,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.padding.extraLarge,
                    vertical = MaterialTheme.padding.small,
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfoText(
                modifier = Modifier.weight(1f),
                primaryText = extension.versionName,
                secondaryText = stringResource(MR.strings.ext_info_version),
            )

            InfoDivider()

            InfoText(
                modifier = Modifier.weight(if (extension.isNsfw) 1.5f else 1f),
                primaryText = LocaleHelper.getSourceDisplayName(extension.lang, context),
                secondaryText = stringResource(MR.strings.ext_info_language),
            )

            if (extension.isNsfw) {
                InfoDivider()

                InfoText(
                    modifier = Modifier.weight(1f),
                    primaryText = stringResource(MR.strings.ext_nsfw_short),
                    primaryTextStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                    ),
                    secondaryText = stringResource(MR.strings.ext_info_age_rating),
                    onClick = onClickAgeRating,
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.padding.medium)
                .padding(top = MaterialTheme.padding.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            InkGlassButton(
                label = stringResource(MR.strings.ext_uninstall),
                modifier = Modifier.weight(1f),
                onClick = onClickUninstall,
            )

            if (onClickAppInfo != null) {
                InkAccentButton(
                    label = stringResource(MR.strings.ext_app_info),
                    modifier = Modifier.weight(1f),
                    onClick = onClickAppInfo,
                )
            }
        }

        TextPreferenceWidget(
            modifier = Modifier.padding(horizontal = MaterialTheme.padding.small),
            title = stringResource(MR.strings.pref_incognito_mode),
            subtitle = stringResource(MR.strings.pref_incognito_mode_extension_summary),
            icon = ImageVector.vectorResource(R.drawable.ic_glasses_24dp),
            widget = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InkPreferenceSwitch(
                        checked = extIncognitoMode,
                        modifier = Modifier.padding(start = TrailingWidgetBuffer),
                    )
                }
            },
        )
    }
}

@Composable
private fun InfoText(
    primaryText: String,
    secondaryText: String,
    modifier: Modifier = Modifier,
    primaryTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(interactionSource = null, indication = null, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier.then(clickableModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = primaryText,
            textAlign = TextAlign.Center,
            style = primaryTextStyle,
        )

        Text(
            text = secondaryText + if (onClick != null) " ⓘ" else "",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun InfoDivider() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .height(20.dp)
            .padding(horizontal = InkGlassTokens.Space8)
            .size(width = 1.dp, height = 20.dp)
            .background(InkGlassTokens.GlassBorder),
    )
}

@Composable
private fun SourceSwitchPreference(
    source: ExtensionSourceItem,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    TextPreferenceWidget(
        modifier = modifier,
        title = if (source.labelAsName) {
            source.source.toString()
        } else {
            LocaleHelper.getSourceDisplayName(source.source.lang, context)
        },
        widget = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (source.source is ConfigurableSource) {
                    InkGlassIconButton(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(MR.strings.label_settings),
                        onClick = { onClickSourcePreferences(source.source.id) },
                        modifier = Modifier.size(40.dp),
                    )
                }

                InkPreferenceSwitch(
                    checked = source.enabled,
                    modifier = Modifier.padding(start = TrailingWidgetBuffer),
                )
            }
        },
        onPreferenceClick = { onClickSource(source.source.id) },
    )
}

@Composable
private fun NsfwWarningDialog(
    onClickConfirm: () -> Unit,
) {
    InkAlertDialog(
        text = {
            Text(text = stringResource(MR.strings.ext_nsfw_warning))
        },
        confirmButton = {
            InkAccentButton(
                label = stringResource(MR.strings.action_ok),
                onClick = onClickConfirm,
            )
        },
        onDismissRequest = onClickConfirm,
    )
}
