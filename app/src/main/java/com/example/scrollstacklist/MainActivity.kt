package com.example.scrollstacklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.scrollstacklist.ui.theme.ScrollStackListTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ScrollStackListTheme {
                // A surface container using the 'background' color from the theme
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color.Blue),
                ) {
                    val dummyList = dummyList
                    val composeHeight = 50.dp
                    val spacerHeight = 4.dp

                    if (dummyList.size != 0) {
                        ScrollStackList(
                            modifier = Modifier
                                .padding(bottom = 100.dp)
                                .align(BottomCenter),
                            stackItems = dummyList.toImmutableList(),
                            itemContent = { child, color ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(composeHeight)
                                        .padding(horizontal = 10.dp)
                                        .background(color = color, shape = RoundedCornerShape(10.dp))
                                        .clip(RoundedCornerShape(10.dp)),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Text(text = child.item)
                                }
                            },
                            topStackModifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth(),
                            childStackModifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth(),
                            moreItemContent = {
                                MoreBar(
                                    modifier = Modifier
                                        .fillMaxSize(0.99f)
                                        .padding(horizontal = 10.dp)
                                        .padding(top = 3.dp)
                                        .background(
                                            color = Color.White.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                        .clip(RoundedCornerShape(10.dp))
                                        .height(composeHeight),
                                )
                            },
                            spacerHeight = spacerHeight,
                            customItemBackgroundColor = Color.White,

                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T>ScrollStackList(
    modifier: Modifier = Modifier,
    stackItems: ImmutableList<Group<T>>,
    itemContent: @Composable (Child<T>, Color) -> Unit,
    moreItemContent: @Composable () -> Unit,
    topStackModifier: Modifier = Modifier,
    childStackModifier: Modifier = Modifier,
    spacerHeight: Dp,
    customItemBackgroundColor: Color,
) {
    val expandableState = remember {
        mutableStateMapOf<String, Boolean>()
    }

    val clickableState = remember {
        mutableStateMapOf<String, Boolean>()
    }

    var isExpandedColumn by remember { mutableStateOf(ExpandState.NONE) }

    val scrollState = rememberLazyListState()

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = with(density) {
        configuration.screenHeightDp.dp.roundToPx()
    }
    val threshold = screenHeight.times(0.8f)
    println(threshold)

    LaunchedEffect(
        key1 = stackItems,
    ) {
        stackItems.forEach {
            launch(Dispatchers.Default) {
                updateExpandable(expandableState, it.key)
            }
            launch(Dispatchers.Default) {
                updateClickable(clickableState, it.key, it.child.size >= 2)
            }
        }
    }

    when (isExpandedColumn) {
        ExpandState.NONE -> {
            NotificationSizeText(modifier, size = stackItems.size) {
                isExpandedColumn = ExpandState.SMALL
            }
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(
                            BottomCenter,
                        ),
                ) {
                    LazyColumn(
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(spacerHeight),
                        contentPadding = PaddingValues(bottom = 200.dp),
                    ) {
                        stackItems.forEachIndexed { index, content ->
                            item(key = index) {
                                var animateFlag by rememberSaveable { mutableStateOf(true) }
                                val animateScale by animateFloatAsState(targetValue = if (animateFlag) { 0f } else { 1f }, label = "")
                                val animateOffset by animateDpAsState(targetValue = if (animateFlag) { (-100).dp } else { 0.dp }, label = "")
                                val animateZIndex by animateFloatAsState(targetValue = if (animateFlag) { 0f } else { 1f }, label = "")
                                val animateAlpha by animateFloatAsState(targetValue = if (animateFlag) { 0f } else { 1f }, label = "")
                                var alphaState by rememberSaveable { mutableStateOf(0f) }
                                val animateColor by animateColorAsState(
                                    targetValue = if (animateFlag) { Color.Unspecified } else { Color.White },
                                    label = "",
                                )

                                LaunchedEffect(scrollState) {
                                    snapshotFlow {
                                        scrollState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == index }?.offset ?: 0
                                    }.collectLatest { offset ->

                                        alphaState = if (offset > threshold) 0f else 1f
                                        animateFlag = (offset > threshold)
                                    }
                                }

                                LazyColumnItem(
                                    modifier = topStackModifier
                                        .animateItemPlacement()
                                        .graphicsLayer { alpha = alphaState }
                                        .alpha(animateAlpha)
                                        .offset(y = animateOffset)
                                        .scale(animateScale)
                                        .zIndex(animateZIndex),
                                    content = content,
                                    expandableState = expandableState,
                                    itemContent = itemContent,
                                    moreItemContent = moreItemContent,
                                    backgroundColor = animateColor,
                                )
                            }

                            if (expandableState.containsKey(content.key) && expandableState[content.key] == true && content.child.size != 1) {
                                itemsIndexed(items = content.child.drop(1), key = { childIndex, it -> it.key }) { childIndex, child ->

                                    var animateFlag by rememberSaveable { mutableStateOf(true) }
                                    val animateScale by animateFloatAsState(targetValue = if (animateFlag) { 0f } else { 1f }, label = "")
                                    val animateOffset by animateDpAsState(targetValue = if (animateFlag) { (-100).dp } else { 0.dp }, label = "")
                                    val animateZIndex by animateFloatAsState(targetValue = if (animateFlag) { 0f } else { 1f }, label = "")
                                    val animateAlpha by animateFloatAsState(targetValue = if (animateFlag) { 0f } else { 1f }, label = "")
                                    var alphaState by rememberSaveable {
                                        mutableStateOf(0f)
                                    }
                                    val animateColor by animateColorAsState(
                                        targetValue = if (animateFlag) { Color.Transparent } else { Color.White },
                                        label = "",
                                    )

                                    LaunchedEffect(scrollState) {
                                        snapshotFlow {
                                            scrollState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == child.key }?.offset ?: 0
                                        }.collectLatest { offset ->

                                            alphaState = if (offset > threshold) 0f else 1f
                                            animateFlag = (offset > threshold)
                                        }
                                    }

                                    Box(
                                        modifier = childStackModifier
                                            .graphicsLayer { alpha = alphaState }
                                            .alpha(animateAlpha)
                                            .offset(y = animateOffset)
                                            .scale(animateScale)
                                            .heightIn(min = 0.dp)
                                            .zIndex(animateZIndex),

                                    ) {
                                        itemContent(child, animateColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun <T>LazyColumnItem(
    modifier: Modifier,
    content: Group<T>,
    backgroundColor: Color,
    expandableState: SnapshotStateMap<String, Boolean>,
    itemContent: @Composable (Child<T>, Color) -> Unit,
    moreItemContent: @Composable () -> Unit,

) {
    Box(
        modifier = modifier.clickable {
            if (expandableState.containsKey(content.key)) {
                expandableState[content.key]?.let { flag ->
                    expandableState[content.key] = !flag
                }
            }
        },
    ) {
        if (expandableState.containsKey(content.key) && expandableState[content.key] == false && content.child.size != 1) {
            Row(
                horizontalArrangement = Arrangement.Center,
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                if (content.child.size >= 2) {
                    moreItemContent()
                }
            }
        }
        itemContent(content.child[0], backgroundColor)
    }
}

@Composable
fun MoreBar(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,

    ) {}
}

@Composable
fun NotificationSizeText(
    modifier: Modifier = Modifier,
    size: Int,
    updateExpandableColumn: (ExpandState) -> Unit,
) {
    Text(
        modifier = modifier.scrollable(
            orientation = Orientation.Vertical,
            state = rememberScrollableState { delta ->
                if (delta < -20f) {
                    updateExpandableColumn(ExpandState.SMALL)
                }
                delta
            },
        ),
        text = "알림이 ${size}개 있어요",
    )
}

fun updateExpandable(expandableState: SnapshotStateMap<String, Boolean>, key: String) {
    if (!expandableState.containsKey(key)) {
        expandableState[key] = false
    }
}
fun updateClickable(clickableState: SnapshotStateMap<String, Boolean>, key: String, flag: Boolean) {
    clickableState[key] = flag
}
fun updateOffsetState(offsetState: SnapshotStateMap<String, Int>, key: String, offset: Int) {
    offsetState[key] = offset
}

data class DpState(var value: Dp)
object DpStateSaver : Saver<MutableState<DpState>, Float> {
    override fun restore(value: Float): MutableState<DpState>? {
        return mutableStateOf(DpState(value.dp))
    }

    override fun SaverScope.save(value: MutableState<DpState>): Float? {
        return value.value.value.value
    }
}
val dummyList: MutableList<Group<String>> = mutableListOf<Group<String>>().apply {
    (1..100).forEach {
        add(
            Group(
                key = "$it",
                child = mutableListOf<Child<String>>().apply {
                    (1..10).forEach { key ->
                        add(
                            Child(
                                key = "$it-$key",
                                item = "Item $it-$key",
                            ),
                        )
                    }
                }.toImmutableList(),
            ),
        )
    }
}

enum class ExpandState {
    NONE, SMALL, FULL
}

@Preview
@Composable
fun TestPreview() {
    MaterialTheme {
    }
}

data class Group<T>(
    val key: String,
    val child: ImmutableList<Child<T>>,
)

data class Child<T>(
    val key: String,
    val item: T,
)
