package io.github.kdroidfilter.ytdlpgui.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.nucleusframework.yarucompose.icons.YaruIcon
import dev.nucleusframework.yarucompose.themes.LocalYaruColorScheme
import dev.nucleusframework.yarucompose.themes.LocalYaruContentColor
import dev.nucleusframework.yarucompose.themes.LocalYaruTypography
import dev.nucleusframework.systemcolor.isSystemInHighContrast
import dev.nucleusframework.systemcolor.systemAccentColor
import dev.nucleusframework.yarucompose.themes.YaruTheme
import dev.nucleusframework.yarucompose.themes.YaruVariant
import dev.nucleusframework.yarucompose.themes.isLight
import dev.nucleusframework.yarucompose.themes.success
import dev.nucleusframework.yarucompose.themes.warning
import dev.nucleusframework.yarucompose.themes.yaruDarkScheme
import dev.nucleusframework.yarucompose.themes.yaruLightScheme
import dev.nucleusframework.yarucompose.themes.yaruSystemAccentVariant
import dev.nucleusframework.yarucompose.widgets.YaruButton
import dev.nucleusframework.yarucompose.widgets.YaruButtonVariant
import dev.nucleusframework.yarucompose.widgets.YaruCheckbox
import dev.nucleusframework.yarucompose.widgets.YaruCircularProgressIndicator
import dev.nucleusframework.yarucompose.widgets.YaruDialog
import dev.nucleusframework.yarucompose.widgets.YaruIconButton
import dev.nucleusframework.yarucompose.widgets.YaruInfoBox
import dev.nucleusframework.yarucompose.widgets.YaruInfoType
import dev.nucleusframework.yarucompose.widgets.YaruLinearProgressIndicator
import dev.nucleusframework.yarucompose.widgets.YaruPopupMenuButton
import dev.nucleusframework.yarucompose.widgets.YaruScrollbar
import dev.nucleusframework.yarucompose.widgets.YaruSwitch
import dev.nucleusframework.yarucompose.widgets.YaruText
import dev.nucleusframework.yarucompose.widgets.YaruTextField
import io.github.kdroidfilter.ytdlpgui.ui.LocalNativeColors
import io.github.kdroidfilter.ytdlpgui.ui.LocalNativeContentColor
import io.github.kdroidfilter.ytdlpgui.ui.LocalNativeShapes
import io.github.kdroidfilter.ytdlpgui.ui.LocalNativeTypography
import io.github.kdroidfilter.ytdlpgui.ui.NativeColors
import io.github.kdroidfilter.ytdlpgui.ui.NativeShapes
import io.github.kdroidfilter.ytdlpgui.ui.NativeTypography
import io.github.kdroidfilter.ytdlpgui.ui.icons.NativeIcon

internal const val NativeDrawsWindowChrome = false

@Composable
internal fun NativeThemeImpl(darkTheme: Boolean, content: @Composable () -> Unit) {
    val accent = systemAccentColor()
    val variant = yaruSystemAccentVariant() ?: YaruVariant.Orange
    YaruTheme(
        isDark = darkTheme,
        highContrast = isSystemInHighContrast(),
        variant = variant,
    ) {
        val scheme = if (accent != null) {
            if (darkTheme) yaruDarkScheme(primaryColor = accent) else yaruLightScheme(primaryColor = accent)
        } else {
            LocalYaruColorScheme.current
        }
        val typography = LocalYaruTypography.current
        val nativeColors = remember(scheme) { scheme.toNative() }
        val nativeTypography = remember(typography) {
            NativeTypography(
                subtitle = typography.titleLarge.copy(color = Color.Unspecified),
                body = typography.bodyMedium.copy(color = Color.Unspecified),
                bodyStrong = typography.titleSmall.copy(color = Color.Unspecified),
                caption = typography.bodySmall.copy(color = Color.Unspecified),
            )
        }
        val nativeShapes = remember { NativeShapes(control = RoundedCornerShape(8.dp)) }
        CompositionLocalProvider(
            LocalYaruColorScheme provides scheme,
            LocalYaruContentColor provides scheme.onSurface,
            LocalNativeColors provides nativeColors,
            LocalNativeTypography provides nativeTypography,
            LocalNativeShapes provides nativeShapes,
            LocalNativeContentColor provides scheme.onSurface,
        ) {
            content()
        }
    }
}

@Composable
internal fun NativeBackgroundImpl(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.background(LocalYaruColorScheme.current.surface)) {
        content()
    }
}

@Composable
internal fun GlyphIcon(
    icon: NativeIcon,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color,
) {
    val glyph = icon.glyph ?: return
    val resolved = tint.takeOrElse {
        LocalNativeContentColor.current.takeOrElse { LocalYaruContentColor.current }
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val iconSize = when {
            constraints.hasFixedWidth && constraints.hasFixedHeight -> minOf(maxWidth, maxHeight)
            constraints.hasFixedWidth -> maxWidth
            constraints.hasFixedHeight -> maxHeight
            else -> {
                var size = 20.dp
                if (constraints.hasBoundedWidth) size = minOf(size, maxWidth)
                if (constraints.hasBoundedHeight) size = minOf(size, maxHeight)
                size
            }
        }
        YaruIcon(
            glyph = glyph,
            modifier = Modifier.size(iconSize),
            size = iconSize,
            tint = resolved,
            semanticLabel = contentDescription,
        )
    }
}

@Composable
internal fun AccentButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    disabled: Boolean,
    iconOnly: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = LocalYaruColorScheme.current
    YaruButton(
        onClick = onClick,
        modifier = modifier,
        enabled = !disabled,
        variant = YaruButtonVariant.Filled,
        backgroundColor = scheme.primary,
        contentColor = scheme.onPrimary,
        contentPadding = if (iconOnly) PaddingValues(8.dp) else PaddingValues(16.dp),
    ) {
        ButtonContent(content)
    }
}

@Composable
internal fun ButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    disabled: Boolean,
    iconOnly: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    YaruButton(
        onClick = onClick,
        modifier = modifier,
        enabled = !disabled,
        variant = YaruButtonVariant.Outlined,
        contentPadding = if (iconOnly) PaddingValues(8.dp) else PaddingValues(16.dp),
    ) {
        ButtonContent(content)
    }
}

@Composable
internal fun SubtleButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    disabled: Boolean,
    iconOnly: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    if (iconOnly) {
        YaruIconButton(
            onPressed = onClick.takeUnless { disabled },
            modifier = modifier,
            enabled = !disabled,
            icon = { ButtonContent(content) },
        )
    } else {
        YaruButton(
            onClick = onClick,
            modifier = modifier,
            enabled = !disabled,
            variant = YaruButtonVariant.Text,
        ) {
            ButtonContent(content)
        }
    }
}

@Composable
internal fun HyperlinkButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    disabled: Boolean,
    iconOnly: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    YaruButton(
        onClick = onClick,
        modifier = modifier,
        enabled = !disabled,
        variant = YaruButtonVariant.Text,
        contentPadding = if (iconOnly) PaddingValues(8.dp) else PaddingValues(16.dp),
    ) {
        ButtonContent(content)
    }
}

@Composable
private fun ButtonContent(content: @Composable RowScope.() -> Unit) {
    val contentColor = LocalYaruContentColor.current
    CompositionLocalProvider(LocalNativeContentColor provides contentColor) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
internal fun CheckBoxImpl(
    checked: Boolean,
    onCheckStateChange: (Boolean) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    label: String?,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        YaruCheckbox(
            checked = checked,
            onCheckedChange = onCheckStateChange.takeIf { enabled },
            enabled = enabled,
        )
        if (label != null) {
            Spacer(Modifier.width(8.dp))
            YaruText(label)
        }
    }
}

@Composable
internal fun ComboBoxImpl(
    items: List<String>,
    selected: Int?,
    onSelectionChange: (index: Int, item: String) -> Unit,
    modifier: Modifier,
    header: String?,
    placeholder: String?,
    disabled: Boolean,
) {
    Column(modifier) {
        if (header != null) {
            YaruText(header)
            Spacer(Modifier.height(8.dp))
        }
        YaruPopupMenuButton(
            items = items,
            selected = selected?.let { items.getOrNull(it) },
            placeholder = placeholder ?: "",
            enabled = !disabled,
            modifier = Modifier.fillMaxWidth(),
            onSelected = { value ->
                val index = items.indexOf(value)
                if (index >= 0) onSelectionChange(index, value)
            },
        )
    }
}

@Composable
internal fun TextFieldImpl(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    singleLine: Boolean,
    header: (@Composable () -> Unit)?,
    trailing: (@Composable RowScope.() -> Unit)?,
    placeholder: (@Composable () -> Unit)?,
) {
    val trailingSlot = trailing?.let { slot ->
        @Composable { Row(verticalAlignment = Alignment.CenterVertically, content = slot) }
    }
    if (header != null) {
        Column(modifier) {
            header()
            Spacer(Modifier.height(8.dp))
            YaruTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = singleLine,
                placeholder = placeholder,
                trailing = trailingSlot,
            )
        }
    } else {
        YaruTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            singleLine = singleLine,
            placeholder = placeholder,
            trailing = trailingSlot,
        )
    }
}

@Composable
internal fun SwitcherImpl(
    checked: Boolean,
    onCheckStateChange: (Boolean) -> Unit,
    text: String?,
    textBefore: Boolean,
    enabled: Boolean,
    modifier: Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (textBefore && text != null) {
            Text(text)
            Spacer(Modifier.width(12.dp))
        }
        YaruSwitch(
            checked = checked,
            onCheckedChange = onCheckStateChange.takeIf { enabled },
            enabled = enabled,
        )
        if (!textBefore && text != null) {
            Spacer(Modifier.width(12.dp))
            Text(text)
        }
    }
}

@Composable
internal fun ProgressRingImpl(modifier: Modifier, progress: Float?, color: Color) {
    YaruCircularProgressIndicator(modifier = modifier, progress = progress, color = color)
}

@Composable
internal fun ProgressBarImpl(modifier: Modifier, progress: Float?, color: Color) {
    YaruLinearProgressIndicator(modifier = modifier, progress = progress, color = color)
}

@Composable
internal fun ContentDialogImpl(
    title: String,
    visible: Boolean,
    content: @Composable () -> Unit,
    primaryButtonText: String,
    onButtonClick: (ContentDialogButton) -> Unit,
    secondaryButtonText: String?,
    closeButtonText: String?,
    size: DialogSize,
) {
    if (!visible) return
    YaruDialog(
        onDismissRequest = { onButtonClick(ContentDialogButton.Close) },
        modifier = Modifier.width(size.min),
        title = { YaruText(title) },
        actions = {
            YaruButton(
                onClick = { onButtonClick(ContentDialogButton.Primary) },
                variant = YaruButtonVariant.Filled,
                backgroundColor = LocalYaruColorScheme.current.primary,
                contentColor = LocalYaruColorScheme.current.onPrimary,
            ) { YaruText(primaryButtonText) }
            if (secondaryButtonText != null) {
                YaruButton(onClick = { onButtonClick(ContentDialogButton.Secondary) }) {
                    YaruText(secondaryButtonText)
                }
            }
            if (closeButtonText != null) {
                YaruButton(
                    onClick = { onButtonClick(ContentDialogButton.Close) },
                    variant = YaruButtonVariant.Text,
                ) { YaruText(closeButtonText) }
            }
        },
    ) {
        content()
    }
}

@Composable
internal fun InfoBarImpl(
    title: @Composable () -> Unit,
    message: @Composable () -> Unit,
    modifier: Modifier,
    colors: InfoBarColors,
    icon: (@Composable () -> Unit)?,
    action: (@Composable () -> Unit)?,
    closeAction: (@Composable () -> Unit)?,
) {
    YaruInfoBox(
        type = YaruInfoType.Information,
        modifier = modifier,
        title = title,
        subtitle = message,
        icon = icon,
        color = colors.iconColor,
        trailing = if (action != null || closeAction != null) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    action?.invoke()
                    closeAction?.invoke()
                }
            }
        } else {
            null
        },
    )
}

@Composable
internal fun CardExpanderItemImpl(
    heading: @Composable () -> Unit,
    modifier: Modifier,
    icon: (@Composable () -> Unit)?,
    caption: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    val scheme = LocalYaruColorScheme.current
    val shape = RoundedCornerShape(8.dp)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceVariant, shape)
            .border(1.dp, scheme.outline.copy(alpha = 0.4f), shape),
    ) {
        val maxTrailingWidth = maxWidth * 0.45f
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.widthIn(min = 48.dp).defaultMinSize(minWidth = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
            } else {
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.padding(vertical = 13.dp).weight(1f)) {
                heading()
                CompositionLocalProvider(LocalYaruContentColor provides scheme.onSurfaceVariant) {
                    caption()
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.widthIn(max = maxTrailingWidth)) {
                trailing()
            }
        }
    }
}

@Composable
internal fun TopNavImpl(
    @Suppress("UNUSED_PARAMETER") expanded: Boolean,
    @Suppress("UNUSED_PARAMETER") onExpandedChanged: (Boolean) -> Unit,
    modifier: Modifier,
    content: TopNavScope.() -> Unit,
) {
    val scope = CollectedTopNavScope()
    scope.content()
    val items = scope.items
    val leading = items.firstOrNull()
    val trailing = items.takeIf { it.size > 1 }?.last()
    val tabs = if (items.size > 2) items.subList(1, items.lastIndex) else emptyList()
    val scheme = LocalYaruColorScheme.current
    val trackShape = RoundedCornerShape(8.dp)
    val trackAlpha = if (scheme.isLight) 0.14f else 0.12f

    Row(
        modifier = modifier
            .height(48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            if (tabs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(trackShape)
                        .background(scheme.onSurface.copy(alpha = trackAlpha), trackShape)
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEach { it() }
                }
            }
        }
        trailing?.invoke()
    }
}

@Composable
internal fun TopNavItemImpl(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    text: (@Composable () -> Unit)?,
    icon: (@Composable () -> Unit)?,
    badge: (@Composable () -> Unit)?,
) {
    val scheme = LocalYaruColorScheme.current
    val shape = RoundedCornerShape(6.dp)
    val selectedBg = if (scheme.isLight) {
        scheme.surface
    } else {
        scheme.onSurface.copy(alpha = 0.20f)
    }
    val contentColor = if (selected) {
        scheme.onSurface
    } else {
        scheme.onSurface.copy(alpha = if (scheme.isLight) 0.72f else 0.68f)
    }
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(shape)
            .background(
                color = if (selected) selectedBg else Color.Transparent,
                shape = shape,
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompositionLocalProvider(
            LocalYaruContentColor provides contentColor,
            LocalNativeContentColor provides contentColor,
        ) {
            if (icon != null) {
                Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            text?.invoke()
            badge?.invoke()
        }
    }
}

@Composable
internal fun MenuFlyoutContainerImpl(
    flyout: @Composable MenuFlyoutContainerScope.() -> Unit,
    modifier: Modifier,
    initialVisible: Boolean,
    placement: FlyoutPlacement,
    @Suppress("UNUSED_PARAMETER") adaptivePlacement: Boolean,
    content: @Composable MenuFlyoutContainerScope.() -> Unit,
) {
    val state = remember { MenuFlyoutState(initialVisible) }
    val scheme = LocalYaruColorScheme.current
    val shape = RoundedCornerShape(10.dp)
    val alignEnd = placement == FlyoutPlacement.BottomAlignedEnd
    Box(modifier) {
        content(state)
        if (state.isFlyoutVisible) {
            Popup(
                popupPositionProvider = remember(alignEnd) { belowAnchorPositionProvider(alignEnd) },
                onDismissRequest = { state.isFlyoutVisible = false },
                properties = PopupProperties(focusable = true),
            ) {
                Column(
                    Modifier
                        .shadow(8.dp, shape)
                        .background(scheme.surface, shape)
                        .border(1.dp, scheme.outline.copy(alpha = 0.4f), shape)
                        .padding(vertical = 8.dp)
                        .width(220.dp),
                ) {
                    flyout(state)
                }
            }
        }
    }
}

private fun belowAnchorPositionProvider(alignEnd: Boolean): PopupPositionProvider =
    object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
        ): IntOffset {
            val gap = 8
            val x = if (alignEnd xor (layoutDirection == LayoutDirection.Rtl)) {
                anchorBounds.right - popupContentSize.width
            } else {
                anchorBounds.left
            }.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
            val yBelow = anchorBounds.bottom + gap
            val y = if (yBelow + popupContentSize.height <= windowSize.height) {
                yBelow
            } else {
                (anchorBounds.top - gap - popupContentSize.height).coerceAtLeast(0)
            }
            return IntOffset(x, y)
        }
    }

@Composable
internal fun MenuFlyoutItemImpl(
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier,
    icon: (@Composable () -> Unit)?,
    selected: Boolean?,
    onSelectedChanged: ((Boolean) -> Unit)?,
    selectionType: ListItemSelectionType,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (onSelectedChanged != null && selected != null) {
                    onSelectedChanged(!selected)
                } else {
                    onClick()
                }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionType == ListItemSelectionType.Check && selected != null) {
            YaruCheckbox(checked = selected, onCheckedChange = null)
            Spacer(Modifier.width(8.dp))
        } else if (icon != null) {
            icon()
            Spacer(Modifier.width(8.dp))
        }
        Box(Modifier.weight(1f)) { text() }
    }
}

@Composable
internal fun MenuFlyoutSeparatorImpl(modifier: Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(1.dp)
            .background(LocalYaruColorScheme.current.outline.copy(alpha = 0.4f)),
    )
}

@Composable
internal fun TooltipBoxImpl(
    tooltip: @Composable () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    DefaultTooltipBox(tooltip, modifier, content)
}

@Composable
internal fun SegmentedControlImpl(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    val scheme = LocalYaruColorScheme.current
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(scheme.onSurface.copy(alpha = 0.08f), shape)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
internal fun SegmentedButtonImpl(
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    modifier: Modifier,
    @Suppress("UNUSED_PARAMETER") position: SegmentedItemPosition,
    icon: (@Composable () -> Unit)?,
    text: (@Composable () -> Unit)?,
) {
    val scheme = LocalYaruColorScheme.current
    YaruButton(
        onClick = { onCheckedChanged(!checked) },
        modifier = modifier,
        variant = if (checked) YaruButtonVariant.Filled else YaruButtonVariant.Text,
        backgroundColor = if (checked) scheme.primary.copy(alpha = 0.2f) else Color.Transparent,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.invoke()
            if (icon != null && text != null) Spacer(Modifier.width(6.dp))
            text?.invoke()
        }
    }
}

@Composable
internal fun VerticalScrollbarImpl(adapter: NativeScrollbarAdapter, modifier: Modifier) {
    val state = adapter.scrollState ?: adapter.lazyListState ?: return
    YaruScrollbar(state = state, modifier = modifier) {}
}

private fun dev.nucleusframework.yarucompose.themes.YaruColorScheme.toNative(): NativeColors =
    NativeColors(
        system = NativeColors.System(
            success = success,
            critical = error,
            caution = warning,
            neutral = onSurface,
            attention = primary,
        ),
        text = NativeColors.TextPalette(
            text = NativeColors.TextLevels(
                primary = onSurface,
                secondary = onSurfaceVariant,
                tertiary = onSurfaceVariant.copy(alpha = 0.7f),
                disabled = onSurface.copy(alpha = 0.38f),
            ),
            onAccent = NativeColors.OnAccent(
                primary = onPrimary,
                disabled = onPrimary.copy(alpha = 0.38f),
            ),
        ),
        fillAccent = NativeColors.FillAccent(default = primary),
        stroke = NativeColors.StrokePalette(
            control = NativeColors.StrokePalette.StrokeControl(default = outline),
        ),
        background = NativeColors.BackgroundPalette(
            layer = NativeColors.BackgroundPalette.Layer(default = surfaceVariant),
        ),
        control = NativeColors.ControlPalette(secondary = onSurface.copy(alpha = 0.12f)),
        controlStrong = NativeColors.ControlStrongPalette(default = onSurface.copy(alpha = 0.25f)),
    )
