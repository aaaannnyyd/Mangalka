package eu.kanade.presentation.more.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.icerock.moko.resources.StringResource
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkMangaBackdrop
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun PreferenceScaffold(
    titleRes: StringResource,
    actions: @Composable RowScope.() -> Unit = {},
    onBackPressed: (() -> Unit)? = null,
    itemsProvider: @Composable () -> List<Preference>,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkGlassTokens.BackgroundPrimary),
    ) {
        InkMangaBackdrop(intensity = 0.35f)
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                InkSettingsTopBar(
                    title = stringResource(titleRes),
                    navigateUp = onBackPressed,
                    actions = actions,
                )
            },
            content = { contentPadding ->
                PreferenceScreen(
                    items = itemsProvider(),
                    contentPadding = contentPadding,
                )
            },
        )
    }
}
