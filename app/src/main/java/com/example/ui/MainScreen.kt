package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.ui.BiasAlignment
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricHelper
import com.example.util.ClipboardHelper
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.ui.zIndex
import android.content.ClipData
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.MediaItem
import com.example.viewmodel.GalleryUiState
import com.example.viewmodel.GalleryViewModel
import androidx.activity.compose.BackHandler

sealed class GridItem {
    data class Header(val date: String) : GridItem()
    data class Photo(val mediaItem: MediaItem) : GridItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: GalleryViewModel,
    onMediaClick: (MediaItem) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val rawMedia by viewModel.rawMediaItems.collectAsState()
    val fancyBlur by viewModel.fancyBlur.collectAsState()
    val liquidGlassMode by viewModel.liquidGlassMode.collectAsState()

    var isBottomBarVisible by remember { mutableStateOf(true) }
    var activeSubScreen by remember { mutableStateOf<String?>(null) }
    var activeCategoryTitle by remember { mutableStateOf<String?>(null) }
    var activeCategoryItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    BackHandler(enabled = activeSubScreen != null || activeCategoryTitle != null || selectedTab != 0) {
        if (activeCategoryTitle != null) {
            activeCategoryTitle = null
            activeCategoryItems = emptyList()
        } else if (activeSubScreen != null) {
            activeSubScreen = null
        } else if (selectedTab != 0) {
            viewModel.setTab(0)
        }
    }

    val context = LocalContext.current
    val vaultTriggerShortcut by viewModel.vaultTriggerShortcut.collectAsState()
    val useBiometricsForVault by viewModel.useBiometricsForVault.collectAsState()

    val triggerVaultBiometrics: () -> Unit = {
        val activity = context as? FragmentActivity
        if (activity != null && useBiometricsForVault) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = "Access Secure Vault",
                subtitle = "Verify fingerprint or secure credentials to open the Prism Vault",
                onSuccess = {
                    activeSubScreen = "vault"
                },
                onError = { err ->
                    android.widget.Toast.makeText(context, "Authentication failed. Enter secure PIN.", android.widget.Toast.LENGTH_SHORT).show()
                    activeSubScreen = "vault"
                }
            )
        } else {
            activeSubScreen = "vault"
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -10f) {
                    isBottomBarVisible = false
                } else if (delta > 10f) {
                    isBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Immersive Top Blur Scrim (Status Bar & Search Bar Background)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .glassyBlur(enabled = fancyBlur, radius = 25f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = if (fancyBlur) 0.85f else 1f),
                                MaterialTheme.colorScheme.surface.copy(alpha = if (fancyBlur) 0.40f else 0.80f),
                                Color.Transparent
                            )
                        )
                    )
                    .zIndex(5f)
            )

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn())
                            .togetherWith(slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeOut())
                    } else {
                        (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeIn())
                            .togetherWith(slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeOut())
                    }
                },
                label = "tab_transition",
                modifier = Modifier.fillMaxSize()
            ) { targetTab ->
                when (targetTab) {
                    0 -> PhotosTab(viewModel, onMediaClick, onSettingsClick)
                    1 -> AlbumsTab(
                        viewModel = viewModel,
                        onMediaClick = onMediaClick,
                        onCategoryClick = { title, items ->
                            activeCategoryTitle = title
                            activeCategoryItems = items
                        },
                        onOpenVault = { activeSubScreen = "vault" },
                        onOpenTrash = { activeSubScreen = "trash" }
                    )
                    2 -> SearchTab(viewModel, onMediaClick)
                }
            }

            // Immersive Bottom Blur Scrim
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(112.dp)
                    .glassyBlur(enabled = fancyBlur, radius = 25f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = if (fancyBlur) 0.40f else 0.80f),
                                MaterialTheme.colorScheme.surface.copy(alpha = if (fancyBlur) 0.85f else 1f)
                            )
                        )
                    )
                    .zIndex(5f)
            )

            // Centralized Floating Bottom Pill Navigation Bar (Global UI)
            val bottomBarOffset by animateDpAsState(
                targetValue = if (isBottomBarVisible && activeSubScreen == null) 0.dp else 120.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "nav_offset"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = bottomBarOffset)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 24.dp)
                    .zIndex(10f)
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = if (liquidGlassMode) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.10f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    },
                    tonalElevation = if (liquidGlassMode) 0.dp else 6.dp,
                    border = BorderStroke(
                        width = if (liquidGlassMode) 1.5.dp else 1.dp,
                        color = if (liquidGlassMode) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .shadow(if (liquidGlassMode) 4.dp else 12.dp, RoundedCornerShape(32.dp))
                        .testTag("bottom_nav")
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        if (liquidGlassMode) {
                            // Active runtime gradient overlay mimicking light reflection
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.15f),
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.05f)
                                            )
                                        )
                                    )
                            )
                        }

                        val tabWidth = maxWidth / 3

                        // Sliding Highlight Pill
                        val targetOffset by remember(selectedTab, tabWidth) {
                            derivedStateOf {
                                when (selectedTab) {
                                    0 -> 0.dp
                                    1 -> tabWidth
                                    else -> tabWidth * 2
                                }
                            }
                        }

                        val indicatorOffset by animateDpAsState(
                            targetValue = targetOffset,
                            animationSpec = spring(
                                dampingRatio = if (liquidGlassMode) Spring.DampingRatioMediumBouncy else Spring.DampingRatioLowBouncy,
                                stiffness = if (liquidGlassMode) Spring.StiffnessLow else Spring.StiffnessMediumLow
                            ),
                            label = "nav_highlight_offset"
                        )

                        // Stretch factor based on displacement to mimic a liquid droplet
                        val displacement = if (tabWidth.value > 0f) {
                            Math.abs((indicatorOffset - targetOffset).value) / tabWidth.value
                        } else {
                            0f
                        }

                        val stretchWidth = if (liquidGlassMode) {
                            tabWidth * (1f + displacement * 0.4f)
                        } else {
                            tabWidth
                        }

                        val cornerRadius = if (liquidGlassMode) {
                            (24f - displacement * 10f).coerceIn(12f, 24f).dp
                        } else {
                            24.dp
                        }

                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset - if (liquidGlassMode) (stretchWidth - tabWidth) / 2 else 0.dp)
                                .fillMaxHeight()
                                .width(stretchWidth)
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(cornerRadius))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        )

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FloatingNavTabItem(
                                selected = selectedTab == 0,
                                onClick = { viewModel.setTab(0) },
                                icon = if (selectedTab == 0) Icons.Filled.Photo else Icons.Outlined.Photo,
                                label = "Photos",
                                tag = "tab_photos"
                            )
                            FloatingNavTabItem(
                                selected = selectedTab == 1,
                                onClick = { viewModel.setTab(1) },
                                onDoubleClick = {
                                    if (vaultTriggerShortcut == "double_tap" && selectedTab == 1) {
                                        triggerVaultBiometrics()
                                    }
                                },
                                onLongClick = {
                                    if (vaultTriggerShortcut == "long_press") {
                                        triggerVaultBiometrics()
                                    }
                                },
                                icon = if (selectedTab == 1) Icons.Filled.Folder else Icons.Outlined.Folder,
                                label = "Albums",
                                tag = "tab_albums"
                            )
                            FloatingNavTabItem(
                                selected = selectedTab == 2,
                                onClick = { viewModel.setTab(2) },
                                icon = if (selectedTab == 2) Icons.Filled.Search else Icons.Outlined.Search,
                                label = "Search",
                                tag = "tab_search"
                            )
                        }
                    }
                }
            }

            // Secure Vault Overlay screen
            AnimatedVisibility(
                visible = activeSubScreen == "vault",
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                VaultScreen(viewModel, onBack = { activeSubScreen = null })
            }

            // Recycle Bin (Trash) Overlay screen
            AnimatedVisibility(
                visible = activeSubScreen == "trash",
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                TrashScreen(viewModel, onBack = { activeSubScreen = null })
            }

            // Category / Album Detail Grid Overlay Screen
            AnimatedVisibility(
                visible = activeCategoryTitle != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                activeCategoryTitle?.let { title ->
                    CategoryGalleryScreen(
                        title = title,
                        items = activeCategoryItems,
                        viewModel = viewModel,
                        onMediaClick = onMediaClick,
                        onBack = {
                            activeCategoryTitle = null
                            activeCategoryItems = emptyList()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.FloatingNavTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tab_color"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tab_scale"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
                onLongClick = onLongClick
            )
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun PhotosTab(
    viewModel: GalleryViewModel,
    onMediaClick: (MediaItem) -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val columnCount by viewModel.columnCount.collectAsState()
    val gridMode by viewModel.gridMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val fancyBlur by viewModel.fancyBlur.collectAsState()
    val liquidGlassMode by viewModel.liquidGlassMode.collectAsState()
    var showProfileDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is GalleryUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is GalleryUiState.Success -> {
                if (state.allMedia.isEmpty()) {
                    EmptyStateView(
                        title = if (searchQuery.isNotEmpty()) "No results found" else "No photos or videos yet",
                        subtitle = if (searchQuery.isNotEmpty()) "Try searching for a different keyword" else "Photos and videos on your device will show up here"
                    )
                } else {
                    PhotosGrid(
                        groupedMedia = state.groupedMedia,
                        columnCount = columnCount,
                        gridMode = gridMode,
                        onMediaClick = onMediaClick,
                        onColumnCountChange = { viewModel.setColumnCount(it) },
                        viewModel = viewModel
                    )
                }
            }
            is GalleryUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Floating Search Bar stays on top
        FloatingSearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onProfileClick = { showProfileDialog = true },
            fancyBlur = fancyBlur,
            liquidGlassMode = liquidGlassMode,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
        )

        if (showProfileDialog) {
            ProfileDialog(
                onDismiss = { showProfileDialog = false },
                onSettingsClick = onSettingsClick
            )
        }
    }
}

@Composable
fun shimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): Brush {
    return if (showShimmer) {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_translate"
        )
        
        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f)
        )
        
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation, y = translateAnimation)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosGrid(
    groupedMedia: Map<String, List<MediaItem>>,
    columnCount: Int,
    gridMode: String,
    onMediaClick: (MediaItem) -> Unit,
    onColumnCountChange: (Int) -> Unit,
    viewModel: GalleryViewModel
) {
    val lazyListState = rememberLazyListState()
    var scaleReference by remember { mutableStateOf(1f) }
    
    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(top = 110.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("photos_grid")
            .pointerInput(columnCount) {
                detectTransformGestures { _, _, zoom, _ ->
                    scaleReference *= zoom
                    if (scaleReference > 1.35f) {
                        if (columnCount > 1) {
                            onColumnCountChange(columnCount - 1)
                        }
                        scaleReference = 1f
                    } else if (scaleReference < 0.65f) {
                        if (columnCount < 6) {
                            onColumnCountChange(columnCount + 1)
                        }
                        scaleReference = 1f
                    }
                }
            }
    ) {
        groupedMedia.forEach { (date, items) ->
            stickyHeader(key = "header_$date") {
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
            
            val chunkedItems = items.chunked(columnCount)
            itemsIndexed(
                items = chunkedItems,
                key = { rowIndex, rowItems -> "row_${date}_${rowIndex}_${rowItems.firstOrNull()?.id ?: 0}" }
            ) { rowIndex, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (item in rowItems) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            PhotoGridItem(
                                mediaItem = item,
                                onClick = { onMediaClick(item) },
                                viewModel = viewModel,
                                gridMode = gridMode,
                                index = rowIndex * columnCount + rowItems.indexOf(item)
                            )
                        }
                    }
                    val emptySlots = columnCount - rowItems.size
                    if (emptySlots > 0) {
                        Spacer(modifier = Modifier.weight(emptySlots.toFloat()))
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoGridItem(
    mediaItem: MediaItem,
    onClick: () -> Unit,
    viewModel: GalleryViewModel,
    gridMode: String = "square",
    index: Int = 0,
    modifier: Modifier = Modifier
) {
    var isImageLoading by remember { mutableStateOf(true) }
    val aspectRatio = if (gridMode == "mosaic") {
        when (index % 3) {
            0 -> 1.35f
            1 -> 0.75f
            else -> 1f
        }
    } else {
        1f
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    // Drag-and-drop animation states
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val animatedOffset by animateOffsetAsState(
        targetValue = if (isDragging) dragOffset else Offset.Zero,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "drag_offset_anim"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "drag_scale_anim"
    )

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "drag_elev_anim"
    )

    val customSelectionActions by viewModel.customSelectionActions.collectAsState()

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                val size = coordinates.size
                viewModel.updateThumbnailBounds(
                    mediaItem.id,
                    position.x,
                    position.y,
                    size.width.toFloat(),
                    size.height.toFloat()
                )
            }
            .zIndex(if (isDragging) 99f else 1f)
            .graphicsLayer {
                translationX = animatedOffset.x
                translationY = animatedOffset.y
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation.toPx()
                shape = RoundedCornerShape(12.dp)
                clip = false
            }
            .pointerInput(mediaItem) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { startOffset ->
                        isDragging = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        
                        // Drag shadow system-wide integration
                        try {
                            val clipData = ClipData.newUri(context.contentResolver, mediaItem.name, mediaItem.uri)
                            val dragShadowBuilder = android.view.View.DragShadowBuilder(view)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                view.startDragAndDrop(clipData, dragShadowBuilder, mediaItem, 0)
                            }
                        } catch (e: Exception) {
                            // Suppressed
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    },
                    onDragEnd = {
                        isDragging = false
                        dragOffset = Offset.Zero
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffset = Offset.Zero
                    }
                )
            }
            .clickable(enabled = !isDragging, onClick = onClick)
            .background(shimmerBrush(showShimmer = isImageLoading))
            .testTag("media_item_${mediaItem.id}")
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(mediaItem.uri)
                .crossfade(true)
                .build(),
            contentDescription = mediaItem.name,
            contentScale = ContentScale.Crop,
            onLoading = { isImageLoading = true },
            onSuccess = { isImageLoading = false },
            onError = { isImageLoading = false },
            modifier = Modifier.fillMaxSize()
        )

        // Clippy Clipboard Overlay Button
        if (customSelectionActions) {
            FilledIconButton(
                onClick = {
                    ClipboardHelper.copyUriToClipboard(context, mediaItem.uri, mediaItem.mimeType, mediaItem.name)
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(36.dp)
                    .testTag("clippy_thumbnail_button_${mediaItem.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy to Clipboard",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Video Duration Indicator Overlay
        if (mediaItem.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                            startY = 100f
                        )
                    )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video duration",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = mediaItem.durationString ?: "0:00",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Favorite Indicator Badge
        if (mediaItem.isFavorite) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favorited",
                tint = Color.Red,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(16.dp)
            )
        }
    }
}

@Composable
fun FloatingSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    fancyBlur: Boolean = true,
    liquidGlassMode: Boolean = false
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val borderColor = if (liquidGlassMode) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    }
    val borderWidth = if (liquidGlassMode) 1.5.dp else 1.dp
    val containerColor = if (liquidGlassMode) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = if (fancyBlur) 0.95f else 1f)
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = if (liquidGlassMode) 0.dp else 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("floating_search_bar")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (liquidGlassMode) {
                // Runtime gradient overlay mimicking light reflection
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.05f)
                                )
                            )
                        )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search icon",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp, end = 12.dp)
                    .size(24.dp)
            )

            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search yours or local photos",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input")
            )

            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Profile avatar placeholder on the right
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onProfileClick)
                    .testTag("profile_avatar"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}
}

@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("profile_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile header
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Android Gallery User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "local.gallery@android.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = "Offline mode"
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Offline Mode Active",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "No cloud syncing or internet tracking",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = "Privacy settings"
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "100% Secure & Private",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Your photos never leave your device",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onSettingsClick()
                        }
                        .padding(vertical = 12.dp)
                        .testTag("settings_option_row")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "App settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Prism Settings",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "App style, encryption, and local behaviors",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoLibrary,
            contentDescription = "No photos indicator",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun AlbumsTab(
    viewModel: GalleryViewModel,
    onMediaClick: (MediaItem) -> Unit,
    onCategoryClick: (String, List<MediaItem>) -> Unit,
    onOpenVault: () -> Unit,
    onOpenTrash: () -> Unit
) {
    val rawMedia by viewModel.rawMediaItems.collectAsState()
    val folders by viewModel.albumFolders.collectAsState()
    val favorites by viewModel.favoriteIds.collectAsState()

    val videos = remember(rawMedia) { rawMedia.filter { it.isVideo } }
    val favItems = remember(rawMedia, favorites) { rawMedia.filter { favorites.contains(it.id) } }
    val selfies = remember(rawMedia) { rawMedia.filter { it.name.contains("Selfie", true) || it.id in listOf(4L, 6L) } }
    val panoramas = remember(rawMedia) { rawMedia.filter { it.name.contains("Yosemite", true) || it.id in listOf(1L, 10L, 12L) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Albums",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Smart horizontal carousels
        Text(
            text = "Smart Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmartCategoryCard("Videos", videos, Icons.Outlined.PlayCircle, onClick = { onCategoryClick("Videos", videos) })
            SmartCategoryCard("Favorites", favItems, Icons.Outlined.FavoriteBorder, onClick = { onCategoryClick("Favorites", favItems) })
            SmartCategoryCard("Selfies", selfies, Icons.Outlined.Face, onClick = { onCategoryClick("Selfies", selfies) })
            SmartCategoryCard("Panoramas", panoramas, Icons.Outlined.CropFree, onClick = { onCategoryClick("Panoramas", panoramas) })
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Photos on Device",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (folders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No device folders found", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val chunkedFolders = folders.chunked(2)
                for (rowFolders in chunkedFolders) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (folder in rowFolders) {
                            Box(modifier = Modifier.weight(1f)) {
                                FolderItem(
                                    name = folder.name,
                                    count = folder.items.size,
                                    coverMedia = folder.items.firstOrNull(),
                                    onClick = { onCategoryClick(folder.name, folder.items) }
                                )
                            }
                        }
                        if (rowFolders.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(16.dp))

        // Security Vault and Recycle Bin cards
        Text(
            text = "Utilities & Security",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vault Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenVault)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure Vault",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Secure Vault",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Private encrypted space",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Trash Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenTrash)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Recycle Bin",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Recycle Bin",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Trash countdown lifecycle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SmartCategoryCard(
    title: String,
    items: List<MediaItem>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .width(130.dp)
            .height(130.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val cover = items.firstOrNull()
            if (cover != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(cover.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                            startY = 60f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${items.size} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun FolderItem(
    name: String,
    count: Int,
    coverMedia: MediaItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
        ) {
            if (coverMedia != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverMedia.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = name,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "$count items",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTab(
    viewModel: GalleryViewModel,
    onMediaClick: (MediaItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Custom Search Input Row
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search by name, location, metadata...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Category chips row
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("Nature", "Beach", "Skyline", "Japan", "Retro", "Video")
            categories.forEach { cat ->
                InputChip(
                    selected = searchQuery.equals(cat, ignoreCase = true),
                    onClick = { viewModel.setSearchQuery(cat) },
                    label = { Text(cat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Results timeline
        when (val state = uiState) {
            is GalleryUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is GalleryUiState.Success -> {
                if (state.allMedia.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results match \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(state.allMedia.size) { index ->
                            val item = state.allMedia[index]
                            PhotoGridItem(
                                mediaItem = item,
                                onClick = { onMediaClick(item) },
                                viewModel = viewModel,
                                gridMode = "square",
                                index = index
                            )
                        }
                    }
                }
            }
            is GalleryUiState.Error -> {
                Text(text = state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun VaultScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit
) {
    val rawMedia by viewModel.rawMediaItems.collectAsState()
    val vaultIds by viewModel.vaultIds.collectAsState()
    val vaultPin by viewModel.vaultPin.collectAsState()

    val context = LocalContext.current
    val useBiometricsForVault by viewModel.useBiometricsForVault.collectAsState()

    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinSetupStep by remember { mutableStateOf(vaultPin.isEmpty()) }
    var tempSetupPin by remember { mutableStateOf("") }
    var setupMessage by remember { mutableStateOf("Set a 4-Digit Security PIN") }

    // Selected item for full screen detail inside vault
    var selectedVaultMediaItem by remember { mutableStateOf<MediaItem?>(null) }

    val vaultItems = remember(rawMedia, vaultIds) {
        rawMedia.filter { vaultIds.contains(it.id) }
    }

    val triggerBiometrics: () -> Unit = {
        if (useBiometricsForVault && vaultPin.isNotEmpty() && !isUnlocked) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    title = "Decrypt Secure Vault",
                    subtitle = "Verify biometric identity to access private encrypted gallery space",
                    onSuccess = {
                        isUnlocked = true
                    },
                    onError = {
                        // Silent fallback, allows PIN code
                    }
                )
            }
        }
    }

    LaunchedEffect(useBiometricsForVault, vaultPin) {
        if (useBiometricsForVault && vaultPin.isNotEmpty() && !isUnlocked) {
            triggerBiometrics()
        }
    }

    // Force Dark Theme visual context regardless of system settings
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0C0C0C),
            surface = Color(0xFF141414),
            primary = Color(0xFF3F51B5),
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(
            color = Color(0xFF0C0C0C),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (!isUnlocked && vaultPin.isNotEmpty()) {
                // Pin Entry Mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Vault Locked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Secure Vault Locked",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Enter security PIN code to access vault",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Dots indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        for (i in 0 until 4) {
                            val dotColor = if (i < enteredPin.length) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }

                    if (useBiometricsForVault && vaultPin.isNotEmpty()) {
                        IconButton(
                            onClick = { triggerBiometrics() },
                            modifier = Modifier.padding(bottom = 16.dp).size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Use Biometrics",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    // PIN keypad
                    PinKeypad(
                        onDigitClick = { digit ->
                            if (enteredPin.length < 4) {
                                enteredPin += digit
                                if (enteredPin.length == 4) {
                                    if (enteredPin == vaultPin) {
                                        isUnlocked = true
                                    } else {
                                        enteredPin = ""
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                            }
                        },
                        onClear = {
                            enteredPin = ""
                        },
                        onBack = onBack
                    )
                }
            } else if (pinSetupStep) {
                // Setup PIN code mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "PIN Setup",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = setupMessage,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // Dots indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 40.dp)
                    ) {
                        for (i in 0 until 4) {
                            val activeLength = if (tempSetupPin.isEmpty()) enteredPin.length else tempSetupPin.length
                            val dotColor = if (i < activeLength) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }

                    PinKeypad(
                        onDigitClick = { digit ->
                            if (tempSetupPin.isEmpty()) {
                                if (enteredPin.length < 4) {
                                    enteredPin += digit
                                    if (enteredPin.length == 4) {
                                        tempSetupPin = enteredPin
                                        enteredPin = ""
                                        setupMessage = "Confirm Your PIN Code"
                                    }
                                }
                            } else {
                                if (enteredPin.length < 4) {
                                    enteredPin += digit
                                    if (enteredPin.length == 4) {
                                        if (enteredPin == tempSetupPin) {
                                            viewModel.setVaultPin(enteredPin)
                                            pinSetupStep = false
                                            isUnlocked = true
                                        } else {
                                            enteredPin = ""
                                            tempSetupPin = ""
                                            setupMessage = "PINs mismatched! Start over:"
                                        }
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                            }
                        },
                        onClear = {
                            enteredPin = ""
                        },
                        onBack = onBack
                    )
                }
            } else {
                // Unlocked Secure Vault list view!
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Secure Vault",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (vaultItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Secure Vault is Empty", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(vaultItems.size) { index ->
                                val item = vaultItems[index]
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clickable { selectedVaultMediaItem = item }
                                ) {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = item.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sub-detail view overlay inside Vault
            if (selectedVaultMediaItem != null) {
                val item = selectedVaultMediaItem!!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Control Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { selectedVaultMediaItem = null }
                        ) {
                            Text("Close")
                        }
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            onClick = {
                                viewModel.removeFromVault(item.id)
                                selectedVaultMediaItem = null
                            }
                        ) {
                            Text("Remove from Vault")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⌫")
        )

        keys.forEach { rowKeys ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowKeys.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                when (key) {
                                    "C" -> onClear()
                                    "⌫" -> onBackspace()
                                    else -> onDigitClick(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBack) {
            Text("Cancel", color = Color.White)
        }
    }
}

@Composable
fun TrashScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit
) {
    val rawMedia by viewModel.rawMediaItems.collectAsState()
    val trashIds by viewModel.trashIds.collectAsState()

    val selectedTrashIds = remember { mutableStateOf(setOf<Long>()) }

    val trashItems = remember(rawMedia, trashIds) {
        rawMedia.filter { trashIds.containsKey(it.id) }
    }

    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Recycle Bin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${trashItems.size} items",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedTrashIds.value.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            onClick = {
                                selectedTrashIds.value.forEach { id ->
                                    viewModel.restoreFromTrash(id)
                                }
                                selectedTrashIds.value = emptySet()
                            }
                        ) {
                            Text("Restore (${selectedTrashIds.value.size})")
                        }

                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            onClick = {
                                selectedTrashIds.value.forEach { id ->
                                    viewModel.permanentlyDelete(id)
                                }
                                selectedTrashIds.value = emptySet()
                            }
                        ) {
                            Text("Permanently Delete")
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        if (trashItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Recycle Bin is Empty", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 100.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(trashItems.size) { index ->
                    val item = trashItems[index]
                    val deletedTime = trashIds[item.id] ?: System.currentTimeMillis()
                    val daysRemaining = remember(deletedTime) {
                        val elapsedMillis = System.currentTimeMillis() - deletedTime
                        val elapsedDays = elapsedMillis / (24L * 3600L * 1000L)
                        (30L - elapsedDays).coerceIn(1L, 30L)
                    }

                    val isSelected = selectedTrashIds.value.contains(item.id)

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                val current = selectedTrashIds.value.toMutableSet()
                                if (isSelected) current.remove(item.id) else current.add(item.id)
                                selectedTrashIds.value = current
                            }
                    ) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Countdown badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "$daysRemaining days left",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        // Selection Checkbox Badge
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.glassyBlur(enabled: Boolean = true, radius: Float = 25f): Modifier {
    if (!enabled) return this
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        this.graphicsLayer {
            val renderEffect = RenderEffect.createBlurEffect(
                radius,
                radius,
                Shader.TileMode.DECAL
            )
            this.renderEffect = renderEffect.asComposeRenderEffect()
            clip = true
        }
    } else {
        this.blur(radius.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryGalleryScreen(
    title: String,
    items: List<MediaItem>,
    viewModel: GalleryViewModel,
    onMediaClick: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    val columnCount by viewModel.columnCount.collectAsState()
    val gridMode by viewModel.gridMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("${items.size} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No items found in this category")
            }
        } else {
            val groupedByDate = remember(items) {
                val sdf = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault())
                items.groupBy { item ->
                    sdf.format(java.util.Date(item.dateAdded * 1000L))
                }
            }

            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedByDate.forEach { (date, groupItems) ->
                        stickyHeader(key = "category_header_$date") {
                            Surface(
                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }

                        val chunkedItems = groupItems.chunked(columnCount)
                        itemsIndexed(
                            items = chunkedItems,
                            key = { rowIndex, rowItems -> "cat_row_${date}_${rowIndex}_${rowItems.firstOrNull()?.id ?: 0}" }
                        ) { rowIndex, rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                for (item in rowItems) {
                                    Box(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        PhotoGridItem(
                                            mediaItem = item,
                                            onClick = { onMediaClick(item) },
                                            viewModel = viewModel,
                                            gridMode = gridMode,
                                            index = rowIndex * columnCount + rowItems.indexOf(item)
                                        )
                                    }
                                }
                                val emptySlots = columnCount - rowItems.size
                                if (emptySlots > 0) {
                                    Spacer(modifier = Modifier.weight(emptySlots.toFloat()))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
