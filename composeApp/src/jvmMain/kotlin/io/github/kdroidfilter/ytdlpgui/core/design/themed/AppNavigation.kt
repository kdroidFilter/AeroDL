package io.github.kdroidfilter.ytdlpgui.core.design.themed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.composefluent.ExperimentalFluentApi
import io.github.kdroidfilter.ytdlpgui.core.config.AppTheme
import io.github.kdroidfilter.ytdlpgui.core.config.LocalAppTheme

@OptIn(ExperimentalFluentApi::class)
@Composable
fun AppTopNav(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                content()
            }
        }
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.Tabs(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            modifier = modifier,
        ) {
            io.github.kdroidfilter.darwinui.components.TabsList {
                content()
            }
        }
    }
}

@OptIn(ExperimentalFluentApi::class)
@Composable
fun AppTopNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    value: String = "",
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (LocalAppTheme.current) {
        AppTheme.FLUENT -> io.github.composefluent.component.TopNavItem(
            selected = selected,
            onClick = { onClick() },
            modifier = modifier,
            text = content,
        )
        AppTheme.DARWIN -> io.github.kdroidfilter.darwinui.components.TabsTrigger(
            value = value,
            modifier = modifier,
            content = content,
        )
    }
}
