package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.example.viewmodel.GalleryViewModel
import com.example.util.BiometricHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()

    // Active sub-page state: null means main page, otherwise "appearance", "timeline", "viewer", "navigation", "general", "security"
    var activePage by remember { mutableStateOf<String?>(null) }

    fun triggerHaptic() {
        if (isVibrationEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // MAIN SETTINGS SCREEN
        AnimatedVisibility(
            visible = activePage == null,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it })
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Settings", fontWeight = FontWeight.Bold, modifier = Modifier.testTag("settings_title")) },
                        navigationIcon = {
                            IconButton(onClick = {
                                triggerHaptic()
                                onBack()
                            }) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                contentWindowInsets = WindowInsets.safeDrawing,
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .testTag("settings_container"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Header Section: App Info Header & GitHub Button
                    AppInfoHeader(
                        appName = "Prism v5.0.3",
                        author = "By Saurav",
                        onGithubClick = {
                            triggerHaptic()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"))
                            context.startActivity(intent)
                        }
                    )

                    // 2. Category List (Card-based UI)
                    SettingsCategoryHeader("Interface Styles")
                    CategoryCard(
                        title = "Appearance",
                        description = "Theme, dark mode accent, wallpapers & transitions",
                        icon = Icons.Outlined.Palette,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = {
                            triggerHaptic()
                            activePage = "appearance"
                        }
                    )

                    SettingsCategoryHeader("Media Arrangement")
                    CategoryCard(
                        title = "Timeline & Albums",
                        description = "Grid custom styling, grouping, folders sections",
                        icon = Icons.Outlined.Timeline,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            triggerHaptic()
                            activePage = "timeline"
                        }
                    )

                    CategoryCard(
                        title = "Media Viewer",
                        description = "Full screen brightness, video player preferences",
                        icon = Icons.Outlined.Visibility,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        onClick = {
                            triggerHaptic()
                            activePage = "viewer"
                        }
                    )

                    SettingsCategoryHeader("Navigation & Controls")
                    CategoryCard(
                        title = "Navigation",
                        description = "Pill layout configuration, scroll behaviors, defaults",
                        icon = Icons.Outlined.Navigation,
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = {
                            triggerHaptic()
                            activePage = "navigation"
                        }
                    )

                    SettingsCategoryHeader("Core & Security")
                    CategoryCard(
                        title = "General",
                        description = "Recycle bin lifecycle, haptics, recents preview safety",
                        icon = Icons.Outlined.Settings,
                        iconTint = Color(0xFF4CAF50),
                        onClick = {
                            triggerHaptic()
                            activePage = "general"
                        }
                    )

                    CategoryCard(
                        title = "Security & Vault",
                        description = "Private folder biometric lock, metadata isolation sandbox",
                        icon = Icons.Outlined.Security,
                        iconTint = Color(0xFFFF9800),
                        onClick = {
                            triggerHaptic()
                            activePage = "security"
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Prism Photos • Fully Modular Secure Gallery\nNo Cloud Syncing • Zero External Telemetry",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ------------------ SUB-PAGES NAVIGATION ------------------

        // APPEARANCE SUB-PAGE
        AnimatedVisibility(
            visible = activePage == "appearance",
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
        ) {
            AppearanceSettingsPage(
                viewModel = viewModel,
                onBack = {
                    triggerHaptic()
                    activePage = null
                }
            )
        }

        // TIMELINE & ALBUMS SUB-PAGE
        AnimatedVisibility(
            visible = activePage == "timeline",
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
        ) {
            TimelineSettingsPage(
                viewModel = viewModel,
                onBack = {
                    triggerHaptic()
                    activePage = null
                }
            )
        }

        // MEDIA VIEWER SUB-PAGE
        AnimatedVisibility(
            visible = activePage == "viewer",
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
        ) {
            MediaViewerSettingsPage(
                viewModel = viewModel,
                onBack = {
                    triggerHaptic()
                    activePage = null
                }
            )
        }

        // NAVIGATION SUB-PAGE
        AnimatedVisibility(
            visible = activePage == "navigation",
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
        ) {
            NavigationSettingsPage(
                viewModel = viewModel,
                onBack = {
                    triggerHaptic()
                    activePage = null
                }
            )
        }

        // GENERAL SUB-PAGE
        AnimatedVisibility(
            visible = activePage == "general",
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
        ) {
            GeneralSettingsPage(
                viewModel = viewModel,
                onBack = {
                    triggerHaptic()
                    activePage = null
                }
            )
        }

        // SECURITY SUB-PAGE
        AnimatedVisibility(
            visible = activePage == "security",
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
        ) {
            SecuritySettingsPage(
                viewModel = viewModel,
                onBack = {
                    triggerHaptic()
                    activePage = null
                }
            )
        }
    }
}

// =========================================
// MAIN PAGE SUB-COMPONENTS
// =========================================

@Composable
fun AppInfoHeader(
    appName: String,
    author: String,
    onGithubClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "Premium design crafted for blazing-fast local navigation, offline privacy protection, and granular visual customizations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Button(
                onClick = onGithubClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(44.dp)
            ) {
                Icon(imageVector = Icons.Default.Code, contentDescription = "Code icon", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Github Repository", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun CategoryCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


// =========================================
// 1. APPEARANCE SETTINGS SUB-PAGE
// =========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsPage(
    viewModel: GalleryViewModel,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val colorPalette by viewModel.colorPalette.collectAsState()
    val followSystemTheme by viewModel.followSystemTheme.collectAsState()
    val useDarkMode by viewModel.useDarkMode.collectAsState()
    val useAmoledMode by viewModel.useAmoledMode.collectAsState()
    val fancyBlur by viewModel.fancyBlur.collectAsState()
    val autoContrast by viewModel.autoContrast.collectAsState()
    val animateMediaItems by viewModel.animateMediaItems.collectAsState()
    val useSystemFont by viewModel.useSystemFont.collectAsState()

    fun triggerHaptic() {
        if (viewModel.isVibrationEnabled.value) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Color Palette Section
            Text("Color Palette Accent", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select a custom theme tint. Choose wallpaper colors or handcrafted accents.", style = MaterialTheme.typography.bodyMedium)
                    
                    val palettes = listOf(
                        Triple("wallpaper", "Wallpaper Colors", Color(0xFF673AB7)),
                        Triple("emerald", "Emerald Green", Color(0xFF009688)),
                        Triple("ruby", "Ruby Red", Color(0xFFE91E63)),
                        Triple("sapphire", "Classic Blue", Color(0xFF2196F3)),
                        Triple("amber", "Golden Amber", Color(0xFFFF9800))
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(palettes) { (id, name, color) ->
                            val isSelected = colorPalette == id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        triggerHaptic()
                                        viewModel.setColorPalette(id)
                                    }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(name.split(" ")[0], style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            // Theme Configuration Card
            Text("Theme Configuration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    SubPreferenceToggle(
                        title = "Follow System Theme",
                        subtitle = "Sync display modes directly with system configuration",
                        icon = Icons.Outlined.BrightnessAuto,
                        checked = followSystemTheme,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setFollowSystemTheme(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Use Dark Mode",
                        subtitle = "Enable night styling (overridden by system sync if active)",
                        icon = Icons.Outlined.DarkMode,
                        checked = useDarkMode,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setUseDarkMode(it)
                        },
                        enabled = !followSystemTheme
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Use AMOLED Mode",
                        subtitle = "Deep true blacks for OLED panels to optimize power consumption",
                        icon = Icons.Outlined.FlashOn,
                        checked = useAmoledMode,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setUseAmoledMode(it)
                        }
                    )
                }
            }

            // Visual Effects Card
            Text("Visual Effects", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    SubPreferenceToggle(
                        title = "Fancy Blur Background",
                        subtitle = "Render rich, blurred photo backdrops during viewer displays",
                        icon = Icons.Outlined.BlurOn,
                        checked = fancyBlur,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setFancyBlur(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Auto-Contrast Magic",
                        subtitle = "Dynamically boost dynamic range when inspecting detailed images",
                        icon = Icons.Outlined.Contrast,
                        checked = autoContrast,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setAutoContrast(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Animate Media Items",
                        subtitle = "Smooth shared element animation zooms when opening assets",
                        icon = Icons.Outlined.Animation,
                        checked = animateMediaItems,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setAnimateMediaItems(it)
                        }
                    )
                }
            }

            // Typography Card
            Text("Typography", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                SubPreferenceToggle(
                    title = "Use System Font",
                    subtitle = "Prefer default device typefaces instead of Sans Grotesk/Mono pairings",
                    icon = Icons.Outlined.FontDownload,
                    checked = useSystemFont,
                    onCheckedChange = {
                        triggerHaptic()
                        viewModel.setUseSystemFont(it)
                    }
                )
            }
        }
    }
}


// =========================================
// 2. TIMELINE & ALBUMS SETTINGS SUB-PAGE
// =========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineSettingsPage(
    viewModel: GalleryViewModel,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val gridMode by viewModel.gridMode.collectAsState()
    val groupSimilarPhotos by viewModel.groupSimilarPhotos.collectAsState()
    val animateGifs by viewModel.animateGifs.collectAsState()
    val dateFormat by viewModel.dateFormat.collectAsState()
    val showFilterButton by viewModel.showFilterButton.collectAsState()
    val showFavoriteButton by viewModel.showFavoriteButton.collectAsState()
    val storyCardsEnabled by viewModel.storyCardsEnabled.collectAsState()
    val hideTimelineForAlbums by viewModel.hideTimelineForAlbums.collectAsState()
    val mergeAlbumsByName by viewModel.mergeAlbumsByName.collectAsState()
    val organizeAlbumsSections by viewModel.organizeAlbumsSections.collectAsState()
    val hideEmptySystemAlbums by viewModel.hideEmptySystemAlbums.collectAsState()
    val dateSeparators by viewModel.dateSeparators.collectAsState()
    val mediaGrouping by viewModel.mediaGrouping.collectAsState()

    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showMediaGroupingDialog by remember { mutableStateOf(false) }

    fun triggerHaptic() {
        if (viewModel.isVibrationEnabled.value) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timeline & Albums", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timeline Group
            Text("Timeline Parameters", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    SubPreferenceRow(
                        title = "Timeline Layout Style",
                        subtitle = "Change grid view shapes",
                        icon = Icons.Outlined.GridView,
                        actionText = gridMode.replaceFirstChar { it.uppercase() },
                        onClick = {
                            triggerHaptic()
                            viewModel.setGridMode(if (gridMode == "square") "mosaic" else "square")
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Group Similar Photos",
                        subtitle = "Cluster near-identical bursts to minimize list length",
                        icon = Icons.Outlined.Collections,
                        checked = groupSimilarPhotos,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setGroupSimilarPhotos(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Animate GIFs in Grid",
                        subtitle = "Play dynamic animations continuously inside standard previews",
                        icon = Icons.Outlined.Gif,
                        checked = animateGifs,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setAnimateGifs(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceRow(
                        title = "Date Format Layout",
                        subtitle = "Specify visual style for headers",
                        icon = Icons.Outlined.CalendarToday,
                        actionText = dateFormat,
                        onClick = {
                            triggerHaptic()
                            showDateFormatDialog = true
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Display Filter Button",
                        subtitle = "Overlay shortcut filters in search timelines",
                        icon = Icons.Outlined.FilterList,
                        checked = showFilterButton,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setShowFilterButton(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Display Favorite Icons",
                        subtitle = "Show explicit heart indicators on timeline cards",
                        icon = Icons.Outlined.Favorite,
                        checked = showFavoriteButton,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setShowFavoriteButton(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Enable Story Cards",
                        subtitle = "Show smart historical 'One Year Ago' preview headers",
                        icon = Icons.Outlined.AutoAwesome,
                        checked = storyCardsEnabled,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setStoryCardsEnabled(it)
                        }
                    )
                }
            }

            // Albums Group
            Text("Albums Classification", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    SubPreferenceToggle(
                        title = "Hide Timeline for Albums",
                        subtitle = "Bypass device photos grid directly into folder directories",
                        icon = Icons.Outlined.FolderOff,
                        checked = hideTimelineForAlbums,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setHideTimelineForAlbums(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Merge Duplicate Names",
                        subtitle = "Consolidate identically labeled storage directories automatically",
                        icon = Icons.Outlined.MergeType,
                        checked = mergeAlbumsByName,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setMergeAlbumsByName(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Organize Albums by Sections",
                        subtitle = "Sort folder directories into distinct system & user headings",
                        icon = Icons.Outlined.ViewStream,
                        checked = organizeAlbumsSections,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setOrganizeAlbumsSections(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Hide Empty System Albums",
                        subtitle = "Do not display predefined default albums if they contain no media assets",
                        icon = Icons.Outlined.FolderOff,
                        checked = hideEmptySystemAlbums,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setHideEmptySystemAlbums(it)
                        }
                    )
                }
            }

            // Display Group
            Text("Display Parameters", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    SubPreferenceToggle(
                        title = "Render Date Separators",
                        subtitle = "Create clean margins and chronological label boundaries",
                        icon = Icons.Outlined.ViewAgenda,
                        checked = dateSeparators,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setDateSeparators(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceRow(
                        title = "Timeline Grouping",
                        subtitle = "Categorize primary feed timeline",
                        icon = Icons.Outlined.FolderCopy,
                        actionText = mediaGrouping.replaceFirstChar { it.uppercase() },
                        onClick = {
                            triggerHaptic()
                            showMediaGroupingDialog = true
                        }
                    )
                }
            }
        }
    }

    // Interactive Option Selection Dialogs
    if (showDateFormatDialog) {
        OptionSelectionDialog(
            title = "Choose Date Format",
            options = listOf(
                Pair("MMM dd, yyyy", "Standard (Jul 01, 2026)"),
                Pair("yyyy-MM-dd", "ISO (2026-07-01)"),
                Pair("dd/MM/yyyy", "European (01/07/2026)"),
                Pair("EEEE, MMM d", "Full Day (Wednesday, Jul 1)")
            ),
            selectedOption = dateFormat,
            onDismiss = { showDateFormatDialog = false },
            onSelect = {
                viewModel.setDateFormat(it)
                showDateFormatDialog = false
            }
        )
    }

    if (showMediaGroupingDialog) {
        OptionSelectionDialog(
            title = "Select Chronological Grouping",
            options = listOf(
                Pair("date", "By Captured Calendar Date"),
                Pair("location", "By Location EXIF Coordinates"),
                Pair("type", "By Media Format Classifications")
            ),
            selectedOption = mediaGrouping,
            onDismiss = { showMediaGroupingDialog = false },
            onSelect = {
                viewModel.setMediaGrouping(it)
                showMediaGroupingDialog = false
            }
        )
    }
}


// =========================================
// 3. MEDIA VIEWER SETTINGS SUB-PAGE
// =========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerSettingsPage(
    viewModel: GalleryViewModel,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val fullBrightness by viewModel.fullBrightness.collectAsState()
    val showMediaHeader by viewModel.showMediaHeader.collectAsState()
    val showFavoriteInViewer by viewModel.showFavoriteInViewer.collectAsState()
    val defaultEditor by viewModel.defaultEditor.collectAsState()
    val hideUiOnVideoPlay by viewModel.hideUiOnVideoPlay.collectAsState()
    val videoAutoPlay by viewModel.videoAutoPlay.collectAsState()
    val muteVideoByDefault by viewModel.muteVideoByDefault.collectAsState()

    var showEditorDialog by remember { mutableStateOf(false) }

    fun triggerHaptic() {
        if (viewModel.isVibrationEnabled.value) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Viewer Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Media View Group
            Text("Viewer Interface", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    SubPreferenceToggle(
                        title = "Force Full Brightness",
                        subtitle = "Boost screen brightness to 100% when reviewing photos",
                        icon = Icons.Outlined.BrightnessHigh,
                        checked = fullBrightness,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setFullBrightness(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Show Media Date Header",
                        subtitle = "Display date and time captions at the viewer top margin",
                        icon = Icons.Outlined.Label,
                        checked = showMediaHeader,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setShowMediaHeader(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Favorite Button Overlay",
                        subtitle = "Include an express toggle heart directly inside viewers",
                        icon = Icons.Outlined.FavoriteBorder,
                        checked = showFavoriteInViewer,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setShowFavoriteInViewer(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceRow(
                        title = "Preferred Image Editor",
                        subtitle = "Specify editing pipeline route",
                        icon = Icons.Outlined.Edit,
                        actionText = if (defaultEditor == "default") "Prism Native" else "External",
                        onClick = {
                            triggerHaptic()
                            showEditorDialog = true
                        }
                    )
                }
            }

            // Video Playback Group
            Text("Video Playback", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    SubPreferenceToggle(
                        title = "Hide UI Controls on Play",
                        subtitle = "Auto-hide back buttons and toolbars when playing videos",
                        icon = Icons.Outlined.PlayCircleOutline,
                        checked = hideUiOnVideoPlay,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setHideUiOnVideoPlay(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Auto-Play Videos",
                        subtitle = "Instantly play local video files without tapping the trigger",
                        icon = Icons.Outlined.SlowMotionVideo,
                        checked = videoAutoPlay,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setVideoAutoPlay(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Mute Videos by Default",
                        subtitle = "Initially mute audio streams on all video file playback",
                        icon = Icons.Outlined.VolumeMute,
                        checked = muteVideoByDefault,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setMuteVideoByDefault(it)
                        }
                    )
                }
            }
        }
    }

    if (showEditorDialog) {
        OptionSelectionDialog(
            title = "Default Image Editor",
            options = listOf(
                Pair("default", "Prism Native (Sliders, Mosaic, Doodles, Text)"),
                Pair("external", "External System Application (Google Photos, Snapseed)")
            ),
            selectedOption = defaultEditor,
            onDismiss = { showEditorDialog = false },
            onSelect = {
                viewModel.setDefaultEditor(it)
                showEditorDialog = false
            }
        )
    }
}


// =========================================
// 4. NAVIGATION SETTINGS SUB-PAGE
// =========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationSettingsPage(
    viewModel: GalleryViewModel,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val defaultLaunchScreen by viewModel.defaultLaunchScreen.collectAsState()
    val hideSearchBarOnScroll by viewModel.hideSearchBarOnScroll.collectAsState()
    val hideNavBarOnScroll by viewModel.hideNavBarOnScroll.collectAsState()
    val displayItemTitle by viewModel.displayItemTitle.collectAsState()
    val customSelectionActions by viewModel.customSelectionActions.collectAsState()
    val reorderFloatingActions by viewModel.reorderFloatingActions.collectAsState()

    var showLaunchScreenDialog by remember { mutableStateOf(false) }

    fun triggerHaptic() {
        if (viewModel.isVibrationEnabled.value) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navigation Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Floating Pill Navigation Card
            Text("Floating Bottom Pill Layout", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    SubPreferenceRow(
                        title = "Launch screen destination",
                        subtitle = "Select initial route on startup",
                        icon = Icons.Outlined.Home,
                        actionText = defaultLaunchScreen.replaceFirstChar { it.uppercase() },
                        onClick = {
                            triggerHaptic()
                            showLaunchScreenDialog = true
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Hide Search Bar on Scroll",
                        subtitle = "Collapse top search bar layout during scroll actions to enlarge visible workspace",
                        icon = Icons.Outlined.SearchOff,
                        checked = hideSearchBarOnScroll,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setHideSearchBarOnScroll(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Hide Bottom Pill on Scroll",
                        subtitle = "Slide floating navigation pill off-screen when scrolling down feed",
                        icon = Icons.Outlined.ViewStream,
                        checked = hideNavBarOnScroll,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setHideNavBarOnScroll(it)
                        }
                    )
                }
            }

            // Interface Preferences Card
            Text("Interface Preferences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    SubPreferenceToggle(
                        title = "Display Item Title",
                        subtitle = "Show small text label filename overlays directly on grid items",
                        icon = Icons.Outlined.Subtitles,
                        checked = displayItemTitle,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setDisplayItemTitle(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Custom Selection Actions",
                        subtitle = "Modify standard long-press floating utility rows with custom presets",
                        icon = Icons.Outlined.TouchApp,
                        checked = customSelectionActions,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setCustomSelectionActions(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Reorder Floating Actions",
                        subtitle = "Enable drag-reordering of buttons inside the floating selection bar",
                        icon = Icons.Outlined.FormatLineSpacing,
                        checked = reorderFloatingActions,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setReorderFloatingActions(it)
                        }
                    )
                }
            }
        }
    }

    if (showLaunchScreenDialog) {
        OptionSelectionDialog(
            title = "Startup Launch Destination",
            options = listOf(
                Pair("photos", "Primary Timeline Feed"),
                Pair("albums", "Folders & Utilities View"),
                Pair("search", "Advanced Search Directory")
            ),
            selectedOption = defaultLaunchScreen,
            onDismiss = { showLaunchScreenDialog = false },
            onSelect = {
                viewModel.setDefaultLaunchScreen(it)
                showLaunchScreenDialog = false
            }
        )
    }
}


// =========================================
// 5. GENERAL SETTINGS SUB-PAGE
// =========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsPage(
    viewModel: GalleryViewModel,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val useTrashCan by viewModel.useTrashCan.collectAsState()
    val isTrashConfirmationEnabled by viewModel.isTrashConfirmationEnabled.collectAsState()
    val doubleConfirmTrash by viewModel.doubleConfirmTrash.collectAsState()
    val secureMode by viewModel.secureMode.collectAsState()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()

    fun triggerHaptic() {
        if (isVibrationEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trash Group Card
            Text("Recycle Bin Policies", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    SubPreferenceToggle(
                        title = "Use Trash Can",
                        subtitle = "Hold deleted images in a 30-day temporal safety retention sandbox before permanently cleaning",
                        icon = Icons.Outlined.DeleteOutline,
                        checked = useTrashCan,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setUseTrashCan(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Require Trash Confirmations",
                        subtitle = "Enforce explicit alerts when moving assets to trash can",
                        icon = Icons.Outlined.ReportGmailerrorred,
                        checked = isTrashConfirmationEnabled,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setTrashConfirmationEnabled(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Double Confirm Permanent Emptying",
                        subtitle = "Enforce a secondary verification prompt before executing irreversible hard-deletes of trash contents",
                        icon = Icons.Outlined.Warning,
                        checked = doubleConfirmTrash,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setDoubleConfirmTrash(it)
                        }
                    )
                }
            }

            // Other Preferences Card
            Text("Device Preferences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    SubPreferenceToggle(
                        title = "Secure Mode (Hide Preview)",
                        subtitle = "Obscure the gallery viewport and disable screenshots inside Android task lists",
                        icon = Icons.Outlined.ScreenLockPortrait,
                        checked = secureMode,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setSecureMode(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Tactile Vibration Haptics",
                        subtitle = "Execute localized physical key hums during screen interactions",
                        icon = Icons.Outlined.Vibration,
                        checked = isVibrationEnabled,
                        onCheckedChange = {
                            viewModel.setVibrationEnabled(it)
                            if (it) triggerHaptic()
                        }
                    )
                }
            }
        }
    }
}


// =========================================
// 6. SECURITY & VAULT SETTINGS SUB-PAGE
// =========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsPage(
    viewModel: GalleryViewModel,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val metadataIsolation by viewModel.metadataIsolation.collectAsState()
    val sandboxedDecoding by viewModel.sandboxedDecoding.collectAsState()
    val useBiometricsForVault by viewModel.useBiometricsForVault.collectAsState()
    val vaultPin by viewModel.vaultPin.collectAsState()
    val vaultTriggerShortcut by viewModel.vaultTriggerShortcut.collectAsState()
    val revokeClipboardOnPause by viewModel.revokeClipboardOnPause.collectAsState()

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showShortcutDialog by remember { mutableStateOf(false) }
    var securityFeedbackMessage by remember { mutableStateOf("") }

    fun triggerHaptic() {
        if (viewModel.isVibrationEnabled.value) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security & Vault", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sandbox Settings
            Text("Sandbox Security Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    SubPreferenceToggle(
                        title = "EXIF Metadata Isolation Mode",
                        subtitle = "Strip geographic position GPS vectors and camera details when sharing media items",
                        icon = Icons.Outlined.GpsOff,
                        checked = metadataIsolation,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setMetadataIsolation(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Sandboxed Image Decoding",
                        subtitle = "Decode binary image vectors in quarantined threads to neutralize buffer vulnerabilities",
                        icon = Icons.Outlined.Code,
                        checked = sandboxedDecoding,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setSandboxedDecoding(it)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
                    SubPreferenceToggle(
                        title = "Revoke Clipboard on App Pause",
                        subtitle = "Securely clear system clipboard of copied media paths when Prism goes into background",
                        icon = Icons.Outlined.ContentPaste,
                        checked = revokeClipboardOnPause,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setRevokeClipboardOnPause(it)
                        }
                    )
                }
            }

            // Data Protection Settings
            Text("Data Encryption & Vault", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column {
                    // Encryption Status Display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.Https, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Local Encryption Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text("Vault data utilizes AES-256 cipher streams inside private storage", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                            contentColor = Color(0xFF4CAF50)
                        ) {
                            Text("AES-256", fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Biometrics Toggle
                    SubPreferenceToggle(
                        title = "Use Device Biometrics",
                        subtitle = "Access private folder instantly via system Fingerprint or Face biometric prompt",
                        icon = Icons.Outlined.Fingerprint,
                        checked = useBiometricsForVault,
                        onCheckedChange = {
                            triggerHaptic()
                            viewModel.setUseBiometricsForVault(it)
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))

                    // PIN Customization
                    SubPreferenceRow(
                        title = "Configure Backup Secure PIN",
                        subtitle = if (vaultPin.isEmpty()) "Secure folder setup required (Click here)" else "Backup PIN fully active",
                        icon = Icons.Outlined.LockClock,
                        actionText = if (vaultPin.isEmpty()) "Setup" else "Modify",
                        onClick = {
                            triggerHaptic()
                            showPinSetupDialog = true
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Secret Biometric Shortcut
                    SubPreferenceRow(
                        title = "Secret Biometric Shortcut",
                        subtitle = "Configure trigger gesture on Albums navigation button",
                        icon = Icons.Outlined.Gesture,
                        actionText = if (vaultTriggerShortcut == "double_tap") "Double-Tap" else "Long-Press",
                        onClick = {
                            triggerHaptic()
                            showShortcutDialog = true
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Test Biometric Setup Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                triggerHaptic()
                                if (activity != null) {
                                    BiometricHelper.showBiometricPrompt(
                                        activity = activity,
                                        title = "Security Demonstration",
                                        subtitle = "Testing communication with system biometric secure layer",
                                        onSuccess = {
                                            securityFeedbackMessage = "SUCCESS: Authenticated correctly"
                                        },
                                        onError = { error ->
                                            securityFeedbackMessage = "BIOMETRIC INFO: $error (Fallback PIN active)"
                                        }
                                    )
                                } else {
                                    securityFeedbackMessage = "Biometric prompt requires active context"
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Verify Secure Authenticator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text("Tap here to run a biometric handshake test against local keystore hardware", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (securityFeedbackMessage.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = securityFeedbackMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // PIN Configuration dialog
    if (showPinSetupDialog) {
        PinSetupDialog(
            currentPin = vaultPin,
            onDismiss = { showPinSetupDialog = false },
            onConfirm = { pin ->
                viewModel.setVaultPin(pin)
                showPinSetupDialog = false
                securityFeedbackMessage = "PIN securely configured"
            }
        )
    }

    // Shortcut Configuration dialog
    if (showShortcutDialog) {
        OptionSelectionDialog(
            title = "Secret Biometric Shortcut",
            options = listOf(
                Pair("double_tap", "Double-Tap Albums Button (Active state)"),
                Pair("long_press", "Long-Press Albums Button (Any state)")
            ),
            selectedOption = vaultTriggerShortcut,
            onDismiss = { showShortcutDialog = false },
            onSelect = {
                viewModel.setVaultTriggerShortcut(it)
                showShortcutDialog = false
            }
        )
    }
}


// =========================================
// SUB-PAGE ATOM DRAWING HELPERS
// =========================================

@Composable
fun SubPreferenceToggle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun SubPreferenceRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (actionText != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsCategoryHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun OptionSelectionDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedOption: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (value, label) ->
                        val isSelected = value == selectedOption
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(value) }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(selected = isSelected, onClick = { onSelect(value) })
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun PinSetupDialog(
    currentPin: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pinValue by remember { mutableStateOf("") }
    var confirmPinValue by remember { mutableStateOf("") }
    var isConfirmStage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isConfirmStage) "Confirm Secure PIN" else "Enter New 4-Digit PIN",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                val displayValue = if (isConfirmStage) confirmPinValue else pinValue

                // Show dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    for (i in 0 until 4) {
                        val active = i < displayValue.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        )
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                // keypad row by row
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    keys.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable {
                                            when (key) {
                                                "C" -> {
                                                    if (isConfirmStage) confirmPinValue = "" else pinValue = ""
                                                    errorMessage = ""
                                                }
                                                "⌫" -> {
                                                    if (isConfirmStage) {
                                                        if (confirmPinValue.isNotEmpty()) confirmPinValue = confirmPinValue.dropLast(1)
                                                    } else {
                                                        if (pinValue.isNotEmpty()) pinValue = pinValue.dropLast(1)
                                                    }
                                                    errorMessage = ""
                                                }
                                                else -> {
                                                    if (isConfirmStage) {
                                                        if (confirmPinValue.length < 4) {
                                                            confirmPinValue += key
                                                            if (confirmPinValue.length == 4) {
                                                                if (confirmPinValue == pinValue) {
                                                                    onConfirm(pinValue)
                                                                } else {
                                                                    errorMessage = "PINs do not match! Restarting setup."
                                                                    isConfirmStage = false
                                                                    pinValue = ""
                                                                    confirmPinValue = ""
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        if (pinValue.length < 4) {
                                                            pinValue += key
                                                            if (pinValue.length == 4) {
                                                                isConfirmStage = true
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
