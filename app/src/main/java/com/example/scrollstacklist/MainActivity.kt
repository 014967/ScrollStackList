package com.example.scrollstacklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
                            moreItemContent = {
                                MoreBar(
                                    modifier = Modifier
                                        .fillMaxSize(0.99f)
                                        .padding(horizontal = 10.dp)
                                        .padding(top = 3.dp)
                                        .background(color = Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(10.dp))
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
    itemContent: @Composable (Child<T>) -> Unit,
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

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val threshold = screenHeight.times(0.8f)

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

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = modifier.graphicsLayer {
                alpha = 0.99f
            }.drawWithContent {
                val brush = Brush.verticalGradient(
                    listOf(
                        Color.Transparent.copy(alpha = 1f),
                        Color.Transparent.copy(alpha = 1f),
                        Color.Transparent.copy(alpha = 1f),
                        Color.Transparent.copy(alpha = 1f),
                        Color.Transparent.copy(alpha = 0f),
                    ),
                )
                drawContent()
                drawRect(
                    brush = brush,
                    blendMode = BlendMode.DstIn,
                )
            },
            state = scrollState,
            verticalArrangement = Arrangement.spacedBy(spacerHeight),

        ) {
            stackItems.forEachIndexed { index, content ->
                item(key = index) {
                    val density = LocalDensity.current
                    var animateFlag by rememberSaveable { mutableStateOf(true) }
                    val animateScale by animateFloatAsState(targetValue = if (animateFlag) { 0f } else { 1f }, label = "")
                    val animateOffset by animateDpAsState(targetValue = if (animateFlag) { (-100).dp } else { 0.dp }, label = "")
                    val animateZIndex by animateFloatAsState(targetValue = if (animateFlag) { 0f } else { 1f }, label = "")
                    val animateAlpha by animateFloatAsState(targetValue = if (animateFlag) { 0f } else { 1f }, label = "")
                    val animateBackground by animateColorAsState(targetValue = if (animateFlag) Color.Transparent else customItemBackgroundColor, label = "")
                    var itemAppeared by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(!animateFlag) {
                        if (!animateFlag) {
                            itemAppeared = true
                        }
                    }

                    LaunchedEffect(scrollState) {
                        snapshotFlow {
                            scrollState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == index }?.offset ?: 0
                        }.collectLatest { offset ->
                            if (offset == Integer.MAX_VALUE) {
                                animateFlag = true
                            } else {
                                val offsetInDp = with(density) { offset.toDp() }
                                animateFlag = offsetInDp > threshold
                            }
                        }
                    }

                    LazyColumnItem(
                        modifier = topStackModifier.animateItemPlacement().alpha(animateAlpha).offset(y = animateOffset).scale(animateScale).zIndex(animateZIndex),
                        content = content,
                        itemAppeared = itemAppeared,
                        expandableState = expandableState,
                        itemContent = itemContent,
                        moreItemContent = moreItemContent,

                    )
                }

                if (expandableState.containsKey(content.key) && expandableState[content.key] == true && content.child.size != 1) {
                    itemsIndexed(items = content.child.drop(1), key = { childIndex, it -> it.key }) { childIndex, child ->

                        val density = LocalDensity.current
                        var animateFlag by rememberSaveable { mutableStateOf(false) }
                        val animateScale by animateFloatAsState(targetValue = if (animateFlag) { 0f } else { 1f }, label = "")
                        val animateOffset by animateDpAsState(targetValue = if (animateFlag) { (-100).dp } else { 0.dp }, label = "")
                        val animateZIndex by animateFloatAsState(targetValue = if (animateFlag) { 0f } else { 1f }, label = "")
                        val animateAlpha by animateFloatAsState(targetValue = if (animateFlag) { 0f } else { 1f }, label = "")
                        var itemAppeared by rememberSaveable { mutableStateOf(false) }

                        LaunchedEffect(Unit) {
                            itemAppeared = true
                        }

                        LaunchedEffect(scrollState) {
                            snapshotFlow {
                                scrollState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == child.key }?.offset ?: 0
                            }.collectLatest { offset ->
                                if (offset == Integer.MAX_VALUE) {
                                    animateFlag = true
                                } else {
                                    val offsetInDp = with(density) { offset.toDp() }
                                    animateFlag = offsetInDp > threshold
                                }
                            }
                        }

                        Box(
                            modifier = childStackModifier.animateItemPlacement().alpha(animateAlpha).offset(y = animateOffset).scale(animateScale).zIndex(animateZIndex),
                        ) {
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
    moreItemContent: @Composable () -> Unit,

) {
    Column(
        modifier = modifier.clickable {
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
            enter = fadeIn(),
        ) {
            if (expandableState.containsKey(content.key) && expandableState[content.key] == false && content.child.size != 1) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                ) {
                    var barAppeared by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        barAppeared = true
                    }

                    AnimatedVisibility(
                        visible = barAppeared,
                        modifier = Modifier.fillMaxWidth(),
                        enter = slideIn(
                            initialOffset = { IntOffset(0, (it.height / 2)) },
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                visibilityThreshold = IntOffset.VisibilityThreshold,
                            ),
                        ),
                        exit = slideOut(
                            targetOffset = { IntOffset(0, it.height / 2) },
                        ),
                    ) {
                        if (content.child.size >= 2) {
                            moreItemContent()
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
