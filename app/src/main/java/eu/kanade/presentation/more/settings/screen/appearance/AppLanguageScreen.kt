package eu.kanade.presentation.more.settings.screen.appearance

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.InkSettingsTopBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.LocaleHelper
import org.xmlpull.v1.XmlPullParser
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.InkGlassLevel
import tachiyomi.presentation.core.components.InkGlassSurface
import tachiyomi.presentation.core.components.InkGlassTokens
import tachiyomi.presentation.core.components.InkTypography
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

class AppLanguageScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val langs = remember { getLangs(context) }
        var currentLanguage by remember {
            mutableStateOf(AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag() ?: "")
        }

        LaunchedEffect(currentLanguage) {
            val locale = if (currentLanguage.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(currentLanguage)
            }
            AppCompatDelegate.setApplicationLocales(locale)
        }

        Scaffold(
            containerColor = InkGlassTokens.BackgroundPrimary,
            topBar = {
                InkSettingsTopBar(
                    title = stringResource(MR.strings.pref_app_language),
                    navigateUp = navigator::pop,
                )
            },
        ) { contentPadding ->
            LazyColumn(
                contentPadding = PaddingValues(
                    start = InkGlassTokens.ScreenHorizontal,
                    top = contentPadding.calculateTopPadding() + InkGlassTokens.Space8,
                    end = InkGlassTokens.ScreenHorizontal,
                    bottom = contentPadding.calculateBottomPadding() + InkGlassTokens.Space32,
                ),
                verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
            ) {
                items(langs, key = { it.langTag }) { language ->
                    val selected = currentLanguage == language.langTag
                    InkGlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        level = if (selected) InkGlassLevel.Accent else InkGlassLevel.Soft,
                        shape = RoundedCornerShape(InkGlassTokens.RadiusSmall),
                        contentPadding = PaddingValues(
                            horizontal = InkGlassTokens.Space16,
                            vertical = InkGlassTokens.Space12,
                        ),
                        shadowElevation = androidx.compose.ui.unit.Dp.Hairline,
                        onClick = { currentLanguage = language.langTag },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    language.displayName,
                                    style = InkTypography.TitleSmall,
                                    color = InkGlassTokens.TextPrimary,
                                )
                                language.localizedDisplayName?.let {
                                    Text(
                                        it,
                                        style = InkTypography.BodySmall,
                                        color = InkGlassTokens.TextSecondary,
                                    )
                                }
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = InkGlassTokens.Accent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getLangs(context: Context): List<Language> = buildList {
        val parser = context.resources.getXml(R.xml.locales_config)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                for (i in 0..<parser.attributeCount) {
                    if (parser.getAttributeName(i) == "name") {
                        val langTag = parser.getAttributeValue(i)
                        val displayName = LocaleHelper.getLocalizedDisplayName(langTag)
                        if (displayName.isNotEmpty()) {
                            add(Language(langTag, displayName, LocaleHelper.getDisplayName(langTag)))
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        sortBy { it.displayName }
        add(0, Language("", context.stringResource(MR.strings.label_default), null))
    }

    private data class Language(
        val langTag: String,
        val displayName: String,
        val localizedDisplayName: String?,
    )
}
