@file:OptIn(ExperimentalFluentApi::class, ExperimentalFoundationApi::class)

package io.github.kdroidfilter.ytdlpgui.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import dev.nucleusframework.systemcolor.systemAccentColor
import io.github.composefluent.Colors
import io.github.composefluent.ExperimentalFluentApi
import io.github.composefluent.FluentTheme
import io.github.composefluent.Shades
import io.github.composefluent.background.Mica
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Button
import io.github.composefluent.component.CardExpanderItem
import io.github.composefluent.component.CheckBox
import io.github.composefluent.component.ComboBox
import io.github.composefluent.component.ContentDialog
import io.github.composefluent.component.HyperlinkButton
import io.github.composefluent.component.InfoBar
import io.github.composefluent.component.ProgressBar
import io.github.composefluent.component.ProgressRing
import io.github.composefluent.component.Scrollbar
import io.github.composefluent.component.SegmentedButton
import io.github.composefluent.component.SegmentedControl
import io.github.composefluent.component.SubtleButton
import io.github.composefluent.component.TextField
import io.github.composefluent.component.TooltipBox as FluentTooltipBox
import io.github.composefluent.component.FlyoutPlacement as FluentFlyoutPlacement
import io.github.composefluent.component.ListItemSelectionType as FluentListItemSelectionType
import io.github.composefluent.component.MenuFlyoutContainer as FluentMenuFlyoutContainer
import io.github.composefluent.component.MenuFlyoutItem as FluentMenuFlyoutItem
import io.github.composefluent.component.MenuFlyoutScope
import io.github.composefluent.component.MenuFlyoutSeparator as FluentMenuFlyoutSeparator
import io.github.composefluent.component.TopNav
import io.github.composefluent.component.TopNavItem
import io.github.kdroidfilter.ytdlpgui.ui.LocalNativeColors
import io.github.kdroidfilter.ytdlpgui.ui.LocalNativeContentColor
import io.github.kdroidfilter.ytdlpgui.ui.LocalNativeShapes
import io.github.kdroidfilter.ytdlpgui.ui.LocalNativeSizes
import io.github.kdroidfilter.ytdlpgui.ui.LocalNativeTypography
import io.github.kdroidfilter.ytdlpgui.ui.NativeColors
import io.github.kdroidfilter.ytdlpgui.ui.NativeShapes
import io.github.kdroidfilter.ytdlpgui.ui.NativeSizes
import io.github.kdroidfilter.ytdlpgui.ui.NativeTypography
import io.github.kdroidfilter.ytdlpgui.ui.icons.NativeIcon
import io.github.composefluent.component.ContentDialogButton as FluentContentDialogButton
import io.github.composefluent.component.DialogSize as FluentDialogSize
import io.github.composefluent.component.SegmentedItemPosition as FluentSegmentedItemPosition
import io.github.composefluent.component.rememberScrollbarAdapter as rememberFluentScrollbarAdapter

internal const val NativeDrawsWindowChrome = false

private val DefaultFluentAccent = Color(0xFF0078D4)

private fun shadesFromAccent(accent: Color): Shades = Shades(
    base = accent,
    light1 = lerp(accent, Color.White, 0.16f),
    light2 = lerp(accent, Color.White, 0.36f),
    light3 = lerp(accent, Color.White, 0.55f),
    dark1 = lerp(accent, Color.Black, 0.16f),
    dark2 = lerp(accent, Color.Black, 0.36f),
    dark3 = lerp(accent, Color.Black, 0.55f),
)

@OptIn(ExperimentalFluentApi::class)
@Composable
internal fun NativeThemeImpl(darkTheme: Boolean, content: @Composable () -> Unit) {
    val accent = systemAccentColor() ?: DefaultFluentAccent
    val fluentColors = remember(darkTheme, accent) {
        Colors(shadesFromAccent(accent), darkTheme)
    }
    FluentTheme(colors = fluentColors, useAcrylicPopup = false) {
        val nativeColors = fluentColors.toNative()
        val nativeTypography = NativeTypography(
            subtitle = FluentTheme.typography.subtitle,
            body = FluentTheme.typography.body,
            bodyStrong = FluentTheme.typography.bodyStrong,
            caption = FluentTheme.typography.caption,
        )
        CompositionLocalProvider(
            LocalNativeColors provides nativeColors,
            LocalNativeTypography provides nativeTypography,
            LocalNativeShapes provides NativeShapes(control = FluentTheme.shapes.control),
            LocalNativeSizes provides NativeSizes(control = 32.dp),
            LocalNativeContentColor provides fluentColors.text.text.primary,
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFluentApi::class)
@Composable
internal fun NativeBackgroundImpl(modifier: Modifier, content: @Composable () -> Unit) {
    Mica(modifier) { content() }
}

@Composable
internal fun GlyphIcon(
    icon: NativeIcon,
    contentDescription: String?,
    modifier: Modifier,
    tint: Color,
) {
    val vector = icon.imageVector ?: return
    Icon(imageVector = vector, contentDescription = contentDescription, modifier = modifier, tint = tint)
}

@Composable
internal fun AccentButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    disabled: Boolean,
    iconOnly: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    AccentButton(onClick = onClick, modifier = modifier, disabled = disabled, iconOnly = iconOnly, content = content)
}

@Composable
internal fun ButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    disabled: Boolean,
    iconOnly: Boolean,
    @Suppress("UNUSED_PARAMETER") large: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    Button(onClick = onClick, modifier = modifier, disabled = disabled, iconOnly = iconOnly, content = content)
}

@Composable
internal fun SubtleButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    disabled: Boolean,
    iconOnly: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    SubtleButton(onClick = onClick, modifier = modifier, disabled = disabled, iconOnly = iconOnly, content = content)
}

@Composable
internal fun HyperlinkButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    disabled: Boolean,
    iconOnly: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    HyperlinkButton(onClick = onClick, modifier = modifier, disabled = disabled, iconOnly = iconOnly, content = content)
}

@Composable
internal fun CheckBoxImpl(
    checked: Boolean,
    onCheckStateChange: (Boolean) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    label: String?,
) {
    CheckBox(
        checked = checked,
        onCheckStateChange = onCheckStateChange,
        modifier = modifier,
        enabled = enabled,
        label = label,
    )
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
    ComboBox(
        modifier = modifier,
        header = header,
        placeholder = placeholder,
        disabled = disabled,
        items = items,
        selected = selected,
        onSelectionChange = onSelectionChange,
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
    @Suppress("UNUSED_PARAMETER") large: Boolean,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        header = header,
        trailing = trailing,
        placeholder = placeholder,
    )
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
    Box(modifier) {
        io.github.composefluent.component.Switcher(
            checked = checked,
            onCheckStateChange = onCheckStateChange,
            text = text,
            textBefore = textBefore,
            enabled = enabled,
        )
    }
}

@Composable
internal fun ProgressRingImpl(modifier: Modifier, progress: Float?, color: Color) {
    if (progress != null) {
        ProgressRing(progress = progress, modifier = modifier, color = color)
    } else {
        ProgressRing(modifier = modifier, color = color)
    }
}

@Composable
internal fun ProgressBarImpl(modifier: Modifier, progress: Float?, color: Color) {
    if (progress != null) {
        ProgressBar(progress = progress, modifier = modifier, color = color)
    } else {
        ProgressBar(modifier = modifier, color = color)
    }
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
    ContentDialog(
        title = title,
        visible = visible,
        content = content,
        primaryButtonText = primaryButtonText,
        secondaryButtonText = secondaryButtonText,
        closeButtonText = closeButtonText,
        onButtonClick = { button ->
            onButtonClick(
                when (button) {
                    FluentContentDialogButton.Primary -> ContentDialogButton.Primary
                    FluentContentDialogButton.Secondary -> ContentDialogButton.Secondary
                    FluentContentDialogButton.Close -> ContentDialogButton.Close
                },
            )
        },
        size = when {
            size.min == DialogSize.Min.min -> FluentDialogSize.Min
            size.min == DialogSize.Max.min -> FluentDialogSize.Max
            else -> FluentDialogSize.Standard
        },
    )
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
    InfoBar(
        title = title,
        message = message,
        modifier = modifier,
        icon = icon,
        action = action,
        closeAction = closeAction,
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
    CardExpanderItem(
        heading = heading,
        modifier = modifier,
        icon = icon,
        caption = caption,
        trailing = trailing,
    )
}

@OptIn(ExperimentalFluentApi::class)
@Composable
internal fun TopNavImpl(
    expanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    modifier: Modifier,
    content: TopNavScope.() -> Unit,
) {
    val scope = CollectedTopNavScope()
    scope.content()
    TopNav(
        expanded = expanded,
        onExpandedChanged = onExpandedChanged,
        modifier = modifier,
    ) {
        scope.items.forEach { entry ->
            item { entry() }
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
    TopNavItem(
        selected = selected,
        onClick = { onClick() },
        modifier = modifier,
        text = text,
        icon = icon,
        badge = badge,
    )
}

private val LocalFluentMenuFlyoutScope = staticCompositionLocalOf<MenuFlyoutScope?> { null }

@Composable
internal fun MenuFlyoutContainerImpl(
    flyout: @Composable MenuFlyoutContainerScope.() -> Unit,
    modifier: Modifier,
    initialVisible: Boolean,
    placement: FlyoutPlacement,
    adaptivePlacement: Boolean,
    content: @Composable MenuFlyoutContainerScope.() -> Unit,
) {
    FluentMenuFlyoutContainer(
        modifier = modifier,
        initialVisible = initialVisible,
        placement = when (placement) {
            FlyoutPlacement.BottomAlignedEnd -> FluentFlyoutPlacement.BottomAlignedEnd
            FlyoutPlacement.Auto -> FluentFlyoutPlacement.Auto
        },
        adaptivePlacement = adaptivePlacement,
        flyout = {
            val host = this
            CompositionLocalProvider(LocalFluentMenuFlyoutScope provides host) {
                flyout(
                    object : MenuFlyoutContainerScope {
                        override var isFlyoutVisible: Boolean
                            get() = host.isFlyoutVisible
                            set(value) {
                                host.isFlyoutVisible = value
                            }
                    },
                )
            }
        },
        content = {
            val host = this
            content(
                object : MenuFlyoutContainerScope {
                    override var isFlyoutVisible: Boolean
                        get() = host.isFlyoutVisible
                        set(value) {
                            host.isFlyoutVisible = value
                        }
                },
            )
        },
    )
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
    val scope = LocalFluentMenuFlyoutScope.current
    if (scope != null) {
        with(scope) {
            if (selected != null && onSelectedChanged != null) {
                FluentMenuFlyoutItem(
                    selected = selected,
                    onSelectedChanged = onSelectedChanged,
                    text = text,
                    modifier = modifier,
                    icon = icon,
                    selectionType = when (selectionType) {
                        ListItemSelectionType.Check -> FluentListItemSelectionType.Check
                        ListItemSelectionType.Standard -> FluentListItemSelectionType.Standard
                    },
                )
            } else {
                FluentMenuFlyoutItem(
                    onClick = onClick,
                    text = text,
                    modifier = modifier,
                    icon = icon,
                )
            }
        }
    }
}

@Composable
internal fun MenuFlyoutSeparatorImpl(modifier: Modifier) {
    val scope = LocalFluentMenuFlyoutScope.current
    if (scope != null) {
        with(scope) {
            FluentMenuFlyoutSeparator(modifier)
        }
    }
}

@Composable
internal fun SegmentedControlImpl(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    SegmentedControl(modifier = modifier, content = content)
}

@Composable
internal fun TooltipBoxImpl(
    tooltip: @Composable () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    FluentTooltipBox(tooltip = tooltip, modifier = modifier, content = content)
}

@Composable
internal fun SegmentedButtonImpl(
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    modifier: Modifier,
    position: SegmentedItemPosition,
    icon: (@Composable () -> Unit)?,
    text: (@Composable () -> Unit)?,
) {
    SegmentedButton(
        checked = checked,
        onCheckedChanged = onCheckedChanged,
        modifier = modifier,
        position = when (position) {
            SegmentedItemPosition.Start -> FluentSegmentedItemPosition.Start
            SegmentedItemPosition.Center -> FluentSegmentedItemPosition.Center
            SegmentedItemPosition.End -> FluentSegmentedItemPosition.End
        },
        icon = icon,
        text = text,
    )
}

@Composable
internal fun VerticalScrollbarImpl(adapter: NativeScrollbarAdapter, modifier: Modifier) {
    val fluentAdapter = when {
        adapter.scrollState != null -> rememberFluentScrollbarAdapter(adapter.scrollState)
        adapter.lazyListState != null -> rememberFluentScrollbarAdapter(adapter.lazyListState)
        else -> return
    }
    Scrollbar(isVertical = true, adapter = fluentAdapter, modifier = modifier)
}

private fun io.github.composefluent.Colors.toNative(): NativeColors =
    NativeColors(
        system = NativeColors.System(
            success = system.success,
            critical = system.critical,
            caution = system.caution,
            neutral = system.neutral,
            attention = system.attention,
        ),
        text = NativeColors.TextPalette(
            text = NativeColors.TextLevels(
                primary = text.text.primary,
                secondary = text.text.secondary,
                tertiary = text.text.tertiary,
                disabled = text.text.disabled,
            ),
            onAccent = NativeColors.OnAccent(
                primary = text.onAccent.primary,
                disabled = text.onAccent.disabled,
            ),
        ),
        fillAccent = NativeColors.FillAccent(default = fillAccent.default),
        stroke = NativeColors.StrokePalette(
            control = NativeColors.StrokePalette.StrokeControl(default = stroke.control.default),
        ),
        background = NativeColors.BackgroundPalette(
            layer = NativeColors.BackgroundPalette.Layer(default = background.layer.default),
        ),
        control = NativeColors.ControlPalette(secondary = control.secondary),
        controlStrong = NativeColors.ControlStrongPalette(default = controlStrong.default),
    )
