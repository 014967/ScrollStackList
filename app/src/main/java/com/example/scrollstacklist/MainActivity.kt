package com.example.scrollstacklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.scrollstacklist.ui.theme.ScrollStackListTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ScrollStackListTheme {
                // A surface container using the 'background' color from the theme
                Box(
                    modifier = Modifier.fillMaxSize().background(color = Color.Blue),
                ) {
                    val dummyList = dummyList
                    val composeHeight = 50.dp
                    val moreBarPadding = 5.dp
                    val spacerHeight = 4.dp
                    if (dummyList.size != 0) {
                        ScrollStackList(
                            modifier = Modifier
                                .padding(bottom = 30.dp).align(BottomCenter),
                            stackItems = dummyList.toImmutableList(),
                            itemContent = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(composeHeight)
                                        .padding(horizontal = 10.dp)
                                        .background(
                                            color = Color.White,
                                            shape = RoundedCornerShape(10.dp),
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Text(text = it.item)
                                }
                            },
                            topStackModifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth(),
                            childStackModifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth(),
                            itemHeight = composeHeight,
                            moreBarPadding = moreBarPadding,
                            spacerHeight = spacerHeight,

                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun <T>ScrollStackList(
    modifier: Modifier = Modifier,
    stackItems: ImmutableList<Group<T>>,
    itemContent: @Composable (Child<T>) -> Unit,
    topStackModifier: Modifier = Modifier,
    childStackModifier: Modifier = Modifier,
    itemHeight: Dp,
    spacerHeight: Dp,
    moreBarPadding: Dp,
) {
    val expandableState = remember {
        mutableStateMapOf<String, Boolean>()
    }

    val clickableState = remember {
        mutableStateMapOf<String, Boolean>()
    }

    var isExpandedColumn by remember { mutableStateOf(ExpandState.NONE) }

    val scrollState = rememberLazyListState()

    var columnHeightDp by remember {
        mutableStateOf(0.dp)
    }

//    var itemCount by remember {
//        mutableStateOf(0)
//    }

    val lastVisibleItemIndex by remember {
        derivedStateOf {
            scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }
    }

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

//    LaunchedEffect(key1 = columnHeightDp) {
//        // 나누기는 Item 높이는 전체 /  ( 아이템 정했던 크기 + moreBar + Spacer)
//        itemCount = (columnHeightDp / (itemHeight + moreBarPadding + spacerHeight)).toInt()
//        // TODO 만약 아이템이 열린다면 itemCount다시 계산해야함.
//    }

    var dragOffset by remember { mutableStateOf(0f) }

    val debounceState = remember {
        MutableSharedFlow<() -> Unit>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    }

    LaunchedEffect(Unit) {
        debounceState
            .debounce(300L)
            .collect { onClick ->
                when (isExpandedColumn) {
                    ExpandState.NONE -> {
                        isExpandedColumn = ExpandState.SMALL
                    }
                    ExpandState.SMALL -> {
                        isExpandedColumn = ExpandState.FULL
                    }
                    ExpandState.FULL -> {
                    }
                }
            }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
//            .scrollable(
//                orientation = Orientation.Vertical,
//                state = rememberScrollableState { delta ->
//                    dragOffset += delta
//                    if (dragOffset < -150) {
//                        debounceState.tryEmit { }
//                    }
//                    dragOffset
//                },
//            ),

    ) {
        LazyColumn(
            modifier = modifier,
            state = scrollState,
        ) {
            stackItems.forEachIndexed { index, content ->
                item(key = index) {
                    var itemAppeared by rememberSaveable(index.toString()) { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        itemAppeared = true
                    }

                    val animateOffset by animateDpAsState(
                        targetValue = if (lastVisibleItemIndex > (index + 1)) 0.dp else if (lastVisibleItemIndex == index + 1) (-50).dp else (-100).dp,
                        label = "",
                    )

                    val animateScale by animateFloatAsState(
                        targetValue = if (lastVisibleItemIndex > (index + 1)) 1f else if (lastVisibleItemIndex == index + 1) 0.8f else 0f,
                        animationSpec = tween(durationMillis = 500),
                        label = "",
                    )
                    val animateZIndex by animateFloatAsState(
                        targetValue = if (lastVisibleItemIndex > (index + 1)) 1f else if (lastVisibleItemIndex == index + 1) 0.8f else 0f,
                        label = "",
                    )

                    LazyColumnItem(
                        modifier = topStackModifier.scale(animateScale).offset(y = animateOffset).zIndex(animateZIndex).alpha(animateZIndex),
                        itemAppeared = itemAppeared,
                        content = content,
                        expandableState = expandableState,
                        itemContent = itemContent,
                        itemHeight = itemHeight,
                        moreBarPadding = moreBarPadding,
                        isExpandedColumn = isExpandedColumn,
                        updateExpandableColumn = {
                            isExpandedColumn = it
                        },

                    )

                    Spacer(modifier = Modifier.height(if (lastVisibleItemIndex <= index) { 0.dp } else { spacerHeight }))
                }

                if (expandableState.containsKey(content.key) && expandableState[content.key] == true && content.child.size != 1) {
                    itemsIndexed(items = content.child.drop(1), key = { index, it -> it.key }) { index, child ->
                        Box(
                            modifier = childStackModifier.animateItemPlacement(),
                        ) {
                            LaunchedEffect(Unit) {
                                println("아이템 로그 index $index")
                            }
                            itemContent(child)
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
    itemAppeared: Boolean,
    content: Group<T>,
    expandableState: SnapshotStateMap<String, Boolean>,
    itemContent: @Composable (Child<T>) -> Unit,
    moreBarPadding: Dp,
    itemHeight: Dp,
    isExpandedColumn: ExpandState,
    updateExpandableColumn: (ExpandState) -> Unit,

) {
    Column(
        modifier = modifier.clickable {
//            when (isExpandedColumn) {
//                ExpandState.FULL -> {
//                }
//                ExpandState.SMALL -> {
//                    updateExpandableColumn(ExpandState.FULL)
//                    return@clickable
//                }
//                ExpandState.NONE -> {
//                    updateExpandableColumn(ExpandState.SMALL)
//                    return@clickable
//                }
//            }
            if (expandableState.containsKey(content.key)) {
                expandableState[content.key]?.let { flag ->
                    expandableState[content.key] = !flag
                }
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {
        AnimatedVisibility(
            visible = itemAppeared,
        ) {
            if (expandableState.containsKey(content.key) && expandableState[content.key] == false && content.child.size != 1) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                ) {
                    var barAppeared by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
//                        println("로그 LauncehdEffect ${content.key}")
                        barAppeared = true
                    }

                    AnimatedVisibility(
                        visible = barAppeared,
                        modifier = Modifier.fillMaxWidth(),
                        enter = slideInVertically(),
                    ) {
                        if (content.child.size == 2) {
                            MoreBar(
                                modifier = modifier
                                    .fillMaxSize(0.9f)
                                    .padding(top = moreBarPadding)
                                    .height(itemHeight),
                            )
                        } else {
                            if (content.child.size > 2) {
                                MoreBar(
                                    modifier = modifier
                                        .fillMaxSize(0.8f)
                                        .padding(top = moreBarPadding * 2)
                                        .height(itemHeight),
                                )
                                MoreBar(
                                    modifier = modifier
                                        .fillMaxSize(0.9f)
                                        .padding(top = moreBarPadding)
                                        .height(itemHeight),
                                )
                            }
                        }
                    }
                }
            }
            itemContent(content.child[0])
        }
    }
}

@Composable
fun MoreBar(
    modifier: Modifier = Modifier,
) {
    val moreNotificationShape = RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp)
    Row(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFFFAFAFA).copy(alpha = 0.9f),
                        Color.LightGray,
                    ),
                ),
                shape = moreNotificationShape,
            )
            .clip(shape = moreNotificationShape),
    ) {}
}

@Composable
fun NotificationSizeText(
    modifier: Modifier = Modifier,
    size: Int,
    updateExpandableColumn: (ExpandState) -> Unit,
) {
    Text(
        modifier = modifier.clickable {
            updateExpandableColumn(ExpandState.SMALL)
        },
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

val dummyList: MutableList<Group<String>> = mutableListOf<Group<String>>().apply {
    (1..100).forEach {
        add(
            Group(
                key = "$it",
                child = mutableListOf<Child<String>>().apply {
                    (1..100).forEach { key ->
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
