package tachiyomi.presentation.core.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Central visual contract for InkFlow's Premium Dark Manga Liquid Glass language. */
object InkGlassTokens {
    val BackgroundPrimary = Color(0xFF080A0C)
    val BackgroundSecondary = Color(0xFF0D1013)
    val BackgroundRaised = Color(0xFF121519)

    val GlassBase = Color(0xD0181C20)
    val GlassStrong = Color(0xE81B1F24)
    val GlassSoft = Color(0xB214171B)
    val GlassList = Color(0xD0191D21)
    val GlassBorder = Color(0x20FFFFFF)
    val GlassBorderSoft = Color(0x10FFFFFF)
    val GlassHighlight = Color(0x0AFFFFFF)
    val GlassShadow = Color(0x6B000000)

    val TextPrimary = Color(0xFFF5F5F6)
    val TextSecondary = Color(0xFFA6A8AD)
    val TextTertiary = Color(0xFF71757C)

    val Accent = Color(0xFFFF5967)
    val AccentPressed = Color(0xFFE94B59)
    val AccentSoft = Color(0x26FF5967)
    val AccentBorder = Color(0x9EFF5967)
    val Success = Color(0xFF72D69B)
    val Warning = Color(0xFFE6B96A)
    val Error = Accent
    val Scrim = Color(0x8F000000)

    val RadiusChip = 12.dp
    val RadiusSmall = 16.dp
    val RadiusMedium = 22.dp
    val RadiusCard = 28.dp
    val RadiusLargeCard = 32.dp
    val RadiusSheet = 36.dp
    val RadiusNavigation = 32.dp
    val RadiusHero = 30.dp

    val Space4 = 4.dp
    val Space8 = 8.dp
    val Space12 = 12.dp
    val Space16 = 16.dp
    val Space20 = 20.dp
    val Space24 = 24.dp
    val Space32 = 32.dp
    val Space40 = 40.dp
    val Space48 = 48.dp

    val ScreenHorizontal = Space20
    val CardPadding = Space20
    val TouchTarget = 48.dp
    val NavigationHeight = 68.dp
    val NavigationRailWidth = 72.dp
    val NavigationRailHeight = 304.dp
    val NavigationReservedSpace = 104.dp
    val CoverRadius = RadiusSmall
    val BorderWidth = 1.dp
}

object InkTypography {
    val Display = TextStyle(
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
        fontWeight = FontWeight.SemiBold,
    )
    val Headline = TextStyle(
        fontSize = 27.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.35).sp,
        fontWeight = FontWeight.SemiBold,
    )
    val Title = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
    val TitleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val Body = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal)
    val BodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    val Label = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    val Eyebrow = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.7.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

enum class InkGlassLevel { Soft, Standard, Strong, Accent }

enum class InkButtonStyle { Glass, Accent }

@Composable
fun InkGlassScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(InkGlassTokens.BackgroundSecondary, InkGlassTokens.BackgroundPrimary),
                ),
            ),
    ) {
        InkMangaBackdrop()
        content()
    }
}

@Composable
fun InkMangaBackdrop(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
) {
    val line = InkGlassTokens.TextPrimary.copy(alpha = 0.028f * intensity)
    val lineSoft = InkGlassTokens.TextPrimary.copy(alpha = 0.014f * intensity)
    val accent = InkGlassTokens.Accent.copy(alpha = 0.018f * intensity)
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.15f)

        // Ghosted manga-panel geometry. It should read as atmosphere, not decoration.
        drawRect(
            lineSoft,
            topLeft = Offset(w * 0.06f, h * 0.12f),
            size = androidx.compose.ui.geometry.Size(
                w * 0.38f,
                h * 0.23f,
            ),
            style = stroke,
        )
        drawRect(
            lineSoft,
            topLeft = Offset(w * 0.55f, h * 0.18f),
            size = androidx.compose.ui.geometry.Size(
                w * 0.34f,
                h * 0.28f,
            ),
            style = stroke,
        )
        drawRect(
            lineSoft,
            topLeft = Offset(w * 0.13f, h * 0.54f),
            size = androidx.compose.ui.geometry.Size(
                w * 0.46f,
                h * 0.29f,
            ),
            style = stroke,
        )

        drawCircle(line, radius = w * 0.20f, center = Offset(w * 0.78f, h * 0.58f), style = stroke)
        drawCircle(accent, radius = w * 0.13f, center = Offset(w * 0.78f, h * 0.58f), style = stroke)

        repeat(9) { index ->
            val y = h * (0.22f + index * 0.055f)
            drawLine(
                color = if (index % 4 == 0) line else lineSoft,
                start = Offset(w * 0.46f, y),
                end = Offset(w * (0.98f - index * 0.018f), y - h * 0.09f),
                strokeWidth = 1f,
            )
        }
        repeat(7) { index ->
            drawLine(
                color = lineSoft,
                start = Offset(w * 0.02f, h * (0.73f + index * 0.025f)),
                end = Offset(w * (0.40f + index * 0.035f), h * 0.49f),
                strokeWidth = 1f,
            )
        }
    }
}

@Composable
fun InkGlassSurface(
    modifier: Modifier = Modifier,
    level: InkGlassLevel = InkGlassLevel.Standard,
    shape: Shape = RoundedCornerShape(InkGlassTokens.RadiusCard),
    contentPadding: PaddingValues = PaddingValues(InkGlassTokens.CardPadding),
    shadowElevation: Dp = InkGlassTokens.Space4,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    role: Role? = null,
    prominent: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = tween(150),
        label = "inkGlassPressScale",
    )
    val (top, bottom, border) = when (level) {
        InkGlassLevel.Soft -> Triple(
            InkGlassTokens.GlassSoft,
            InkGlassTokens.GlassSoft.copy(alpha = 0.92f),
            InkGlassTokens.GlassBorderSoft,
        )
        InkGlassLevel.Standard -> Triple(InkGlassTokens.GlassList, InkGlassTokens.GlassBase, InkGlassTokens.GlassBorder)
        InkGlassLevel.Strong -> Triple(
            InkGlassTokens.GlassStrong,
            InkGlassTokens.GlassStrong.copy(alpha = 0.96f),
            InkGlassTokens.GlassBorder,
        )
        InkGlassLevel.Accent -> Triple(
            InkGlassTokens.GlassBase,
            InkGlassTokens.GlassBase,
            InkGlassTokens.AccentBorder.copy(alpha = 0.72f),
        )
    }
    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            role = role,
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick,
        )
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .scale(pressScale)
            .shadow(
                shadowElevation,
                shape,
                ambientColor = InkGlassTokens.GlassShadow,
                spotColor = InkGlassTokens.GlassShadow,
            )
            .clip(shape)
            .border(InkGlassTokens.BorderWidth, border, shape)
            .then(clickModifier),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(0f to top, 1f to bottom)),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (prominent) 0.055f else 0.028f),
                        0.08f to Color.White.copy(alpha = if (prominent) 0.018f else 0.010f),
                        0.24f to Color.Transparent,
                    ),
                ),
        )
        Box(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun InkGlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: InkButtonStyle = InkButtonStyle.Glass,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, tween(140), label = "inkButtonPress")
    val accent = style == InkButtonStyle.Accent
    val shape = RoundedCornerShape(InkGlassTokens.RadiusSmall)
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = InkGlassTokens.TouchTarget)
            .scale(scale)
            .clip(shape)
            .background(
                if (accent) InkGlassTokens.AccentSoft else InkGlassTokens.GlassSoft,
            )
            .border(
                InkGlassTokens.BorderWidth,
                if (accent) InkGlassTokens.AccentBorder else InkGlassTokens.GlassBorder,
                shape,
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = null,
            )
            .padding(horizontal = InkGlassTokens.Space20, vertical = InkGlassTokens.Space12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = if (accent) InkGlassTokens.Accent else InkGlassTokens.TextPrimary,
            )
            Spacer(Modifier.width(InkGlassTokens.Space8))
        }
        Text(
            label,
            style = InkTypography.Label,
            color = (if (accent) InkGlassTokens.Accent else InkGlassTokens.TextPrimary).copy(
                alpha = if (enabled) 1f else 0.42f,
            ),
        )
    }
}

@Composable
fun InkAccentButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) = InkGlassButton(label, onClick, modifier, InkButtonStyle.Accent, icon, enabled)

@Composable
fun InkGlassIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    InkGlassSurface(
        modifier = modifier.size(InkGlassTokens.TouchTarget),
        level = if (selected) InkGlassLevel.Accent else InkGlassLevel.Strong,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        shadowElevation = InkGlassTokens.Space4,
        onClick = onClick.takeIf { enabled },
        role = Role.Button,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.size(21.dp),
                tint = when {
                    !enabled -> InkGlassTokens.TextTertiary
                    selected -> InkGlassTokens.Accent
                    else -> InkGlassTokens.TextPrimary
                },
            )
        }
    }
}

@Composable
fun InkGlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(InkGlassTokens.RadiusChip)
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(shape)
            .background(if (selected) InkGlassTokens.AccentSoft else InkGlassTokens.GlassSoft)
            .border(
                InkGlassTokens.BorderWidth,
                if (selected) InkGlassTokens.AccentBorder else InkGlassTokens.GlassBorderSoft,
                shape,
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
                onLongClick = null,
            )
            .padding(horizontal = InkGlassTokens.Space12, vertical = InkGlassTokens.Space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            InkGlassTokens.Space8,
            alignment = Alignment.CenterHorizontally,
        ),
    ) {
        leadingIcon?.let {
            Icon(
                it,
                null,
                Modifier.size(16.dp),
                tint = if (selected) InkGlassTokens.Accent else InkGlassTokens.TextSecondary,
            )
        }
        Text(
            label,
            style = InkTypography.Label,
            color = if (selected) InkGlassTokens.Accent else InkGlassTokens.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun InkGlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(InkGlassTokens.RadiusMedium),
        containerColor = InkGlassTokens.GlassStrong,
        tonalElevation = 0.dp,
        shadowElevation = InkGlassTokens.Space12,
        border = BorderStroke(InkGlassTokens.BorderWidth, InkGlassTokens.GlassBorder),
        content = content,
    )
}

@Composable
fun InkGlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            eyebrow?.let { Text(it.uppercase(), style = InkTypography.Eyebrow, color = InkGlassTokens.Accent) }
            Text(title, style = InkTypography.Headline, color = InkGlassTokens.TextPrimary)
        }
        action?.invoke()
    }
}

data class InkNavigationItem(
    val key: String,
    val label: String,
    val icon: Painter,
    val selected: Boolean,
    val badgeCount: Int = 0,
    val onClick: () -> Unit,
)

@Composable
fun InkFloatingBottomNavigation(
    items: List<InkNavigationItem>,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier
            .padding(horizontal = InkGlassTokens.Space16, vertical = InkGlassTokens.Space8)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .fillMaxWidth(),
        level = InkGlassLevel.Strong,
        shape = RoundedCornerShape(InkGlassTokens.RadiusNavigation),
        contentPadding = PaddingValues(horizontal = InkGlassTokens.Space8, vertical = InkGlassTokens.Space8),
        shadowElevation = InkGlassTokens.Space16,
        prominent = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val interactionSource = remember(item.key) { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(if (pressed) 0.9f else 1f, tween(140), label = "inkNavPress")
                Box(
                    modifier = Modifier
                        .size(InkGlassTokens.TouchTarget)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(if (item.selected) InkGlassTokens.AccentSoft else Color.Transparent)
                        .border(
                            InkGlassTokens.BorderWidth,
                            if (item.selected) InkGlassTokens.AccentBorder.copy(alpha = 0.52f) else Color.Transparent,
                            CircleShape,
                        )
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = item.onClick,
                            onLongClick = null,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        item.icon,
                        item.label,
                        Modifier.size(23.dp),
                        tint = if (item.selected) InkGlassTokens.Accent else InkGlassTokens.TextSecondary,
                    )
                    if (item.badgeCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(InkGlassTokens.Accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                item.badgeCount.coerceAtMost(9).toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkGlassTokens.BackgroundPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InkFloatingNavigationRail(
    items: List<InkNavigationItem>,
    modifier: Modifier = Modifier,
) {
    InkGlassSurface(
        modifier = modifier
            .padding(horizontal = InkGlassTokens.Space12, vertical = InkGlassTokens.Space16)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(InkGlassTokens.NavigationRailHeight)
            .width(InkGlassTokens.NavigationRailWidth),
        level = InkGlassLevel.Strong,
        shape = RoundedCornerShape(InkGlassTokens.RadiusNavigation),
        contentPadding = PaddingValues(vertical = InkGlassTokens.Space12),
        shadowElevation = InkGlassTokens.Space16,
        prominent = true,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.forEach { item ->
                InkNavigationButton(item)
                if (item != items.last()) Spacer(Modifier.height(InkGlassTokens.Space12))
            }
        }
    }
}

@Composable
private fun InkNavigationButton(item: InkNavigationItem) {
    val interactionSource = remember(item.key) { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, tween(140), label = "inkNavRailPress")
    Box(
        modifier = Modifier
            .size(InkGlassTokens.TouchTarget)
            .scale(scale)
            .clip(CircleShape)
            .background(if (item.selected) InkGlassTokens.AccentSoft else Color.Transparent)
            .border(
                InkGlassTokens.BorderWidth,
                if (item.selected) InkGlassTokens.AccentBorder.copy(alpha = 0.52f) else Color.Transparent,
                CircleShape,
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = item.onClick,
                onLongClick = null,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            item.icon,
            item.label,
            Modifier.size(23.dp),
            tint = if (item.selected) InkGlassTokens.Accent else InkGlassTokens.TextSecondary,
        )
        if (item.badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(InkGlassTokens.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    item.badgeCount.coerceAtMost(9).toString(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkGlassTokens.BackgroundPrimary,
                )
            }
        }
    }
}

@Composable
fun InkMangaCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
    cover: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            role = Role.Button,
            onClick = onClick,
            onLongClick = null,
        ),
        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space8),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(InkGlassTokens.CoverRadius))
                .background(InkGlassTokens.BackgroundRaised)
                .border(
                    InkGlassTokens.BorderWidth,
                    InkGlassTokens.GlassBorderSoft,
                    RoundedCornerShape(InkGlassTokens.CoverRadius),
                ),
            content = cover,
        )
        Text(
            title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = InkTypography.TitleSmall,
            color = InkGlassTokens.TextPrimary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                subtitle,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = InkTypography.BodySmall,
                color = InkGlassTokens.TextSecondary,
            )
            meta?.let { Text(it, style = InkTypography.Eyebrow, color = InkGlassTokens.TextTertiary) }
        }
    }
}

@Composable
fun InkCircularProgress(
    modifier: Modifier = Modifier.size(24.dp),
    progress: Float? = null,
    strokeWidth: Dp = 2.dp,
    color: Color = InkGlassTokens.Accent,
    trackColor: Color = InkGlassTokens.GlassBorderSoft,
) {
    val transition = rememberInfiniteTransition(label = "inkCircularProgress")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "inkCircularRotation",
    )
    Canvas(modifier) {
        val stroke = strokeWidth.toPx().coerceAtMost(size.minDimension / 2f)
        if (trackColor.alpha > 0f) {
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        drawArc(
            color = color,
            startAngle = -90f + if (progress == null) rotation else 0f,
            sweepAngle = if (progress == null) 92f else progress.coerceIn(0f, 1f) * 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun InkGlassProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = InkGlassTokens.Space4) {
    val value = progress.coerceIn(0f, 1f)
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(InkGlassTokens.GlassBorder)) {
        Box(
            Modifier
                .fillMaxWidth(value)
                .height(height)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(InkGlassTokens.AccentPressed, InkGlassTokens.Accent))),
        )
    }
}

@Composable
fun InkSectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = InkGlassTokens.TextPrimary, style = InkTypography.Title)
        trailing?.invoke()
    }
}

@Composable
fun InkLoadingState(modifier: Modifier = Modifier, label: String = "Загружаем вашу мангу") {
    val transition = rememberInfiniteTransition(label = "inkLoading")
    val alpha by transition.animateFloat(
        0.28f,
        0.78f,
        infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "inkLoadingAlpha",
    )
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space16),
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(InkGlassTokens.RadiusSmall))
                .background(InkGlassTokens.AccentSoft.copy(alpha = alpha))
                .border(
                    InkGlassTokens.BorderWidth,
                    InkGlassTokens.AccentBorder.copy(alpha = alpha),
                    RoundedCornerShape(InkGlassTokens.RadiusSmall),
                ),
        )
        Text(label, style = InkTypography.Body, color = InkGlassTokens.TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
fun InkEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    InkGlassSurface(modifier, InkGlassLevel.Soft, contentPadding = PaddingValues(InkGlassTokens.Space24)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(Modifier.width(42.dp).height(InkGlassTokens.Space4).clip(CircleShape).background(InkGlassTokens.Accent))
            Text(title, style = InkTypography.Title, color = InkGlassTokens.TextPrimary)
            Text(message, style = InkTypography.Body, color = InkGlassTokens.TextSecondary)
            if (actionLabel != null && onAction != null) {
                InkGlassButton(actionLabel, onAction, style = InkButtonStyle.Accent)
            }
        }
    }
}

@Composable
fun InkErrorState(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Повторить",
) {
    InkGlassSurface(
        modifier = modifier,
        level = InkGlassLevel.Soft,
        contentPadding = PaddingValues(InkGlassTokens.Space24),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(InkGlassTokens.Space12),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                Modifier
                    .width(42.dp)
                    .height(InkGlassTokens.Space4)
                    .clip(CircleShape)
                    .background(InkGlassTokens.Error),
            )
            Text(title, style = InkTypography.Title, color = InkGlassTokens.TextPrimary)
            Text(message, style = InkTypography.Body, color = InkGlassTokens.TextSecondary)
            InkAccentButton(retryLabel, onRetry)
        }
    }
}
