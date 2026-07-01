package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MediaItem
import com.example.viewmodel.GalleryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToInt

data class DoodleStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val isEraser: Boolean = false,
    val brushType: String = "Pen" // "Pen", "Marker", "Neon"
)

data class TextNode(
    val id: Long = System.currentTimeMillis(),
    val text: String = "Double-tap to edit",
    val color: Color = Color.White,
    val fontStyle: String = "Sans", // "Sans", "Serif", "Monospace", "Cursive"
    val alignment: TextAlign = TextAlign.Center,
    val hasBackground: Boolean = false,
    val backgroundColor: Color = Color.Black.copy(alpha = 0.6f),
    val offset: Offset = Offset.Zero,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    // Premium Text Engine parameters
    val strokeColor: Color? = null,
    val hasShadow: Boolean = false,
    val opacity: Float = 1f,
    val isBold: Boolean = false,
    val fontSize: Float = 16f
)

data class EditorState(
    val selectedPreset: String = "original",
    val filterIntensity: Float = 1.0f,
    val brightness: Float = 0f,       // -100 to 100
    val contrast: Float = 0f,         // -100 to 100
    val saturation: Float = 0f,       // -100 to 100
    val warmth: Float = 0f,           // -100 to 100
    val tint: Float = 0f,             // -100 to 100
    val exposure: Float = 0f,         // -100 to 100
    val highlights: Float = 0f,       // -100 to 100
    val shadows: Float = 0f,          // -100 to 100
    val vibrance: Float = 0f,         // -100 to 100
    
    // Retouch / Beautify options
    val smoothIntensity: Float = 0f,      // 0 to 100
    val whiteningIntensity: Float = 0f,   // 0 to 100
    val skinToneIntensity: Float = 0f,    // 0 to 100
    val blemishHealPoints: List<Offset> = emptyList(),
    
    val rotationAngle: Float = 0f,    // 0, 90, 180, 270
    val isFlippedHorizontal: Boolean = false,
    val isFlippedVertical: Boolean = false,
    val straightenAngle: Float = 0f,  // -45 to 45
    val cropAspectRatioName: String = "Free",
    
    val strokes: List<DoodleStroke> = emptyList(),
    val textNodes: List<TextNode> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    itemId: Long,
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val rawMediaItems by viewModel.rawMediaItems.collectAsState()
    val mediaItem = remember(rawMediaItems, itemId) {
        rawMediaItems.firstOrNull { it.id == itemId }
    }

    if (mediaItem == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Media asset not found", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()
    fun triggerHaptic() {
        if (isVibrationEnabled) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    // --- MAIN STATES ---
    var activeTab by remember { mutableStateOf("presets") } // presets, adjust, crop, doodle, text

    // Current live state
    var editorState by remember { mutableStateOf(EditorState()) }

    // History stack for Undo/Redo
    var history by remember { mutableStateOf(listOf(EditorState())) }
    var historyIndex by remember { mutableStateOf(0) }

    fun commitCurrentState() {
        val updatedHistory = history.subList(0, historyIndex + 1).toMutableList()
        if (updatedHistory.isEmpty() || updatedHistory.last() != editorState) {
            updatedHistory.add(editorState)
            history = updatedHistory
            historyIndex = updatedHistory.size - 1
        }
    }

    fun updateStateAndCommit(newState: EditorState) {
        editorState = newState
        val updatedHistory = history.subList(0, historyIndex + 1).toMutableList()
        updatedHistory.add(newState)
        history = updatedHistory
        historyIndex = updatedHistory.size - 1
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            editorState = history[historyIndex]
            triggerHaptic()
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            editorState = history[historyIndex]
            triggerHaptic()
        }
    }

    // --- DOODLE TEMPORARY DRAW STATES ---
    var isDrawModeEnabled by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var brushWidth by remember { mutableStateOf(10f) }
    var brushType by remember { mutableStateOf("Pen") } // Pen, Marker, Neon
    var isEraserEnabled by remember { mutableStateOf(false) }
    var currentStroke by remember { mutableStateOf<DoodleStroke?>(null) }

    // --- TEXT OVERLAYS STATES ---
    var selectedTextNodeId by remember { mutableStateOf<Long?>(null) }
    var showTextEditDialog by remember { mutableStateOf(false) }
    var editingTextNode by remember { mutableStateOf<TextNode?>(null) }

    // --- SAVE & EXPORT DIALOG STATES ---
    var showSaveDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Generate combined ColorMatrix
    val combinedColorMatrix = remember(editorState) {
        getCombinedColorMatrix(
            presetName = editorState.selectedPreset,
            filterIntensity = editorState.filterIntensity,
            brightness = editorState.brightness,
            contrast = editorState.contrast,
            saturation = editorState.saturation,
            warmth = editorState.warmth,
            tint = editorState.tint,
            exposure = editorState.exposure,
            highlights = editorState.highlights,
            shadows = editorState.shadows,
            vibrance = editorState.vibrance
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prism Editor", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        triggerHaptic()
                        onBack()
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Discard")
                    }
                },
                actions = {
                    // Undo/Redo Controls
                    IconButton(
                        onClick = { undo() },
                        enabled = historyIndex > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (historyIndex > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(
                        onClick = { redo() },
                        enabled = historyIndex < history.size - 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (historyIndex < history.size - 1) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            triggerHaptic()
                            showSaveDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_edit_button")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.Black)
            ) {
                // --- IMAGE VIEW WORKSPACE CANVAS ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF121212))
                        .testTag("editor_workspace_container"),
                    contentAlignment = Alignment.Center
                ) {
                    val ratioModifier = when (editorState.cropAspectRatioName) {
                        "1:1" -> Modifier.aspectRatio(1f)
                        "4:3" -> Modifier.aspectRatio(4f / 3f)
                        "3:4" -> Modifier.aspectRatio(3f / 4f)
                        "16:9" -> Modifier.aspectRatio(16f / 9f)
                        "9:16" -> Modifier.aspectRatio(9f / 16f)
                        "2:3" -> Modifier.aspectRatio(2f / 3f)
                        "3:2" -> Modifier.aspectRatio(3f / 2f)
                        "5:7" -> Modifier.aspectRatio(5f / 7f)
                        "7:5" -> Modifier.aspectRatio(7f / 5f)
                        else -> Modifier.fillMaxSize()
                    }

                    // Combined visual frame
                    Box(
                        modifier = ratioModifier
                            .padding(12.dp)
                            .graphicsLayer {
                                rotationZ = editorState.rotationAngle
                                scaleX = if (editorState.isFlippedHorizontal) -1f else 1f
                                scaleY = if (editorState.isFlippedVertical) -1f else 1f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Image Layer
                        AsyncImage(
                            model = mediaItem.uri,
                            contentDescription = "Edited preview",
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.colorMatrix(combinedColorMatrix),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    // Smoothly tilt and scale to keep borders hidden
                                    val angleRad = Math.toRadians(editorState.straightenAngle.toDouble())
                                    val zoomFactor = 1f + (abs(editorState.straightenAngle) / 45f) * 0.18f
                                    scaleX = zoomFactor
                                    scaleY = zoomFactor
                                    rotationZ = editorState.straightenAngle
                                }
                        )

                        // Drawing layer canvas (Offscreen composition so Eraser PorterDuff.Mode.CLEAR doesn't erase image layer)
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                                }
                                .pointerInput(isDrawModeEnabled, selectedColor, brushWidth, isEraserEnabled, brushType) {
                                    if (isDrawModeEnabled) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentStroke = DoodleStroke(
                                                    points = listOf(offset),
                                                    color = selectedColor,
                                                    width = brushWidth,
                                                    isEraser = isEraserEnabled,
                                                    brushType = brushType
                                                )
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                currentStroke?.let { stroke ->
                                                    currentStroke = stroke.copy(
                                                        points = stroke.points + change.position
                                                    )
                                                }
                                            },
                                            onDragEnd = {
                                                currentStroke?.let {
                                                    val nextStrokes = editorState.strokes + it
                                                    updateStateAndCommit(editorState.copy(strokes = nextStrokes))
                                                }
                                                currentStroke = null
                                            }
                                        )
                                    }
                                }
                        ) {
                            editorState.strokes.forEach { stroke ->
                                drawStroke(
                                    stroke = stroke,
                                    blendMode = if (stroke.isEraser) androidx.compose.ui.graphics.BlendMode.Clear else androidx.compose.ui.graphics.BlendMode.SrcOver
                                )
                            }
                            currentStroke?.let { stroke ->
                                drawStroke(
                                    stroke = stroke,
                                    blendMode = if (stroke.isEraser) androidx.compose.ui.graphics.BlendMode.Clear else androidx.compose.ui.graphics.BlendMode.SrcOver
                                )
                            }
                        }

                        // Text Nodes overlay Layer
                        Box(modifier = Modifier.fillMaxSize()) {
                            editorState.textNodes.forEach { node ->
                                val isSelected = selectedTextNodeId == node.id
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(node.offset.x.roundToInt(), node.offset.y.roundToInt()) }
                                        .graphicsLayer {
                                            scaleX = node.scale
                                            scaleY = node.scale
                                            rotationZ = node.rotation
                                        }
                                        .pointerInput(node.id) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    selectedTextNodeId = node.id
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val updatedNodes = editorState.textNodes.map {
                                                        if (it.id == node.id) it.copy(offset = it.offset + dragAmount) else it
                                                    }
                                                    editorState = editorState.copy(textNodes = updatedNodes)
                                                },
                                                onDragEnd = {
                                                    commitCurrentState()
                                                }
                                            )
                                        }
                                        .clickable {
                                            triggerHaptic()
                                            selectedTextNodeId = node.id
                                        }
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                            } else Modifier
                                        )
                                        .padding(8.dp)
                                ) {
                                    val fontFamily = when (node.fontStyle) {
                                        "Serif" -> FontFamily.Serif
                                        "Monospace" -> FontFamily.Monospace
                                        "Cursive" -> FontFamily.Cursive
                                        else -> FontFamily.SansSerif
                                    }

                                    Box(
                                        modifier = if (node.hasBackground) {
                                            Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(node.backgroundColor.copy(alpha = node.backgroundColor.alpha * node.opacity))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        } else Modifier
                                    ) {
                                        Text(
                                            text = node.text,
                                            color = node.color.copy(alpha = node.opacity),
                                            fontFamily = fontFamily,
                                            fontWeight = if (node.isBold) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = node.alignment,
                                            fontSize = node.fontSize.sp,
                                            style = if (node.hasShadow) {
                                                androidx.compose.ui.text.TextStyle(
                                                    shadow = androidx.compose.ui.graphics.Shadow(
                                                        color = Color.Black.copy(alpha = 0.5f),
                                                        offset = Offset(3f, 3f),
                                                        blurRadius = 4f
                                                    )
                                                )
                                            } else androidx.compose.ui.text.TextStyle.Default
                                        )
                                    }

                                    // Floating mini controls when selected
                                    if (isSelected) {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 12.dp, y = (-20).dp)
                                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                                .border(0.5.dp, Color.LightGray, CircleShape)
                                                .padding(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Text",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable {
                                                        editingTextNode = node
                                                        showTextEditDialog = true
                                                    }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Text",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable {
                                                        triggerHaptic()
                                                        val updatedNodes = editorState.textNodes.filter { it.id != node.id }
                                                        updateStateAndCommit(editorState.copy(textNodes = updatedNodes))
                                                        selectedTextNodeId = null
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Rule of Thirds Grid overlay (Active only during Crop/Geometry tab)
                        if (activeTab == "crop") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, Color.White.copy(alpha = 0.35f))
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    // Rule lines
                                    drawLine(Color.White.copy(alpha = 0.35f), Offset(w / 3f, 0f), Offset(w / 3f, h), 1.dp.toPx())
                                    drawLine(Color.White.copy(alpha = 0.35f), Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), 1.dp.toPx())
                                    drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, h / 3f), Offset(w, h / 3f), 1.dp.toPx())
                                    drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), 1.dp.toPx())

                                    // Thick Corner Calipers
                                    val len = 16.dp.toPx()
                                    val thick = 3.dp.toPx()
                                    // Top Left
                                    drawLine(Color.White, Offset(0f, 0f), Offset(len, 0f), thick)
                                    drawLine(Color.White, Offset(0f, 0f), Offset(0f, len), thick)
                                    // Top Right
                                    drawLine(Color.White, Offset(w, 0f), Offset(w - len, 0f), thick)
                                    drawLine(Color.White, Offset(w, 0f), Offset(w, len), thick)
                                    // Bottom Left
                                    drawLine(Color.White, Offset(0f, h), Offset(len, h), thick)
                                    drawLine(Color.White, Offset(0f, h), Offset(0f, h - len), thick)
                                    // Bottom Right
                                    drawLine(Color.White, Offset(w, h), Offset(w - len, h), thick)
                                    drawLine(Color.White, Offset(w, h), Offset(w, h - len), thick)
                                }
                            }
                        }
                    }

                    // Floating temporary mode badge when Doodle is active
                    if (isDrawModeEnabled) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Gesture, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Doodle Mode Active", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // --- TABS PANEL & COMPREHENSIVE CONTROL SYSTEM ---
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Horizontally scrolling Primary Tool Bar (Bottom)
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            item {
                                EditorTabItem(
                                    icon = Icons.Outlined.PhotoFilter,
                                    label = "Presets",
                                    selected = activeTab == "presets",
                                    onClick = {
                                        triggerHaptic()
                                        activeTab = "presets"
                                        isDrawModeEnabled = false
                                    }
                                )
                            }
                            item {
                                EditorTabItem(
                                    icon = Icons.Outlined.Tune,
                                    label = "Adjust",
                                    selected = activeTab == "adjust",
                                    onClick = {
                                        triggerHaptic()
                                        activeTab = "adjust"
                                        isDrawModeEnabled = false
                                    }
                                )
                            }
                            item {
                                EditorTabItem(
                                    icon = Icons.Outlined.Crop,
                                    label = "Framing",
                                    selected = activeTab == "crop",
                                    onClick = {
                                        triggerHaptic()
                                        activeTab = "crop"
                                        isDrawModeEnabled = false
                                    }
                                )
                            }
                            item {
                                EditorTabItem(
                                    icon = Icons.Default.Face,
                                    label = "Retouch",
                                    selected = activeTab == "retouch",
                                    onClick = {
                                        triggerHaptic()
                                        activeTab = "retouch"
                                        isDrawModeEnabled = false
                                    }
                                )
                            }
                            item {
                                EditorTabItem(
                                    icon = Icons.Default.GridOn,
                                    label = "Mosaic",
                                    selected = activeTab == "mosaic",
                                    onClick = {
                                        triggerHaptic()
                                        activeTab = "mosaic"
                                        isDrawModeEnabled = true // Enable drawing for mosaic brushes
                                        isEraserEnabled = false
                                        brushType = "Pixelate" // Default to pixelate brush
                                    }
                                )
                            }
                            item {
                                EditorTabItem(
                                    icon = Icons.Outlined.Gesture,
                                    label = "Doodle",
                                    selected = activeTab == "doodle",
                                    onClick = {
                                        triggerHaptic()
                                        activeTab = "doodle"
                                        isDrawModeEnabled = true
                                        isEraserEnabled = false
                                        brushType = "Pen"
                                    }
                                )
                            }
                            item {
                                EditorTabItem(
                                    icon = Icons.Outlined.TextFormat,
                                    label = "Text",
                                    selected = activeTab == "text",
                                    onClick = {
                                        triggerHaptic()
                                        activeTab = "text"
                                        isDrawModeEnabled = false
                                    }
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                        // ACTIVE TAB CONTROL CONTENT WORKSPACE
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (activeTab) {
                                "presets" -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val presets = listOf(
                                                "original" to "Original",
                                                "portrait" to "Portrait",
                                                "landscape" to "Landscape",
                                                "food" to "Food",
                                                "night" to "Night",
                                                "japanese" to "Japanese",
                                                "film" to "Film"
                                            )
                                            items(presets) { (id, title) ->
                                                PresetFilterThumbnail(
                                                    title = title,
                                                    isSelected = editorState.selectedPreset == id,
                                                    onClick = {
                                                        triggerHaptic()
                                                        val next = editorState.copy(selectedPreset = id)
                                                        updateStateAndCommit(next)
                                                    }
                                                )
                                            }
                                        }

                                        // Filter Intensity slider
                                        if (editorState.selectedPreset != "original") {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                            ) {
                                                Text(
                                                    text = "Filter Intensity",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.width(96.dp)
                                                )
                                                Slider(
                                                    value = editorState.filterIntensity,
                                                    onValueChange = {
                                                        editorState = editorState.copy(filterIntensity = it)
                                                    },
                                                    onValueChangeFinished = {
                                                        commitCurrentState()
                                                    },
                                                    valueRange = 0f..1f,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = "${(editorState.filterIntensity * 100).roundToInt()}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.width(36.dp),
                                                    textAlign = TextAlign.End
                                                )
                                            }
                                        }
                                    }
                                }
                                "adjust" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // Auto-Tune Magic wand row
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Button(
                                                onClick = {
                                                    triggerHaptic()
                                                    val autoState = editorState.copy(
                                                        brightness = 15f,
                                                        contrast = 10f,
                                                        saturation = 8f,
                                                        vibrance = 12f,
                                                        exposure = 5f,
                                                        highlights = -5f,
                                                        shadows = 8f
                                                    )
                                                    updateStateAndCommit(autoState)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Auto", modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Auto-Tune", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        AdjustmentSliderRow(
                                            label = "Brightness",
                                            value = editorState.brightness,
                                            valueRange = -100f..100f,
                                            onValueChange = { editorState = editorState.copy(brightness = it) },
                                            onValueChangeFinished = { commitCurrentState() }
                                        )
                                        AdjustmentSliderRow(
                                            label = "Contrast",
                                            value = editorState.contrast,
                                            valueRange = -100f..100f,
                                            onValueChange = { editorState = editorState.copy(contrast = it) },
                                            onValueChangeFinished = { commitCurrentState() }
                                        )
                                        AdjustmentSliderRow(
                                            label = "Highlights",
                                            value = editorState.highlights,
                                            valueRange = -100f..100f,
                                            onValueChange = { editorState = editorState.copy(highlights = it) },
                                            onValueChangeFinished = { commitCurrentState() }
                                        )
                                        AdjustmentSliderRow(
                                            label = "Shadows",
                                            value = editorState.shadows,
                                            valueRange = -100f..100f,
                                            onValueChange = { editorState = editorState.copy(shadows = it) },
                                            onValueChangeFinished = { commitCurrentState() }
                                        )
                                        AdjustmentSliderRow(
                                            label = "Saturation",
                                            value = editorState.saturation,
                                            valueRange = -100f..100f,
                                            onValueChange = { editorState = editorState.copy(saturation = it) },
                                            onValueChangeFinished = { commitCurrentState() }
                                        )
                                        AdjustmentSliderRow(
                                            label = "Vibrance",
                                            value = editorState.vibrance,
                                            valueRange = -100f..100f,
                                            onValueChange = { editorState = editorState.copy(vibrance = it) },
                                            onValueChangeFinished = { commitCurrentState() }
                                        )
                                        AdjustmentSliderRow(
                                            label = "Temperature",
                                            value = editorState.warmth,
                                            valueRange = -100f..100f,
                                            onValueChange = { editorState = editorState.copy(warmth = it) },
                                            onValueChangeFinished = { commitCurrentState() }
                                        )
                                        AdjustmentSliderRow(
                                            label = "Tint / Hue",
                                            value = editorState.tint,
                                            valueRange = -100f..100f,
                                            onValueChange = { editorState = editorState.copy(tint = it) },
                                            onValueChangeFinished = { commitCurrentState() }
                                        )
                                        AdjustmentSliderRow(
                                            label = "Exposure",
                                            value = editorState.exposure,
                                            valueRange = -100f..100f,
                                            onValueChange = { editorState = editorState.copy(exposure = it) },
                                            onValueChangeFinished = { commitCurrentState() }
                                        )
                                    }
                                }
                                "crop" -> {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Top row: rotation + flip
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RotateActionButton(
                                                icon = Icons.Default.RotateRight,
                                                label = "Rotate 90°",
                                                onClick = {
                                                    triggerHaptic()
                                                    val nextAngle = (editorState.rotationAngle + 90f) % 360f
                                                    updateStateAndCommit(editorState.copy(rotationAngle = nextAngle))
                                                }
                                            )
                                            RotateActionButton(
                                                icon = Icons.Default.Flip,
                                                label = "Flip Horiz.",
                                                onClick = {
                                                    triggerHaptic()
                                                    updateStateAndCommit(editorState.copy(isFlippedHorizontal = !editorState.isFlippedHorizontal))
                                                }
                                            )
                                            RotateActionButton(
                                                icon = Icons.Default.FlipToBack,
                                                label = "Flip Vert.",
                                                onClick = {
                                                    triggerHaptic()
                                                    updateStateAndCommit(editorState.copy(isFlippedVertical = !editorState.isFlippedVertical))
                                                }
                                            )
                                            RotateActionButton(
                                                icon = Icons.Default.Refresh,
                                                label = "Reset View",
                                                onClick = {
                                                    triggerHaptic()
                                                    updateStateAndCommit(
                                                        editorState.copy(
                                                            rotationAngle = 0f,
                                                            isFlippedHorizontal = false,
                                                            isFlippedVertical = false,
                                                            straightenAngle = 0f,
                                                            cropAspectRatioName = "Free"
                                                        )
                                                    )
                                                }
                                            )
                                        }

                                        // Aspect Ratios
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            val ratios = listOf("Free", "Original", "1:1", "4:3", "3:4", "16:9", "9:16", "2:3", "3:2", "5:7", "7:5", "Full")
                                            items(ratios) { name ->
                                                val isSelected = editorState.cropAspectRatioName == name
                                                SuggestionChip(
                                                    onClick = {
                                                        triggerHaptic()
                                                        updateStateAndCommit(editorState.copy(cropAspectRatioName = name))
                                                    },
                                                    label = { Text(name) },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                        labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                        }

                                        // Straighten
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(imageVector = Icons.Default.AlignHorizontalCenter, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Straighten", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                                            Slider(
                                                value = editorState.straightenAngle,
                                                onValueChange = {
                                                    editorState = editorState.copy(straightenAngle = it)
                                                },
                                                onValueChangeFinished = {
                                                    commitCurrentState()
                                                },
                                                valueRange = -45f..45f,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${editorState.straightenAngle.roundToInt()}°",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.width(32.dp),
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }
                                }
                                "retouch" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Portrait Beauty & Skin Retouching",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )

                                        AdjustmentSliderRow(
                                            label = "Skin Smooth",
                                            value = editorState.smoothIntensity,
                                            valueRange = 0f..100f,
                                            onValueChange = { editorState = editorState.copy(smoothIntensity = it) },
                                            onValueChangeFinished = { commitCurrentState() }
                                        )
                                        AdjustmentSliderRow(
                                            label = "Whitening",
                                            value = editorState.whiteningIntensity,
                                            valueRange = 0f..100f,
                                            onValueChange = { editorState = editorState.copy(whiteningIntensity = it) },
                                            onValueChangeFinished = { commitCurrentState() }
                                        )
                                        AdjustmentSliderRow(
                                            label = "Skin Tone",
                                            value = editorState.skinToneIntensity,
                                            valueRange = 0f..100f,
                                            onValueChange = { editorState = editorState.copy(skinToneIntensity = it) },
                                            onValueChangeFinished = { commitCurrentState() }
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Acne / Blemish Healing", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            Button(
                                                onClick = {
                                                    triggerHaptic()
                                                    val healPoints = editorState.blemishHealPoints + Offset(150f, 200f) // Simulated stamp
                                                    updateStateAndCommit(editorState.copy(blemishHealPoints = healPoints))
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                                contentPadding = PaddingValues(horizontal = 12.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Face, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Tap-to-Heal Spot", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                                "mosaic" -> {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Brush Selectors
                                            listOf("Pixelate", "Blur", "Pattern").forEach { type ->
                                                val isSelected = brushType == type && !isEraserEnabled
                                                InputChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        brushType = type
                                                        isEraserEnabled = false
                                                    },
                                                    label = { Text(type, fontSize = 10.sp) },
                                                    modifier = Modifier.height(32.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.weight(1f))

                                            IconButton(
                                                onClick = {
                                                    triggerHaptic()
                                                    isEraserEnabled = !isEraserEnabled
                                                },
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = if (isEraserEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (isEraserEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.LayersClear, contentDescription = "Toggle Eraser", modifier = Modifier.size(16.dp))
                                            }

                                            IconButton(
                                                onClick = {
                                                    triggerHaptic()
                                                    updateStateAndCommit(editorState.copy(strokes = emptyList()))
                                                    currentStroke = null
                                                },
                                                enabled = editorState.strokes.isNotEmpty(),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear Mosaic", modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        // Size slider & Stroke stats
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Stroke Size", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            Slider(
                                                value = brushWidth,
                                                onValueChange = { brushWidth = it },
                                                valueRange = 8f..50f,
                                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                                            )
                                            Text("${brushWidth.roundToInt()}px", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                "doodle" -> {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Controls: Toggle Draw mode, Eraser, Clear Canvas
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Button(
                                                onClick = {
                                                    triggerHaptic()
                                                    isDrawModeEnabled = !isDrawModeEnabled
                                                    if (isDrawModeEnabled) isEraserEnabled = false
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isDrawModeEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = if (isDrawModeEnabled) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1.2f)
                                            ) {
                                                Icon(
                                                    imageVector = if (isDrawModeEnabled) Icons.Default.Draw else Icons.Default.Gesture,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isDrawModeEnabled) "Stop Drawing" else "Start Draw", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // Brush Type selectors: Pen, Marker, Neon
                                            if (isDrawModeEnabled && !isEraserEnabled) {
                                                listOf("Pen", "Marker", "Neon").forEach { type ->
                                                    val isSelected = brushType == type
                                                    InputChip(
                                                        selected = isSelected,
                                                        onClick = { brushType = type },
                                                        label = { Text(type, fontSize = 10.sp) },
                                                        modifier = Modifier.height(32.dp)
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    triggerHaptic()
                                                    isEraserEnabled = !isEraserEnabled
                                                    if (isEraserEnabled) isDrawModeEnabled = true
                                                },
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = if (isEraserEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (isEraserEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.LayersClear, contentDescription = "Toggle Eraser", modifier = Modifier.size(16.dp))
                                            }

                                            IconButton(
                                                onClick = {
                                                    triggerHaptic()
                                                    updateStateAndCommit(editorState.copy(strokes = emptyList()))
                                                    currentStroke = null
                                                },
                                                enabled = editorState.strokes.isNotEmpty(),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear Canvas", modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        // Bottom details: palette & size
                                        if (isDrawModeEnabled) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                // Palette colors
                                                val colors = listOf(
                                                    Color.Red, Color(0xFFFF9800), Color.Green, Color.Blue, Color(0xFF9C27B0), Color.White, Color.Black
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    colors.forEach { color ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                                .background(color)
                                                                .border(
                                                                    width = if (selectedColor == color && !isEraserEnabled) 2.5.dp else 1.dp,
                                                                    color = if (selectedColor == color && !isEraserEnabled) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                                                    shape = CircleShape
                                                                )
                                                                .clickable {
                                                                    triggerHaptic()
                                                                    selectedColor = color
                                                                    isEraserEnabled = false
                                                                }
                                                        )
                                                    }
                                                }

                                                // Brush size slider
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.width(120.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.LineWeight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                                    Slider(
                                                        value = brushWidth,
                                                        onValueChange = { brushWidth = it },
                                                        valueRange = 4f..45f,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "Activate 'Start Draw' to markup directly on the canvas image. Toggle Marker for highlighters and Neon for light effects.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                                            )
                                        }
                                    }
                                }
                                "text" -> {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Button(
                                                onClick = {
                                                    triggerHaptic()
                                                    val newNode = TextNode()
                                                    val nextNodes = editorState.textNodes + newNode
                                                    updateStateAndCommit(editorState.copy(textNodes = nextNodes))
                                                    selectedTextNodeId = newNode.id
                                                    editingTextNode = newNode
                                                    showTextEditDialog = true
                                                },
                                                modifier = Modifier.weight(1.2f)
                                            ) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add Text", fontSize = 11.sp)
                                            }

                                            if (selectedTextNodeId != null) {
                                                Button(
                                                    onClick = { selectedTextNodeId = null },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                                    modifier = Modifier.height(36.dp).weight(0.8f)
                                                ) {
                                                    Text("Deselect", fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        if (selectedTextNodeId != null) {
                                            val activeNode = editorState.textNodes.firstOrNull { it.id == selectedTextNodeId }
                                            if (activeNode != null) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .verticalScroll(rememberScrollState()),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Font Families & Opacity row
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        listOf("Sans", "Serif", "Mono", "Cursive").forEach { font ->
                                                            val isSelected = activeNode.fontStyle == font
                                                            FilterChip(
                                                                selected = isSelected,
                                                                onClick = {
                                                                    triggerHaptic()
                                                                    val updated = editorState.textNodes.map {
                                                                        if (it.id == activeNode.id) it.copy(fontStyle = font) else it
                                                                    }
                                                                    updateStateAndCommit(editorState.copy(textNodes = updated))
                                                                },
                                                                label = { Text(font, fontSize = 10.sp) }
                                                            )
                                                        }
                                                    }

                                                    // Text alignment & Bold / Shade / Background row
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Toggle Bold
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("Bold", style = MaterialTheme.typography.labelSmall)
                                                            Checkbox(
                                                                checked = activeNode.isBold,
                                                                onCheckedChange = { bold ->
                                                                    triggerHaptic()
                                                                    val updated = editorState.textNodes.map {
                                                                        if (it.id == activeNode.id) it.copy(isBold = bold) else it
                                                                    }
                                                                    updateStateAndCommit(editorState.copy(textNodes = updated))
                                                                }
                                                            )
                                                        }

                                                        // Toggle Shade (Drop Shadow)
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("Shadow", style = MaterialTheme.typography.labelSmall)
                                                            Checkbox(
                                                                checked = activeNode.hasShadow,
                                                                onCheckedChange = { shadow ->
                                                                    triggerHaptic()
                                                                    val updated = editorState.textNodes.map {
                                                                        if (it.id == activeNode.id) it.copy(hasShadow = shadow) else it
                                                                    }
                                                                    updateStateAndCommit(editorState.copy(textNodes = updated))
                                                                }
                                                            )
                                                        }

                                                        // Toggle Box background
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("Box BG", style = MaterialTheme.typography.labelSmall)
                                                            Checkbox(
                                                                checked = activeNode.hasBackground,
                                                                onCheckedChange = { checked ->
                                                                    triggerHaptic()
                                                                    val updated = editorState.textNodes.map {
                                                                        if (it.id == activeNode.id) it.copy(hasBackground = checked) else it
                                                                    }
                                                                    updateStateAndCommit(editorState.copy(textNodes = updated))
                                                                }
                                                            )
                                                        }
                                                    }

                                                    // Text node color selection palette
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Text("Color", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                        listOf(Color.White, Color.Yellow, Color.Red, Color.Green, Color.Cyan, Color.Black).forEach { col ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(20.dp)
                                                                    .clip(CircleShape)
                                                                    .background(col)
                                                                    .border(if (activeNode.color == col) 2.dp else 0.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                                    .clickable {
                                                                        triggerHaptic()
                                                                        val updated = editorState.textNodes.map {
                                                                            if (it.id == activeNode.id) it.copy(color = col) else it
                                                                        }
                                                                        updateStateAndCommit(editorState.copy(textNodes = updated))
                                                                    }
                                                            )
                                                        }
                                                    }

                                                    // Opacity slider
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Opacity", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(64.dp))
                                                        Slider(
                                                            value = activeNode.opacity,
                                                            onValueChange = { op ->
                                                                val updated = editorState.textNodes.map {
                                                                    if (it.id == activeNode.id) it.copy(opacity = op) else it
                                                                }
                                                                editorState = editorState.copy(textNodes = updated)
                                                            },
                                                            onValueChangeFinished = { commitCurrentState() },
                                                            valueRange = 0.1f..1f,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Text("${(activeNode.opacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "Add a new text label to customize font families, outline stroke, shadow backdrop, opacity, and alignment, or double-tap to modify content.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Export Processing circular progress overlay
            if (isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Exporting High-Res Render...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Applying filters, graphics, and markups...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // --- SAVE OPTIONS AND MEDIASTORE FLOW DIALOG ---
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Export Modifications") },
            text = { Text("How would you like to save these edits? Overwriting updates the original item directly; Saving as Copy creates a brand new media entry in your photo feed.") },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            triggerHaptic()
                            showSaveDialog = false
                            isSaving = true
                            // Simulate background render processing delay
                            coroutineScope.launch {
                                delay(1000)
                                val newItem = mediaItem.copy(
                                    id = System.currentTimeMillis(),
                                    name = "Edited_${mediaItem.name}",
                                    dateAdded = System.currentTimeMillis() / 1000L,
                                    location = mediaItem.location ?: "Edited in Prism"
                                )
                                viewModel.insertEditedMediaItem(newItem)
                                isSaving = false
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_copy_confirm_button")
                    ) {
                        Text("Save as New Copy (Default)", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            triggerHaptic()
                            showSaveDialog = false
                            isSaving = true
                            coroutineScope.launch {
                                delay(1000)
                                val updatedItem = mediaItem.copy(
                                    location = mediaItem.location ?: "Edited in Prism"
                                )
                                viewModel.insertEditedMediaItem(updatedItem)
                                isSaving = false
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Overwrite Original Entry")
                    }
                }
            }
        )
    }

    // --- DYNAMIC DIALOG FOR EDITING TEXT STRINGS ---
    if (showTextEditDialog && editingTextNode != null) {
        var tempText by remember { mutableStateOf(editingTextNode!!.text) }
        AlertDialog(
            onDismissRequest = { showTextEditDialog = false },
            title = { Text("Edit Text Node Overlay") },
            text = {
                OutlinedTextField(
                    value = tempText,
                    onValueChange = { tempText = it },
                    label = { Text("Enter text label") },
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("edit_text_field")
                )
            },
            confirmButton = {
                Button(onClick = {
                    triggerHaptic()
                    val updatedNodes = editorState.textNodes.map {
                        if (it.id == editingTextNode!!.id) it.copy(text = tempText) else it
                    }
                    updateStateAndCommit(editorState.copy(textNodes = updatedNodes))
                    showTextEditDialog = false
                    editingTextNode = null
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Draw stroke helper handles Pen, Marker, and Neon Glow
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(
    stroke: DoodleStroke,
    blendMode: androidx.compose.ui.graphics.BlendMode
) {
    if (stroke.points.size < 2) return
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(stroke.points[0].x, stroke.points[0].y)
        // Path smoothing via quadratic Bezier interpolation curves
        for (i in 1 until stroke.points.size) {
            val p0 = stroke.points[i - 1]
            val p1 = stroke.points[i]
            val midX = (p0.x + p1.x) / 2
            val midY = (p0.y + p1.y) / 2
            quadraticTo(p0.x, p0.y, midX, midY)
        }
        lineTo(stroke.points.last().x, stroke.points.last().y)
    }

    when (stroke.brushType) {
        "Marker" -> {
            drawPath(
                path = path,
                color = stroke.color.copy(alpha = 0.45f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke.width,
                    cap = StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                ),
                blendMode = blendMode
            )
        }
        "Neon" -> {
            // Luminous thick backdrop
            drawPath(
                path = path,
                color = stroke.color.copy(alpha = 0.28f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke.width * 2.4f,
                    cap = StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                ),
                blendMode = blendMode
            )
            // Hot white core
            drawPath(
                path = path,
                color = Color.White,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke.width * 0.55f,
                    cap = StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                ),
                blendMode = blendMode
            )
        }
        "Pixelate" -> {
            // Drawn as blocky pixels using square dash path style
            drawPath(
                path = path,
                color = Color.Gray.copy(alpha = 0.92f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke.width * 1.5f,
                    cap = StrokeCap.Square,
                    join = androidx.compose.ui.graphics.StrokeJoin.Bevel,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                ),
                blendMode = blendMode
            )
        }
        "Blur" -> {
            // Frosted soft glass semi-transparent blur
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.55f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke.width * 1.8f,
                    cap = StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                ),
                blendMode = blendMode
            )
        }
        "Pattern" -> {
            // Pattern dotted line
            drawPath(
                path = path,
                color = stroke.color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke.width,
                    cap = StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 25f), 0f)
                ),
                blendMode = blendMode
            )
        }
        else -> {
            // Pen brush
            drawPath(
                path = path,
                color = stroke.color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke.width,
                    cap = StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                ),
                blendMode = blendMode
            )
        }
    }
}

@Composable
fun EditorTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .width(62.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PresetFilterThumbnail(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .width(80.dp)
            .height(95.dp)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (title) {
                            "Warm Glow" -> Color(0xFFE28B53)
                            "Cool Ice" -> Color(0xFF6FBCE4)
                            "Mono" -> Color(0xFF7F7F7F)
                            "Vintage" -> Color(0xFFB1A280)
                            "Dramatic" -> Color(0xFFB523D4)
                            "Sepia" -> Color(0xFF867252)
                            else -> Color(0xFF3F95D8)
                        }
                    )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AdjustmentSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(82.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = String.format("%.0f", value),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RotateActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

fun getCombinedColorMatrix(
    presetName: String,
    filterIntensity: Float,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
    tint: Float,
    exposure: Float,
    highlights: Float,
    shadows: Float,
    vibrance: Float
): ColorMatrix {
    val identity = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    val base = when (presetName) {
        "portrait" -> floatArrayOf(
            1.15f, 0.05f, 0f, 0f, 10f,
            0f, 1.08f, 0f, 0f, 5f,
            0f, 0f, 0.9f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
        )
        "landscape" -> floatArrayOf(
            1.2f, 0f, 0.05f, 0f, 5f,
            0.05f, 1.25f, 0f, 0f, 8f,
            0f, 0.05f, 1.3f, 0f, 12f,
            0f, 0f, 0f, 1f, 0f
        )
        "food" -> floatArrayOf(
            1.25f, 0.1f, 0f, 0f, 15f,
            0.1f, 1.15f, 0f, 0f, 10f,
            0f, 0f, 0.85f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        )
        "night" -> floatArrayOf(
            0.85f, 0f, 0f, 0f, -15f,
            0f, 0.85f, 0.1f, 0f, -10f,
            0.1f, 0f, 1.05f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
        )
        "japanese" -> floatArrayOf(
            0.95f, 0.1f, 0.05f, 0f, 5f,
            0.1f, 0.95f, 0.05f, 0f, 10f,
            0.05f, 0.05f, 1.05f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
        )
        "film" -> floatArrayOf(
            0.9f, 0.1f, 0.1f, 0f, 8f,
            0.1f, 0.85f, 0.1f, 0f, 5f,
            0.05f, 0.1f, 0.8f, 0f, 2f,
            0f, 0f, 0f, 1f, 0f
        )
        "monochrome" -> floatArrayOf(
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        "warm" -> floatArrayOf(
            1.25f, 0f, 0f, 0f, 15f,
            0f, 1.1f, 0f, 0f, 8f,
            0f, 0f, 0.75f, 0f, -15f,
            0f, 0f, 0f, 1f, 0f
        )
        "cool" -> floatArrayOf(
            0.75f, 0f, 0f, 0f, -15f,
            0f, 0.9f, 0f, 0f, -8f,
            0f, 0f, 1.25f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
        )
        "vintage" -> floatArrayOf(
            0.9f, 0.3f, 0.15f, 0f, 0f,
            0.15f, 0.8f, 0.2f, 0f, 0f,
            0.15f, 0.15f, 0.7f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        "dramatic" -> floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
        "sepia" -> floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        else -> identity
    }

    val m = FloatArray(20)
    for (i in 0 until 20) {
        m[i] = identity[i] * (1f - filterIntensity) + base[i] * filterIntensity
    }

    if (brightness != 0f) {
        val shift = (brightness / 100f) * 80f
        m[4] += shift
        m[9] += shift
        m[14] += shift
    }

    if (contrast != 0f) {
        val cScale = if (contrast >= 0) 1f + (contrast / 100f) else 1f + (contrast / 200f)
        m[0] *= cScale
        m[1] *= cScale
        m[2] *= cScale
        m[5] *= cScale
        m[6] *= cScale
        m[7] *= cScale
        m[10] *= cScale
        m[11] *= cScale
        m[12] *= cScale
        val trans = 128f * (1f - cScale)
        m[4] += trans
        m[9] += trans
        m[14] += trans
    }

    val totalSat = saturation + vibrance * 0.5f
    if (totalSat != 0f) {
        val sat = if (totalSat >= 0) 1f + (totalSat / 100f) else 1f + (totalSat / 100f)
        val lr = 0.213f
        val lg = 0.715f
        val lb = 0.072f
        val r = (1f - sat) * lr
        val g = (1f - sat) * lg
        val b = (1f - sat) * lb

        val satMatrix = floatArrayOf(
            r + sat, g, b, 0f, 0f,
            r, g + sat, b, 0f, 0f,
            r, g, b + sat, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )

        val out = FloatArray(20)
        for (row in 0..2) {
            for (col in 0..4) {
                var sum = 0f
                for (k in 0..2) {
                    sum += satMatrix[row * 5 + k] * m[k * 5 + col]
                }
                if (col == 4) {
                    sum += satMatrix[row * 5 + 4]
                }
                out[row * 5 + col] = sum
            }
        }
        for (i in 0..14) {
            m[i] = out[i]
        }
    }

    if (exposure != 0f) {
        val eScale = if (exposure >= 0) 1f + (exposure / 100f) * 0.7f else 1f + (exposure / 100f) * 0.4f
        m[0] *= eScale
        m[5] *= eScale
        m[10] *= eScale
    }

    if (warmth != 0f) {
        val wVal = (warmth / 100f) * 30f
        m[4] += wVal
        m[9] += wVal * 0.5f
        m[14] -= wVal
    }

    if (tint != 0f) {
        val tVal = (tint / 100f) * 25f
        m[9] += tVal
        m[4] -= tVal * 0.5f
        m[14] -= tVal * 0.5f
    }

    if (highlights != 0f) {
        val hVal = (highlights / 100f) * 15f
        m[4] += hVal
        m[9] += hVal
        m[14] += hVal
    }

    if (shadows != 0f) {
        val sVal = (shadows / 100f) * 20f
        m[4] += sVal
        m[9] += sVal
        m[14] += sVal
    }

    return ColorMatrix(m)
}
