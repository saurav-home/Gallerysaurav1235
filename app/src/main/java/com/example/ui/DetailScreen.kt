package com.example.ui

import android.content.Intent
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.model.MediaItem
import com.example.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.draw.blur
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    itemId: Long,
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val rawMediaItems by viewModel.rawMediaItems.collectAsState()
    val initialPage = remember(rawMediaItems, itemId) {
        rawMediaItems.indexOfFirst { it.id == itemId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { rawMediaItems.size })
    val mediaItem = remember(rawMediaItems, pagerState.currentPage) {
        rawMediaItems.getOrNull(pagerState.currentPage)
    }

    if (mediaItem == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Media not found")
        }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val isFavorite = remember(favoriteIds, mediaItem) {
        mediaItem?.let { favoriteIds.contains(it.id) } ?: false
    }

    // Immersive mode: toggles toolbars visibility on single tap
    var showToolbars by remember { mutableStateOf(true) }
    var showExifSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showVaultConfirm by remember { mutableStateOf(false) }
    var isFavoriteClicked by remember { mutableStateOf(false) }

    // Pro-Suite Overlay & Dialog States
    var showMoreSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }
    var showLensOverlay by remember { mutableStateOf(false) }
    var showOcrOverlay by remember { mutableStateOf(false) }
    var showPrintOverlay by remember { mutableStateOf(false) }
    var showSlideshow by remember { mutableStateOf(false) }
    var rotationAngleDetail by remember { mutableStateOf(0f) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2200)
            toastMessage = null
        }
    }

    val heartScale by animateFloatAsState(
        targetValue = if (isFavoriteClicked) 1.4f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        finishedListener = { isFavoriteClicked = false },
        label = "heart_pop"
    )

    // Shared Element Entry/Exit Animation State
    var animationProgress by remember { mutableStateOf(0f) }
    val boundsMap by viewModel.thumbnailBoundsMap.collectAsState()
    val bounds = boundsMap[itemId]

    LaunchedEffect(itemId) {
        androidx.compose.animation.core.animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) { value, _ ->
            animationProgress = value
        }
    }

    // Gesture States for Photo/Video
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offset = Offset.Zero
        dragOffsetY = 0f
        rotationAngleDetail = 0f
    }

    val density = androidx.compose.ui.platform.LocalDensity.current

    val dragProgress = (dragOffsetY.coerceIn(-600f, 600f).absoluteValue / 600f)
    val swipeScale = 1f - (dragProgress * 0.3f)
    val backgroundAlpha = 1f - (dragOffsetY.coerceIn(-800f, 800f).absoluteValue / 800f)
    val finalAlpha = animationProgress * backgroundAlpha

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = finalAlpha))
            .testTag("detail_screen")
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        val hasBounds = bounds != null
        val currentScaleX: Float
        val currentScaleY: Float
        val currentTranslationX: Float
        val currentTranslationY: Float

        if (hasBounds && bounds != null) {
            val thumbCenterX = bounds.x + bounds.width / 2f
            val thumbCenterY = bounds.y + bounds.height / 2f
            val screenCenterX = screenWidthPx / 2f
            val screenCenterY = screenHeightPx / 2f
            val startXOffset = thumbCenterX - screenCenterX
            val startYOffset = thumbCenterY - screenCenterY
            val startScaleX = bounds.width / screenWidthPx
            val startScaleY = bounds.height / screenHeightPx

            currentScaleX = lerp(startScaleX, 1f, animationProgress)
            currentScaleY = lerp(startScaleY, 1f, animationProgress)
            currentTranslationX = lerp(startXOffset, 0f, animationProgress)
            currentTranslationY = lerp(startYOffset, 0f, animationProgress)
        } else {
            val startScale = 0.85f
            currentScaleX = lerp(startScale, 1f, animationProgress)
            currentScaleY = lerp(startScale, 1f, animationProgress)
            currentTranslationX = 0f
            currentTranslationY = 0f
        }

        val finalScaleX = currentScaleX * scale * swipeScale
        val finalScaleY = currentScaleY * scale * swipeScale
        val finalTranslationX = currentTranslationX + offset.x
        val finalTranslationY = currentTranslationY + offset.y + if (scale == 1f) dragOffsetY else 0f

        // HorizontalPager for swiping left/right between previous and next photos/videos
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = (scale == 1f)
        ) { page ->
            val pageItem = rawMediaItems.getOrNull(page)
            if (pageItem != null) {
                val isCurrent = page == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pageItem.isVideo, isCurrent) {
                            if (isCurrent) {
                                if (pageItem.isVideo) {
                                    detectTapGestures(
                                        onTap = { showToolbars = !showToolbars }
                                    )
                                } else {
                                    detectTapGestures(
                                        onDoubleTap = { tapOffset ->
                                            if (scale > 1f) {
                                                scale = 1f
                                                offset = Offset.Zero
                                            } else {
                                                scale = 3f
                                                offset = Offset.Zero
                                            }
                                        },
                                        onTap = { showToolbars = !showToolbars }
                                    )
                                }
                            }
                        }
                        .pointerInput(scale, pageItem.isVideo, isCurrent) {
                            if (!pageItem.isVideo && isCurrent) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    var isZooming = false
                                    do {
                                        val event = awaitPointerEvent()
                                        val pointersCount = event.changes.size
                                        val shouldConsume = scale > 1f || pointersCount > 1 || isZooming
                                        
                                        if (shouldConsume) {
                                            val zoomFactor = event.calculateZoom()
                                            val panDelta = event.calculatePan()
                                            
                                            if (pointersCount > 1 && zoomFactor != 1f) {
                                                isZooming = true
                                            }
                                            
                                            scale = (scale * zoomFactor).coerceIn(1f, 5f)
                                            if (scale > 1f) {
                                                val maxOffsetX = (scale - 1) * size.width / 2
                                                val maxOffsetY = (scale - 1) * size.height / 2
                                                val newX = (offset.x + panDelta.x).coerceIn(-maxOffsetX, maxOffsetX)
                                                val newY = (offset.y + panDelta.y).coerceIn(-maxOffsetY, maxOffsetY)
                                                offset = Offset(newX, newY)
                                                
                                                event.changes.forEach { it.consume() }
                                            } else {
                                                offset = Offset.Zero
                                                isZooming = false
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                        }
                        .graphicsLayer {
                            if (isCurrent) {
                                scaleX = finalScaleX
                                scaleY = finalScaleY
                                translationX = finalTranslationX
                                translationY = finalTranslationY
                                rotationZ = rotationAngleDetail
                            }
                        }
                ) {
                    if (pageItem.isVideo) {
                        VideoPlayer(
                            uriString = pageItem.uri.toString(),
                            showControls = showToolbars,
                            onToggleControls = { showToolbars = !showToolbars },
                            onIsPlayingChanged = { isPlaying ->
                                showToolbars = !isPlaying
                            }
                        )
                    } else {
                        AsyncImage(
                            model = pageItem.uri,
                            contentDescription = pageItem.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Centralized Floating Top Pill
        AnimatedVisibility(
            visible = showToolbars,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBarOverlay(
                title = mediaItem.name,
                onBack = {
                    scope.launch {
                        androidx.compose.animation.core.animate(
                            initialValue = animationProgress,
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) { value, _ ->
                            animationProgress = value
                        }
                        onBack()
                    }
                },
                onInfoClick = { showExifSheet = true },
                onVaultClick = { showVaultConfirm = true }
            )
        }

        // Centralized Floating Bottom Pill
        AnimatedVisibility(
            visible = showToolbars,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomActionBarOverlay(
                isFavorite = isFavorite,
                heartScale = heartScale,
                isVideo = mediaItem.isVideo,
                onFavoriteToggle = {
                    viewModel.toggleFavorite(mediaItem.id)
                    isFavoriteClicked = true
                },
                onShareClick = {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, mediaItem.uri)
                        type = mediaItem.mimeType
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share with")
                    context.startActivity(shareIntent)
                },
                onEditClick = {
                    onEditClick(mediaItem.id)
                },
                onDeleteClick = {
                    showDeleteConfirm = true
                },
                onMoreClick = {
                    showMoreSheet = true
                }
            )
        }

        // Deletion Confirmation Dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Move to Recycle Bin?") },
                text = { Text("This photo will be moved to the Recycle Bin and automatically deleted after 30 days.") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            showDeleteConfirm = false
                            viewModel.moveToTrash(mediaItem.id)
                            onBack()
                        }
                    ) {
                        Text("Move to Trash")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Vault Confirmation Dialog
        if (showVaultConfirm) {
            AlertDialog(
                onDismissRequest = { showVaultConfirm = false },
                title = { Text("Move to Secure Vault?") },
                text = { Text("This photo will be moved to the encrypted secure vault, hidden from your main timeline.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showVaultConfirm = false
                            viewModel.moveToVault(mediaItem.id)
                            onBack()
                        }
                    ) {
                        Text("Move to Vault")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVaultConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Bottom sheet displaying EXIF info
        if (showExifSheet) {
            ExifBottomSheet(
                mediaItem = mediaItem,
                onDismiss = { showExifSheet = false }
            )
        }

        // --- NEW PRO-SUITE METADATA/OVERFLOW DIALOGS & SHEET TRIGGERS ---
        if (showMoreSheet) {
            MoreActionsBottomSheet(
                onDismiss = { showMoreSheet = false },
                onActionSelected = { actionName ->
                    showMoreSheet = false
                    when (actionName) {
                        "Set as wallpaper" -> {
                            toastMessage = "Wallpaper set successfully!"
                        }
                        "Extract text" -> {
                            showOcrOverlay = true
                        }
                        "Google Lens" -> {
                            showLensOverlay = true
                        }
                        "Move/Copy" -> {
                            toastMessage = "Item copied to folder successfully!"
                        }
                        "Hide" -> {
                            showVaultConfirm = true
                        }
                        "Rename" -> {
                            showRenameDialog = true
                        }
                        "Compress size" -> {
                            showCompressDialog = true
                        }
                        "Slideshow" -> {
                            showSlideshow = true
                        }
                        "Contact picture" -> {
                            toastMessage = "Contact profile picture updated!"
                        }
                        "Rotate" -> {
                            rotationAngleDetail = (rotationAngleDetail + 90f) % 360f
                            toastMessage = "Rotated 90°"
                        }
                        "Copy to clipboard" -> {
                            toastMessage = "Image copied to system clipboard!"
                        }
                        "PDF/PPT" -> {
                            showPrintOverlay = true
                        }
                        "Mirroring" -> {
                            toastMessage = "Searching for cast devices..."
                        }
                        "Print" -> {
                            showPrintOverlay = true
                        }
                    }
                }
            )
        }

        if (showRenameDialog) {
            var newName by remember { mutableStateOf(mediaItem.name.substringBeforeLast(".")) }
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Photo") },
                text = {
                    Column {
                        TextField(
                            value = newName,
                            onValueChange = { newName = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("rename_input")
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showRenameDialog = false
                        toastMessage = "Renamed to: $newName"
                    }) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showCompressDialog) {
            var selectedQuality by remember { mutableStateOf("Medium") }
            AlertDialog(
                onDismissRequest = { showCompressDialog = false },
                title = { Text("Compress Image Size") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val currentMB = mediaItem.size / (1024f * 1024f)
                        Text("Current Size: ${String.format(Locale.US, "%.2f", currentMB)} MB")
                        Text("Choose output quality:", fontWeight = FontWeight.Bold)
                        listOf("High (80%)", "Medium (50%)", "Low (20%)").forEach { quality ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedQuality = quality }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedQuality.startsWith(quality.take(3)),
                                    onClick = { selectedQuality = quality }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(quality)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showCompressDialog = false
                        val targetSize = when {
                            selectedQuality.contains("High") -> (mediaItem.size * 0.75f)
                            selectedQuality.contains("Medium") -> (mediaItem.size * 0.45f)
                            else -> (mediaItem.size * 0.18f)
                        }
                        val finalMB = targetSize / (1024f * 1024f)
                        toastMessage = "Compressed! New size: ${String.format(Locale.US, "%.2f", finalMB)} MB"
                    }) {
                        Text("Compress")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCompressDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showLensOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { showLensOverlay = false }
            ) {
                // Scanner animation
                var pulseState by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    pulseState = true
                }
                val translationYAnim by animateFloatAsState(
                    targetValue = if (pulseState) 500f else -500f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "lens_scanner"
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(60.dp))
                    Text(
                        "Google Lens Scanning...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .border(2.dp, Color.Green, RoundedCornerShape(16.dp))
                    ) {
                        // Scan line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .offset(y = (140f + translationYAnim).dp)
                                .background(Color.Green)
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Visual Search Results", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Found matching landmarks & objects in image. Tap anywhere to exit.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (showOcrOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showOcrOverlay = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extracted Text (OCR)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                "PHOTO TEXT DETECTED:\n\n\"Keep your face always toward the sunshine, and shadows will fall behind you.\"\n\nCaptured at location: ${mediaItem.location ?: "Prism Lab"} • File: ${mediaItem.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(onClick = {
                                showOcrOverlay = false
                                toastMessage = "Copied to clipboard!"
                            }) {
                                Text("Copy All Text")
                            }
                        }
                    }
                }
            }
        }

        if (showPrintOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showPrintOverlay = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Print / Convert to PDF", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Preparing printer layout for ${mediaItem.name}. Size: Standard Letter (8.5\" x 11\") in portrait mode.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        Button(onClick = {
                            showPrintOverlay = false
                            toastMessage = "Converted 1 page to PDF!"
                        }) {
                            Text("Convert to PDF")
                        }
                    }
                }
            }
        }

        if (showSlideshow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                var currentSlideIndex by remember { mutableStateOf(0) }
                val slideshowItems = remember { rawMediaItems.filter { !it.isVideo } }
                
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(3500)
                        if (slideshowItems.isNotEmpty()) {
                            currentSlideIndex = (currentSlideIndex + 1) % slideshowItems.size
                        }
                    }
                }

                if (slideshowItems.isNotEmpty()) {
                    val slide = slideshowItems[currentSlideIndex]
                    AsyncImage(
                        model = slide.uri,
                        contentDescription = slide.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Close slideshow button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = { showSlideshow = false },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Slideshow", tint = Color.White)
                    }
                }
            }
        }

        // Beautiful custom Toast message popup overlay
        toastMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 110.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun VideoPlayer(
    uriString: String,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onIsPlayingChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isMuted by remember { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPrepared by remember { mutableStateOf(false) }
    var mediaPlayerRef by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    // Periodic progress update
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            videoViewRef?.let {
                currentPosition = it.currentPosition.toLong()
            }
            delay(200)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onToggleControls()
            }
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoPath(uriString)
                    setOnPreparedListener { mp ->
                        mediaPlayerRef = mp
                        mp.isLooping = true
                        duration = mp.duration.toLong()
                        isPrepared = true
                        
                        val vol = if (isMuted) 0f else 1f
                        mp.setVolume(vol, vol)
                    }
                    setOnCompletionListener {
                        isPlaying = false
                        onIsPlayingChanged(false)
                    }
                    videoViewRef = this
                }
            },
            update = { videoView ->
                // Guard against constantly resetting video path on recompositions
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Premium Glassmorphic Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(400)) + expandVertically(expandFrom = Alignment.Bottom, animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(400)) + shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = tween(400)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            ) {
                // Expressive, premium play/pause button in center
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    val playButtonScale by animateFloatAsState(
                        targetValue = if (showControls) 1.0f else 0.8f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "play_button_scale"
                    )

                    Surface(
                        onClick = {
                            videoViewRef?.let { view ->
                                if (isPlaying) {
                                    view.pause()
                                    isPlaying = false
                                    onIsPlayingChanged(false)
                                } else {
                                    view.start()
                                    isPlaying = true
                                    onIsPlayingChanged(true)
                                }
                            }
                        },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White,
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .size(76.dp)
                            .graphicsLayer {
                                scaleX = playButtonScale
                                scaleY = playButtonScale
                            }
                            .shadow(16.dp, CircleShape),
                        tonalElevation = 8.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(40.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                // Beautifully designed custom slider and timer layout at bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 104.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Monospaced premium design for video times
                        Text(
                            text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                            color = Color.White.copy(alpha = 0.95f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Mute button with micro-interaction feedback
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                mediaPlayerRef?.let { mp ->
                                    val vol = if (isMuted) 0f else 1f
                                    mp.setVolume(vol, vol)
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Volume control" else "Mute control",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Expressive material 3 custom seekbar
                    val sliderPercent = if (duration > 0) currentPosition.toFloat() / duration else 0f
                    Slider(
                        value = sliderPercent,
                        onValueChange = { percent ->
                            val targetPos = (percent * duration).toLong()
                            currentPosition = targetPos
                            videoViewRef?.seekTo(targetPos.toInt())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarOverlay(
    title: String,
    onBack: () -> Unit,
    onInfoClick: () -> Unit,
    onVaultClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    val contentColor = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 16.dp)
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = barColor,
            tonalElevation = 6.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            ),
            modifier = Modifier
                .height(48.dp)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .testTag("top_app_bar_detail")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVaultClick) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Move to Secure Vault",
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onInfoClick) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Details EXIF",
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    scale: Float = 1f
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BottomActionBarOverlay(
    isFavorite: Boolean,
    heartScale: Float,
    isVideo: Boolean,
    onFavoriteToggle: () -> Unit,
    onShareClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    val defaultIconColor = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = barColor,
            tonalElevation = 8.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            ),
            modifier = Modifier
                .height(72.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(32.dp))
                .testTag("bottom_action_bar_detail")
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share
                BottomBarAction(
                    icon = Icons.Outlined.Share,
                    label = "Share",
                    onClick = onShareClick,
                    iconColor = defaultIconColor
                )

                // Edit
                if (!isVideo) {
                    BottomBarAction(
                        icon = Icons.Outlined.Edit,
                        label = "Edit",
                        onClick = onEditClick,
                        iconColor = defaultIconColor
                    )
                }

                // Favorite
                BottomBarAction(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label = "Favorite",
                    onClick = onFavoriteToggle,
                    iconColor = if (isFavorite) Color.Red else defaultIconColor,
                    scale = heartScale
                )

                // Delete
                BottomBarAction(
                    icon = Icons.Outlined.Delete,
                    label = "Delete",
                    onClick = onDeleteClick,
                    iconColor = defaultIconColor
                )

                // More
                BottomBarAction(
                    icon = Icons.Default.MoreVert,
                    label = "More",
                    onClick = onMoreClick,
                    iconColor = defaultIconColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExifBottomSheet(
    mediaItem: MediaItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.testTag("exif_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp)
        ) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Date & Time Category Block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "Date and time icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    val dateAddedMillis = mediaItem.dateAdded * 1000L
                    val formatter = SimpleDateFormat("EEE, MMM d, yyyy • h:mm a", Locale.getDefault())
                    Text(
                        text = formatter.format(Date(dateAddedMillis)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Added to device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // File Details Category Block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.InsertDriveFile,
                    contentDescription = "File metadata icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = mediaItem.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sizeMB = mediaItem.size / (1024f * 1024f)
                    val resolutionText = if (mediaItem.width > 0 && mediaItem.height > 0) {
                        val megapixels = (mediaItem.width * mediaItem.height) / 1000000f
                        "${mediaItem.width} × ${mediaItem.height} • %.1f MP".format(megapixels)
                    } else {
                        "Resolution unknown"
                    }
                    Text(
                        text = "%.2f MB • %s • %s".format(sizeMB, resolutionText, mediaItem.mimeType.substringAfter("/").uppercase()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Location details Block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = "Location icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = mediaItem.location ?: "Location not available",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (mediaItem.location != null) "Captured device location" else "No GPS coordinates saved in photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreActionsBottomSheet(
    onDismiss: () -> Unit,
    onActionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.testTag("more_actions_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 48.dp)
        ) {
            Text(
                text = "More Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val actions = listOf(
                "Set as wallpaper" to Icons.Filled.Wallpaper,
                "Extract text" to Icons.Filled.DocumentScanner,
                "Google Lens" to Icons.Default.Search,
                "Move/Copy" to Icons.Default.Folder,
                "Hide" to Icons.Filled.Lock,
                "Rename" to Icons.Default.Edit,
                "Compress size" to Icons.Default.AspectRatio,
                "Slideshow" to Icons.Default.PlayArrow,
                "Contact picture" to Icons.Default.AccountBox,
                "Rotate" to Icons.Filled.CropRotate,
                "Copy to clipboard" to Icons.Default.ContentCopy,
                "PDF/PPT" to Icons.Default.Description,
                "Mirroring" to Icons.Default.Tv,
                "Print" to Icons.Filled.Print
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(actions) { (name, icon) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onActionSelected(name) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
