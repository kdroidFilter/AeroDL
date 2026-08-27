package io.github.kdroidfilter.ytdlpgui.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.kdroidfilter.ytdlpgui.ui.NativeTheme
import io.github.kdroidfilter.ytdlpgui.ui.icons.Icons

@Composable
fun NativeBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    NativeBackgroundImpl(modifier, content)
}

@Composable
fun AccentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    iconOnly: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    AccentButtonImpl(onClick, modifier, disabled, iconOnly, content)
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    iconOnly: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    ButtonImpl(onClick, modifier, disabled, iconOnly, content)
}

@Composable
fun SubtleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    iconOnly: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    SubtleButtonImpl(onClick, modifier, disabled, iconOnly, content)
}

@Composable
fun HyperlinkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    iconOnly: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    HyperlinkButtonImpl(onClick, modifier, disabled, iconOnly, content)
}

@Composable
fun CheckBox(
    checked: Boolean,
    onCheckStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
) {
    CheckBoxImpl(checked, onCheckStateChange, modifier, enabled, label)
}

@Composable
fun ComboBox(
    items: List<String>,
    selected: Int?,
    onSelectionChange: (index: Int, item: String) -> Unit,
    modifier: Modifier = Modifier,
    header: String? = null,
    placeholder: String? = null,
    disabled: Boolean = false,
) {
    ComboBoxImpl(items, selected, onSelectionChange, modifier, header, placeholder, disabled)
}

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    header: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
) {
    TextFieldImpl(value, onValueChange, modifier, enabled, singleLine, header, trailing, placeholder)
}

@Composable
fun Switcher(
    checked: Boolean,
    onCheckStateChange: (Boolean) -> Unit,
    text: String? = null,
    textBefore: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    SwitcherImpl(checked, onCheckStateChange, text, textBefore, enabled, modifier)
}

@Composable
fun ProgressRing(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    color: Color = NativeTheme.colors.fillAccent.default,
) {
    ProgressRingImpl(modifier, progress, color)
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = NativeTheme.colors.fillAccent.default,
) {
    ProgressRingImpl(modifier, progress, color)
}

@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = NativeTheme.colors.fillAccent.default,
) {
    ProgressBarImpl(modifier, progress, color)
}

@Composable
fun ProgressBar(
    modifier: Modifier = Modifier,
    color: Color = NativeTheme.colors.fillAccent.default,
) {
    ProgressBarImpl(modifier, progress = null, color)
}

@Composable
fun ContentDialog(
    title: String,
    visible: Boolean,
    content: @Composable () -> Unit,
    primaryButtonText: String,
    onButtonClick: (ContentDialogButton) -> Unit,
    secondaryButtonText: String? = null,
    closeButtonText: String? = null,
    size: DialogSize = DialogSize.Standard,
) {
    ContentDialogImpl(
        title = title,
        visible = visible,
        content = content,
        primaryButtonText = primaryButtonText,
        onButtonClick = onButtonClick,
        secondaryButtonText = secondaryButtonText,
        closeButtonText = closeButtonText,
        size = size,
    )
}

@Composable
fun InfoBar(
    title: @Composable () -> Unit,
    message: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: InfoBarColors = InfoBarDefaults.colors(),
    icon: (@Composable () -> Unit)? = { InfoBarDefaults.Badge() },
    action: (@Composable () -> Unit)? = null,
    closeAction: (@Composable () -> Unit)? = null,
) {
    InfoBarImpl(title, message, modifier, colors, icon, action, closeAction)
}

object InfoBarDefaults {
    @Composable
    fun colors(): InfoBarColors = InfoBarColors(
        backgroundColor = NativeTheme.colors.background.layer.default,
        contentColor = NativeTheme.colors.text.text.primary,
        iconColor = NativeTheme.colors.system.attention,
    )

    @Composable
    fun Badge() {
        Icon(Icons.Regular.Info, contentDescription = null)
    }

    @Composable
    fun CloseActionButton(onClick: () -> Unit) {
        SubtleButton(iconOnly = true, onClick = onClick) {
            Icon(Icons.Regular.Dismiss, contentDescription = null)
        }
    }
}

@Composable
fun CardExpanderItem(
    heading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = {},
    caption: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    CardExpanderItemImpl(heading, modifier, icon, caption, trailing)
}

@Composable
fun TopNav(
    expanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: TopNavScope.() -> Unit,
) {
    TopNavImpl(expanded, onExpandedChanged, modifier, content)
}

@Composable
fun TopNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
) {
    TopNavItemImpl(selected, onClick, modifier, text, icon, badge)
}

@Composable
fun MenuFlyoutContainer(
    flyout: @Composable MenuFlyoutContainerScope.() -> Unit,
    modifier: Modifier = Modifier,
    initialVisible: Boolean = false,
    placement: FlyoutPlacement = FlyoutPlacement.Auto,
    adaptivePlacement: Boolean = false,
    content: @Composable MenuFlyoutContainerScope.() -> Unit,
) {
    MenuFlyoutContainerImpl(flyout, modifier, initialVisible, placement, adaptivePlacement, content)
}

@Composable
fun MenuFlyoutItem(
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
) {
    MenuFlyoutItemImpl(
        onClick,
        text,
        modifier,
        icon,
        selected = null,
        onSelectedChanged = null,
        selectionType = ListItemSelectionType.Standard,
    )
}

@Composable
fun MenuFlyoutItem(
    selected: Boolean,
    onSelectedChanged: (Boolean) -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    selectionType: ListItemSelectionType = ListItemSelectionType.Standard,
) {
    MenuFlyoutItemImpl(
        onClick = { onSelectedChanged(!selected) },
        text = text,
        modifier = modifier,
        icon = icon,
        selected = selected,
        onSelectedChanged = onSelectedChanged,
        selectionType = selectionType,
    )
}

@Composable
fun MenuFlyoutSeparator(modifier: Modifier = Modifier) {
    MenuFlyoutSeparatorImpl(modifier)
}

@Composable
fun SegmentedControl(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    SegmentedControlImpl(modifier, content)
}

@Composable
fun SegmentedButton(
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    position: SegmentedItemPosition = SegmentedItemPosition.Center,
    icon: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    SegmentedButtonImpl(checked, onCheckedChanged, modifier, position, icon, text)
}

@Suppress("UNUSED_PARAMETER")
object ListItemDefaults {
    @Composable
    fun defaultListItemColors(): Any = Unit
}
