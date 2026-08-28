package io.github.kdroidfilter.ytdlpgui.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class BadgeStatus {
    Informational,
    InformationalSafe,
    Caution,
    Attention,
    Success,
    Critical,
}

class DialogSize(val min: Dp, val max: Dp) {
    companion object {
        val Max = DialogSize(540.dp, 540.dp)
        val Standard = DialogSize(448.dp, 448.dp)
        val Min = DialogSize(320.dp, 320.dp)
    }
}

enum class ContentDialogButton { Primary, Secondary, Close }

enum class FlyoutPlacement { Auto, BottomAlignedEnd }

enum class SegmentedItemPosition { Start, Center, End }

enum class ListItemSelectionType { Standard, Check }

class InfoBarColors(
    val backgroundColor: Color,
    val contentColor: Color,
    val iconColor: Color,
)

interface TopNavScope {
    fun item(content: @Composable () -> Unit)
    fun items(count: Int, itemContent: @Composable (index: Int) -> Unit)
}

interface MenuFlyoutContainerScope {
    var isFlyoutVisible: Boolean
}

class NativeScrollbarAdapter internal constructor(
    internal val scrollState: ScrollState? = null,
    internal val lazyListState: LazyListState? = null,
)

internal class MenuFlyoutState(initialVisible: Boolean = false) : MenuFlyoutContainerScope {
    override var isFlyoutVisible by mutableStateOf(initialVisible)
}

internal class CollectedTopNavScope : TopNavScope {
    val items = mutableListOf<@Composable () -> Unit>()

    override fun item(content: @Composable () -> Unit) {
        items += content
    }

    override fun items(count: Int, itemContent: @Composable (index: Int) -> Unit) {
        repeat(count) { index ->
            items += { itemContent(index) }
        }
    }
}
