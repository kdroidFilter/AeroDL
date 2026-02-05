package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

@Composable
fun AppSegmentedControl(
    selectedIndex: Int = -1,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.SegmentedControl(
            modifier = modifier,
        ) {
            content()
        }
        AppTheme.DARWIN -> {
            io.github.kdroidfilter.darwinui.components.Tabs(
                selectedTab = selectedIndex.toString(),
                onTabSelected = { },
                modifier = modifier,
            ) {
                io.github.kdroidfilter.darwinui.components.TabsList {
                    content()
                }
            }
        }
    }
}

@Composable
fun AppSegmentedButton(
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.SegmentedButton(
            checked = selected,
            onCheckedChanged = { if (it) onClick() },
            position = io.github.composefluent.component.SegmentedItemPosition.Center,
            modifier = modifier,
            text = content,
        )
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.TabsTrigger(
            value = index.toString(),
            modifier = modifier,
            content = content,
        )
    }
}
