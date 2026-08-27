package io.github.kdroidfilter.ytdlpgui.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.nucleusframework.macoscompose.components.Checkbox
import dev.nucleusframework.macoscompose.components.ComboBox as MacosComboBox
import dev.nucleusframework.macoscompose.components.DialogSize as MacosDialogSize
import dev.nucleusframework.macoscompose.components.LinearProgress
import dev.nucleusframework.macoscompose.components.PushButton
import dev.nucleusframework.macoscompose.components.PushButtonStyle
import dev.nucleusframework.macoscompose.components.SmallDialog
import dev.nucleusframework.macoscompose.components.Spinner
import dev.nucleusframework.macoscompose.components.Switcher as MacosSwitcher
import dev.nucleusframework.macoscompose.components.VerticalScrollbar as MacosVerticalScrollbar
import dev.nucleusframework.macoscompose.components.rememberScrollbarState
import dev.nucleusframework.macoscompose.components.TextField as MacosTextField
import dev.nucleusframework.macoscompose.icons.Icon as MacosIcon
import dev.nucleusframework.macoscompose.icons.SystemIcon
import dev.nucleusframework.macoscompose.theme.AccentColor
import dev.nucleusframework.macoscompose.theme.ControlSize
import dev.nucleusframework.macoscompose.theme.MacosTheme
import dev.nucleusframework.macoscompose.theme.darkColorScheme
import dev.nucleusframework.macoscompose.theme.lightColorScheme
import dev.nucleusframework.systemcolor.systemAccentColor
import io.github.kdroidfilter.ytdlpgui.ui.contrastingOnColor
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
    val systemAccent = systemAccentColor()
    val namedAccent = systemAccent?.closestAccent() ?: AccentColor.Blue
    val baseScheme = if (darkTheme) darkColorScheme(namedAccent) else lightColorScheme(namedAccent)
    val colorScheme = if (systemAccent == null) {
        baseScheme
    } else {
        baseScheme.copy(
            accent = systemAccent,
            onAccent = systemAccent.contrastingOnColor(),
            tertiary = systemAccent,
            surfaceTint = systemAccent,
            info = systemAccent,
            inputFocusBorder = systemAccent,
            ring = systemAccent,
        )
    }
    MacosTheme(
        darkTheme = darkTheme,
        accentColor = namedAccent,
        colorScheme = colorScheme,
        liquidGlass = false,
    ) {
        ControlSize(ControlSize.Small) {
            val scheme = MacosTheme.colorScheme
            val typography = MacosTheme.typography
            val nativeColors = remember(scheme) { scheme.toNative() }
            val nativeTypography = remember(typography) {
                NativeTypography(
                    subtitle = typography.headline,
                    body = typography.footnote,
                    bodyStrong = typography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    caption = typography.caption1,
                )
            }
            val shapes = MacosTheme.shapes
            val nativeShapes = remember(shapes) { NativeShapes(control = shapes.medium) }
            CompositionLocalProvider(
                LocalNativeColors provides nativeColors,
                LocalNativeTypography provides nativeTypography,
                LocalNativeShapes provides nativeShapes,
                LocalNativeContentColor provides scheme.textPrimary,
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun NativeBackgroundImpl(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.background(MacosTheme.colorScheme.background)) {
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
    val name = icon.sfSymbolName ?: return
    val fallback = icon.imageVector ?: return
    val resolved = tint.takeOrElse {
        LocalNativeContentColor.current.takeOrElse { MacosTheme.colorScheme.textPrimary }
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val iconSize = when {
            constraints.hasFixedWidth && constraints.hasFixedHeight -> minOf(maxWidth, maxHeight)
            constraints.hasFixedWidth -> maxWidth
            constraints.hasFixedHeight -> maxHeight
            else -> 16.dp
        }
        MacosIcon(
            icon = SystemIcon(name, fallback),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = resolved,
        )
    }
}

@Composable
internal fun AccentButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    disabled: Boolean,
    @Suppress("UNUSED_PARAMETER") iconOnly: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    ControlSize(ControlSize.ExtraLarge) {
        PushButton(
            onClick = onClick,
            modifier = modifier,
            style = PushButtonStyle.Default,
            enabled = !disabled,
            content = content,
        )
    }
}

@Composable
internal fun ButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    disabled: Boolean,
    @Suppress("UNUSED_PARAMETER") iconOnly: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    PushButton(
        onClick = onClick,
        modifier = modifier,
        style = PushButtonStyle.Neutral,
        enabled = !disabled,
        content = content,
    )
}

@Composable
internal fun SubtleButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    disabled: Boolean,
    @Suppress("UNUSED_PARAMETER") iconOnly: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    PushButton(
        onClick = onClick,
        modifier = modifier,
        style = PushButtonStyle.Borderless,
        enabled = !disabled,
        content = content,
    )
}

@Composable
internal fun HyperlinkButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    disabled: Boolean,
    @Suppress("UNUSED_PARAMETER") iconOnly: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    PushButton(
        onClick = onClick,
        modifier = modifier,
        style = PushButtonStyle.Borderless,
        enabled = !disabled,
        content = content,
    )
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
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckStateChange.takeIf { enabled },
            enabled = enabled,
        )
        if (label != null) {
            Spacer(Modifier.width(8.dp))
            Text(label)
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
    MacosComboBox(
        items = items,
        selected = selected,
        onSelectionChange = onSelectionChange,
        modifier = modifier,
        header = header,
        placeholder = placeholder,
        disabled = disabled,
    )
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
    ControlSize(ControlSize.ExtraLarge) {
        MacosTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            singleLine = singleLine,
            label = header,
            trailingIcon = trailing?.let { slot -> { Row(content = slot) } },
            placeholder = placeholder,
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
    if (textBefore && text != null) {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(text)
            Spacer(Modifier.width(12.dp))
            MacosSwitcher(
                checked = checked,
                onCheckedChange = onCheckStateChange,
                enabled = enabled,
            )
        }
    } else {
        MacosSwitcher(
            checked = checked,
            onCheckedChange = onCheckStateChange,
            modifier = modifier,
            label = text,
            enabled = enabled,
        )
    }
}

@Composable
internal fun ProgressRingImpl(modifier: Modifier, progress: Float?, color: Color) {
    if (progress == null) {
        BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
            val spinnerSize = when {
                constraints.hasFixedWidth && constraints.hasFixedHeight -> minOf(maxWidth, maxHeight)
                else -> 16.dp
            }
            Spinner(size = spinnerSize, color = color)
        }
    } else {
        val stroke = 3.dp
        Canvas(modifier) {
            val strokePx = stroke.toPx()
            val diameter = minOf(size.width, size.height) - strokePx
            if (diameter <= 0f) return@Canvas
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = color.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
internal fun ProgressBarImpl(modifier: Modifier, progress: Float?, @Suppress("UNUSED_PARAMETER") color: Color) {
    LinearProgress(
        value = if (progress != null) progress * 100f else 0f,
        max = 100f,
        indeterminate = progress == null,
        modifier = modifier,
    )
}

@Suppress("UNUSED_PARAMETER")
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
    SmallDialog(
        visible = visible,
        onDismissRequest = { onButtonClick(ContentDialogButton.Close) },
        title = title,
        confirmText = primaryButtonText,
        onConfirm = { onButtonClick(ContentDialogButton.Primary) },
        cancelText = closeButtonText ?: secondaryButtonText,
        onCancel = {
            onButtonClick(
                if (closeButtonText != null) ContentDialogButton.Close else ContentDialogButton.Secondary,
            )
        },
        size = when {
            size.min == DialogSize.Min.min -> MacosDialogSize.Small
            size.min == DialogSize.Max.min -> MacosDialogSize.Large
            else -> MacosDialogSize.Standard
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
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.backgroundColor, shape)
            .border(0.5.dp, MacosTheme.colorScheme.borderSubtle, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                icon()
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            title()
            message()
        }
        if (action != null || closeAction != null) {
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                action?.invoke()
                closeAction?.invoke()
            }
        }
    }
}

@Composable
internal fun CardExpanderItemImpl(
    heading: @Composable () -> Unit,
    modifier: Modifier,
    icon: (@Composable () -> Unit)?,
    caption: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    val scheme = MacosTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceContainer, shape)
            .border(0.5.dp, scheme.borderSubtle, shape),
    ) {
        val maxTrailingWidth = maxWidth * 0.45f
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.widthIn(min = 40.dp).defaultMinSize(minWidth = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
            } else {
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.padding(vertical = 10.dp).weight(1f)) {
                heading()
                CompositionLocalProvider(LocalNativeContentColor provides scheme.textSecondary) {
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
    Row(
        modifier = modifier
            .height(44.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        scope.items.forEachIndexed { index, item ->
            if (index == scope.items.lastIndex && scope.items.size > 1) {
                Spacer(Modifier.weight(1f))
            }
            item()
        }
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
    PushButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 28.dp),
        style = if (selected) PushButtonStyle.Secondary else PushButtonStyle.Borderless,
        selected = selected,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.invoke()
            if (icon != null && text != null) Spacer(Modifier.width(6.dp))
            text?.invoke()
            if (badge != null) {
                Spacer(Modifier.width(6.dp))
                badge()
            }
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
    val scheme = MacosTheme.colorScheme
    val shape = RoundedCornerShape(13.dp)
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
                        .shadow(12.dp, shape)
                        .background(scheme.popover, shape)
                        .border(0.5.dp, scheme.borderSubtle, shape)
                        .padding(vertical = 6.dp)
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
            val gap = 6
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionType == ListItemSelectionType.Check && selected != null) {
            Checkbox(checked = selected, onCheckedChange = null)
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
            .background(MacosTheme.colorScheme.borderSubtle),
    )
}

@Composable
internal fun SegmentedControlImpl(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    val scheme = MacosTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(scheme.surfaceContainer, shape)
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
    PushButton(
        onClick = { onCheckedChanged(!checked) },
        modifier = modifier,
        style = if (checked) PushButtonStyle.Neutral else PushButtonStyle.Borderless,
        selected = checked,
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
    val state = when {
        adapter.scrollState != null -> rememberScrollbarState(adapter.scrollState)
        adapter.lazyListState != null -> rememberScrollbarState(adapter.lazyListState)
        else -> return
    }
    MacosVerticalScrollbar(state = state, modifier = modifier)
}

private fun Color.closestAccent(): AccentColor {
    val r = red
    val g = green
    val b = blue
    return AccentColor.entries.minBy { candidate ->
        val c = candidate.light
        val dr = r - c.red
        val dg = g - c.green
        val db = b - c.blue
        dr * dr + dg * dg + db * db
    }
}

private fun dev.nucleusframework.macoscompose.theme.ColorScheme.toNative(): NativeColors =
    NativeColors(
        system = NativeColors.System(
            success = success,
            critical = error,
            caution = warning,
            neutral = onSurface,
            attention = info,
        ),
        text = NativeColors.TextPalette(
            text = NativeColors.TextLevels(
                primary = textPrimary,
                secondary = textSecondary,
                tertiary = textTertiary,
                disabled = textQuaternary,
            ),
            onAccent = NativeColors.OnAccent(
                primary = onAccent,
                disabled = onAccent.copy(alpha = 0.38f),
            ),
        ),
        fillAccent = NativeColors.FillAccent(default = accent),
        stroke = NativeColors.StrokePalette(
            control = NativeColors.StrokePalette.StrokeControl(default = outline),
        ),
        background = NativeColors.BackgroundPalette(
            layer = NativeColors.BackgroundPalette.Layer(default = surfaceContainer),
        ),
        control = NativeColors.ControlPalette(secondary = muted),
        controlStrong = NativeColors.ControlStrongPalette(default = outline),
    )
