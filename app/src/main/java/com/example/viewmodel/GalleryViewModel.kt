package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediaRepository
import com.example.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ThumbnailBounds(
    val id: Long,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

sealed interface GalleryUiState {
    object Loading : GalleryUiState
    data class Success(
        val groupedMedia: Map<String, List<MediaItem>>,
        val allMedia: List<MediaItem>
    ) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
}

data class AlbumFolder(
    val name: String,
    val items: List<MediaItem>
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application.applicationContext)
    private val prefs = application.getSharedPreferences("prism_gallery_prefs", Application.MODE_PRIVATE)

    // Map to track the window bounds of the thumbnails in the grid for Shared Element Transition
    private val _thumbnailBoundsMap = MutableStateFlow<Map<Long, ThumbnailBounds>>(emptyMap())
    val thumbnailBoundsMap: StateFlow<Map<Long, ThumbnailBounds>> = _thumbnailBoundsMap.asStateFlow()

    fun updateThumbnailBounds(id: Long, x: Float, y: Float, width: Float, height: Float) {
        _thumbnailBoundsMap.value = _thumbnailBoundsMap.value + (id to ThumbnailBounds(id, x, y, width, height))
    }

    // Raw list of loaded media items
    private val _rawMediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val rawMediaItems: StateFlow<List<MediaItem>> = _rawMediaItems.asStateFlow()

    // Loading status
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Search query for photos/videos filtering (by name, format, or location EXIF)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active bottom navigation tab index (0 = Photos, 1 = Search/Albums, 2 = Library)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Dynamic grid column span count, defaults to 3 columns
    private val _columnCount = MutableStateFlow(prefs.getInt("column_count", 3))
    val columnCount: StateFlow<Int> = _columnCount.asStateFlow()

    // Set of IDs of favorited items (since this is client-side, we store favorites in state)
    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds.asStateFlow()

    // --- MODULE 5: SETTINGS CONFIGURATION ENGINE ---
    private val _isVibrationEnabled = MutableStateFlow(prefs.getBoolean("is_vibration_enabled", true))
    val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()

    private val _isTrashConfirmationEnabled = MutableStateFlow(prefs.getBoolean("is_trash_confirmation_enabled", true))
    val isTrashConfirmationEnabled: StateFlow<Boolean> = _isTrashConfirmationEnabled.asStateFlow()

    private val _isSystemTrashEnabled = MutableStateFlow(prefs.getBoolean("is_system_trash_enabled", true))
    val isSystemTrashEnabled: StateFlow<Boolean> = _isSystemTrashEnabled.asStateFlow()

    private val _gridMode = MutableStateFlow(prefs.getString("grid_mode", "square") ?: "square")
    val gridMode: StateFlow<String> = _gridMode.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // Granular Appearance Settings
    private val _colorPalette = MutableStateFlow(prefs.getString("color_palette", "wallpaper") ?: "wallpaper")
    val colorPalette: StateFlow<String> = _colorPalette.asStateFlow()

    private val _followSystemTheme = MutableStateFlow(prefs.getBoolean("follow_system_theme", true))
    val followSystemTheme: StateFlow<Boolean> = _followSystemTheme.asStateFlow()

    private val _useDarkMode = MutableStateFlow(prefs.getBoolean("use_dark_mode", false))
    val useDarkMode: StateFlow<Boolean> = _useDarkMode.asStateFlow()

    private val _useAmoledMode = MutableStateFlow(prefs.getBoolean("use_amoled_mode", false))
    val useAmoledMode: StateFlow<Boolean> = _useAmoledMode.asStateFlow()

    private val _fancyBlur = MutableStateFlow(prefs.getBoolean("fancy_blur", true))
    val fancyBlur: StateFlow<Boolean> = _fancyBlur.asStateFlow()

    private val _liquidGlassMode = MutableStateFlow(prefs.getBoolean("liquid_glass_mode", false))
    val liquidGlassMode: StateFlow<Boolean> = _liquidGlassMode.asStateFlow()

    private val _autoContrast = MutableStateFlow(prefs.getBoolean("auto_contrast", false))
    val autoContrast: StateFlow<Boolean> = _autoContrast.asStateFlow()

    private val _animateMediaItems = MutableStateFlow(prefs.getBoolean("animate_media_items", true))
    val animateMediaItems: StateFlow<Boolean> = _animateMediaItems.asStateFlow()

    private val _useSystemFont = MutableStateFlow(prefs.getBoolean("use_system_font", true))
    val useSystemFont: StateFlow<Boolean> = _useSystemFont.asStateFlow()

    // Granular Timeline & Albums Settings
    private val _groupSimilarPhotos = MutableStateFlow(prefs.getBoolean("group_similar_photos", true))
    val groupSimilarPhotos: StateFlow<Boolean> = _groupSimilarPhotos.asStateFlow()

    private val _animateGifs = MutableStateFlow(prefs.getBoolean("animate_gifs", true))
    val animateGifs: StateFlow<Boolean> = _animateGifs.asStateFlow()

    private val _dateFormat = MutableStateFlow(prefs.getString("date_format", "MMM dd, yyyy") ?: "MMM dd, yyyy")
    val dateFormat: StateFlow<String> = _dateFormat.asStateFlow()

    private val _showFilterButton = MutableStateFlow(prefs.getBoolean("show_filter_button", true))
    val showFilterButton: StateFlow<Boolean> = _showFilterButton.asStateFlow()

    private val _showFavoriteButton = MutableStateFlow(prefs.getBoolean("show_favorite_button", true))
    val showFavoriteButton: StateFlow<Boolean> = _showFavoriteButton.asStateFlow()

    private val _storyCardsEnabled = MutableStateFlow(prefs.getBoolean("story_cards_enabled", true))
    val storyCardsEnabled: StateFlow<Boolean> = _storyCardsEnabled.asStateFlow()

    private val _hideTimelineForAlbums = MutableStateFlow(prefs.getBoolean("hide_timeline_for_albums", false))
    val hideTimelineForAlbums: StateFlow<Boolean> = _hideTimelineForAlbums.asStateFlow()

    private val _mergeAlbumsByName = MutableStateFlow(prefs.getBoolean("merge_albums_by_name", true))
    val mergeAlbumsByName: StateFlow<Boolean> = _mergeAlbumsByName.asStateFlow()

    private val _organizeAlbumsSections = MutableStateFlow(prefs.getBoolean("organize_albums_sections", true))
    val organizeAlbumsSections: StateFlow<Boolean> = _organizeAlbumsSections.asStateFlow()

    private val _dateSeparators = MutableStateFlow(prefs.getBoolean("date_separators", true))
    val dateSeparators: StateFlow<Boolean> = _dateSeparators.asStateFlow()

    private val _mediaGrouping = MutableStateFlow(prefs.getString("media_grouping", "date") ?: "date")
    val mediaGrouping: StateFlow<String> = _mediaGrouping.asStateFlow()

    // Granular Media Viewer Settings
    private val _fullBrightness = MutableStateFlow(prefs.getBoolean("full_brightness", false))
    val fullBrightness: StateFlow<Boolean> = _fullBrightness.asStateFlow()

    private val _showMediaHeader = MutableStateFlow(prefs.getBoolean("show_media_header", true))
    val showMediaHeader: StateFlow<Boolean> = _showMediaHeader.asStateFlow()

    private val _showFavoriteInViewer = MutableStateFlow(prefs.getBoolean("show_favorite_in_viewer", true))
    val showFavoriteInViewer: StateFlow<Boolean> = _showFavoriteInViewer.asStateFlow()

    private val _defaultEditor = MutableStateFlow(prefs.getString("default_editor", "default") ?: "default")
    val defaultEditor: StateFlow<String> = _defaultEditor.asStateFlow()

    private val _hideUiOnVideoPlay = MutableStateFlow(prefs.getBoolean("hide_ui_on_video_play", true))
    val hideUiOnVideoPlay: StateFlow<Boolean> = _hideUiOnVideoPlay.asStateFlow()

    private val _videoAutoPlay = MutableStateFlow(prefs.getBoolean("video_auto_play", false))
    val videoAutoPlay: StateFlow<Boolean> = _videoAutoPlay.asStateFlow()

    // Granular Navigation Settings
    private val _defaultLaunchScreen = MutableStateFlow(prefs.getString("default_launch_screen", "photos") ?: "photos")
    val defaultLaunchScreen: StateFlow<String> = _defaultLaunchScreen.asStateFlow()

    private val _hideSearchBarOnScroll = MutableStateFlow(prefs.getBoolean("hide_search_bar_on_scroll", true))
    val hideSearchBarOnScroll: StateFlow<Boolean> = _hideSearchBarOnScroll.asStateFlow()

    private val _hideNavBarOnScroll = MutableStateFlow(prefs.getBoolean("hide_nav_bar_on_scroll", true))
    val hideNavBarOnScroll: StateFlow<Boolean> = _hideNavBarOnScroll.asStateFlow()

    private val _displayItemTitle = MutableStateFlow(prefs.getBoolean("display_item_title", false))
    val displayItemTitle: StateFlow<Boolean> = _displayItemTitle.asStateFlow()

    private val _customSelectionActions = MutableStateFlow(prefs.getBoolean("custom_selection_actions", false))
    val customSelectionActions: StateFlow<Boolean> = _customSelectionActions.asStateFlow()

    // Granular General Settings
    private val _useTrashCan = MutableStateFlow(prefs.getBoolean("use_trash_can", true))
    val useTrashCan: StateFlow<Boolean> = _useTrashCan.asStateFlow()

    private val _secureMode = MutableStateFlow(prefs.getBoolean("secure_mode", false))
    val secureMode: StateFlow<Boolean> = _secureMode.asStateFlow()

    // Granular Security Settings
    private val _metadataIsolation = MutableStateFlow(prefs.getBoolean("metadata_isolation", false))
    val metadataIsolation: StateFlow<Boolean> = _metadataIsolation.asStateFlow()

    private val _sandboxedDecoding = MutableStateFlow(prefs.getBoolean("sandboxed_decoding", true))
    val sandboxedDecoding: StateFlow<Boolean> = _sandboxedDecoding.asStateFlow()

    private val _useBiometricsForVault = MutableStateFlow(prefs.getBoolean("use_biometrics_for_vault", true))
    val useBiometricsForVault: StateFlow<Boolean> = _useBiometricsForVault.asStateFlow()

    // Additional requested advanced state variables
    private val _hideEmptySystemAlbums = MutableStateFlow(prefs.getBoolean("hide_empty_system_albums", true))
    val hideEmptySystemAlbums: StateFlow<Boolean> = _hideEmptySystemAlbums.asStateFlow()

    private val _muteVideoByDefault = MutableStateFlow(prefs.getBoolean("mute_video_by_default", true))
    val muteVideoByDefault: StateFlow<Boolean> = _muteVideoByDefault.asStateFlow()

    private val _reorderFloatingActions = MutableStateFlow(prefs.getBoolean("reorder_floating_actions", false))
    val reorderFloatingActions: StateFlow<Boolean> = _reorderFloatingActions.asStateFlow()

    private val _doubleConfirmTrash = MutableStateFlow(prefs.getBoolean("double_confirm_trash", true))
    val doubleConfirmTrash: StateFlow<Boolean> = _doubleConfirmTrash.asStateFlow()

    private val _vaultTriggerShortcut = MutableStateFlow(prefs.getString("vault_trigger_shortcut", "long_press") ?: "long_press")
    val vaultTriggerShortcut: StateFlow<String> = _vaultTriggerShortcut.asStateFlow()

    private val _revokeClipboardOnPause = MutableStateFlow(prefs.getBoolean("revoke_clipboard_on_pause", true))
    val revokeClipboardOnPause: StateFlow<Boolean> = _revokeClipboardOnPause.asStateFlow()

    // --- MODULE 2: SECURITY VAULT STATES ---
    private val _vaultPin = MutableStateFlow(prefs.getString("vault_pin", "") ?: "")
    val vaultPin: StateFlow<String> = _vaultPin.asStateFlow()

    private val _vaultEncryptionMode = MutableStateFlow(prefs.getString("vault_encryption_mode", "ask") ?: "ask")
    val vaultEncryptionMode: StateFlow<String> = _vaultEncryptionMode.asStateFlow()

    // --- TRASH & SECURE VAULT PERSISTENCE ---
    private val _trashIds = MutableStateFlow<Map<Long, Long>>(loadTrashIdsFromPrefs()) // ID -> Deleted timestamp
    val trashIds: StateFlow<Map<Long, Long>> = _trashIds.asStateFlow()

    private val _vaultIds = MutableStateFlow<Set<Long>>(loadVaultIdsFromPrefs())
    val vaultIds: StateFlow<Set<Long>> = _vaultIds.asStateFlow()

    private val _hardDeletedIds = MutableStateFlow<Set<Long>>(loadHardDeletedIdsFromPrefs())

    private fun loadTrashIdsFromPrefs(): Map<Long, Long> {
        val set = prefs.getStringSet("trash_ids_map_v2", emptySet()) ?: emptySet()
        val map = mutableMapOf<Long, Long>()
        set.forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val id = parts[0].toLongOrNull()
                val ts = parts[1].toLongOrNull()
                if (id != null && ts != null) {
                    map[id] = ts
                }
            }
        }
        return map
    }

    private fun saveTrashIdsToPrefs(map: Map<Long, Long>) {
        val set = map.map { "${it.key}:${it.value}" }.toSet()
        prefs.edit().putStringSet("trash_ids_map_v2", set).apply()
    }

    private fun loadVaultIdsFromPrefs(): Set<Long> {
        val set = prefs.getStringSet("vault_ids_v2", emptySet()) ?: emptySet()
        return set.mapNotNull { it.toLongOrNull() }.toSet()
    }

    private fun saveVaultIdsToPrefs(set: Set<Long>) {
        val stringSet = set.map { it.toString() }.toSet()
        prefs.edit().putStringSet("vault_ids_v2", stringSet).apply()
    }

    private fun loadHardDeletedIdsFromPrefs(): Set<Long> {
        val set = prefs.getStringSet("hard_deleted_ids", emptySet()) ?: emptySet()
        return set.mapNotNull { it.toLongOrNull() }.toSet()
    }

    private fun saveHardDeletedIdsToPrefs(set: Set<Long>) {
        val stringSet = set.map { it.toString() }.toSet()
        prefs.edit().putStringSet("hard_deleted_ids", stringSet).apply()
    }

    // Dynamic album classification
    val albumFolders: StateFlow<List<AlbumFolder>> = combine(
        _rawMediaItems,
        _trashIds,
        _vaultIds,
        _favoriteIds,
        _hardDeletedIds
    ) { items, trash, vault, favorites, hardDeleted ->
        val visibleItems = items.filter { 
            !trash.containsKey(it.id) && !vault.contains(it.id) && !hardDeleted.contains(it.id)
        }
        
        val folders = mutableListOf<AlbumFolder>()
        
        // 1. Camera
        val camera = visibleItems.filter { !it.isVideo && !it.name.contains("Screenshot", true) && !it.name.contains("Download", true) }
        if (camera.isNotEmpty()) folders.add(AlbumFolder("Camera", camera))
        
        // 2. Screenshots
        val screenshots = visibleItems.filter { it.name.contains("Screenshot", true) || it.id == 4L || it.id == 8L }
        if (screenshots.isNotEmpty()) folders.add(AlbumFolder("Screenshots", screenshots))
        
        // 3. Downloads
        val downloads = visibleItems.filter { it.name.contains("Download", true) || it.id == 9L || it.id == 11L }
        if (downloads.isNotEmpty()) folders.add(AlbumFolder("Downloads", downloads))
        
        // 4. WhatsApp Images
        val whatsapp = visibleItems.filter { it.name.contains("WhatsApp", true) || it.id == 6L || it.id == 12L || it.id == 3L }
        if (whatsapp.isNotEmpty()) folders.add(AlbumFolder("WhatsApp Images", whatsapp))
        
        // Add default/fallback if list is empty
        if (folders.isEmpty() && visibleItems.isNotEmpty()) {
            folders.add(AlbumFolder("Camera", visibleItems))
        }
        
        folders
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI state containing filtered and grouped media items
    val uiState: StateFlow<GalleryUiState> = combine(
        _rawMediaItems,
        _searchQuery,
        _favoriteIds,
        _isLoading,
        _trashIds,
        _vaultIds,
        _hardDeletedIds
    ) { flowsArray ->
        val items = flowsArray[0] as List<MediaItem>
        val query = flowsArray[1] as String
        val favorites = flowsArray[2] as Set<Long>
        val loading = flowsArray[3] as Boolean
        val trash = flowsArray[4] as Map<Long, Long>
        val vault = flowsArray[5] as Set<Long>
        val hardDeleted = flowsArray[6] as Set<Long>

        if (loading) {
            GalleryUiState.Loading
        } else {
            // Apply favorite status on the fly
            val updatedItems = items.map { item ->
                item.copy(isFavorite = favorites.contains(item.id))
            }.filter { !hardDeleted.contains(it.id) }

            // Exclude trash and vault from the main timeline
            val visibleItems = updatedItems.filter { item ->
                !trash.containsKey(item.id) && !vault.contains(item.id)
            }

            // Filter items by search query if non-empty
            val filteredItems = if (query.isBlank()) {
                visibleItems
            } else {
                visibleItems.filter { item ->
                    item.name.contains(query, ignoreCase = true) ||
                        (item.location?.contains(query, ignoreCase = true) ?: false) ||
                        item.mimeType.contains(query, ignoreCase = true)
                }
            }

            // Group media items by date descending
            val grouped = filteredItems.groupBy { it.friendlyDate }

            GalleryUiState.Success(
                groupedMedia = grouped,
                allMedia = filteredItems
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GalleryUiState.Loading
    )

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val media = repository.fetchLocalMedia()
                _rawMediaItems.value = media
            } catch (e: Exception) {
                _rawMediaItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun setColumnCount(count: Int) {
        val bounded = count.coerceIn(1, 6)
        _columnCount.value = bounded
        prefs.edit().putInt("column_count", bounded).apply()
    }

    fun moveToTrash(itemId: Long) {
        val currentTrash = _trashIds.value.toMutableMap()
        currentTrash[itemId] = System.currentTimeMillis()
        _trashIds.value = currentTrash
        saveTrashIdsToPrefs(currentTrash)
    }

    fun restoreFromTrash(itemId: Long) {
        val currentTrash = _trashIds.value.toMutableMap()
        currentTrash.remove(itemId)
        _trashIds.value = currentTrash
        saveTrashIdsToPrefs(currentTrash)
    }

    fun permanentlyDelete(itemId: Long) {
        // Remove from trash
        val currentTrash = _trashIds.value.toMutableMap()
        currentTrash.remove(itemId)
        _trashIds.value = currentTrash
        saveTrashIdsToPrefs(currentTrash)

        // Mark as hard deleted so it never returns in raw lists
        val currentHardDeleted = _hardDeletedIds.value.toMutableSet()
        currentHardDeleted.add(itemId)
        _hardDeletedIds.value = currentHardDeleted
        saveHardDeletedIdsToPrefs(currentHardDeleted)

        // Delete from current raw list
        deleteItem(itemId)
    }

    fun moveToVault(itemId: Long) {
        val currentVault = _vaultIds.value.toMutableSet()
        currentVault.add(itemId)
        _vaultIds.value = currentVault
        saveVaultIdsToPrefs(currentVault)
    }

    fun removeFromVault(itemId: Long) {
        val currentVault = _vaultIds.value.toMutableSet()
        currentVault.remove(itemId)
        _vaultIds.value = currentVault
        saveVaultIdsToPrefs(currentVault)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _isVibrationEnabled.value = enabled
        prefs.edit().putBoolean("is_vibration_enabled", enabled).apply()
    }

    fun setTrashConfirmationEnabled(enabled: Boolean) {
        _isTrashConfirmationEnabled.value = enabled
        prefs.edit().putBoolean("is_trash_confirmation_enabled", enabled).apply()
    }

    fun setSystemTrashEnabled(enabled: Boolean) {
        _isSystemTrashEnabled.value = enabled
        prefs.edit().putBoolean("is_system_trash_enabled", enabled).apply()
    }

    fun setGridMode(mode: String) {
        _gridMode.value = mode
        prefs.edit().putString("grid_mode", mode).apply()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setVaultPin(pin: String) {
        _vaultPin.value = pin
        prefs.edit().putString("vault_pin", pin).apply()
    }

    fun setVaultEncryptionMode(mode: String) {
        _vaultEncryptionMode.value = mode
        prefs.edit().putString("vault_encryption_mode", mode).apply()
    }

    fun toggleFavorite(itemId: Long) {
        val currentFavorites = _favoriteIds.value.toMutableSet()
        if (currentFavorites.contains(itemId)) {
            currentFavorites.remove(itemId)
        } else {
            currentFavorites.add(itemId)
        }
        _favoriteIds.value = currentFavorites
    }

    fun deleteItem(itemId: Long) {
        // Remove from current list in state (since it is local offline gallery)
        val currentList = _rawMediaItems.value.toMutableList()
        currentList.removeAll { it.id == itemId }
        _rawMediaItems.value = currentList
    }

    fun insertEditedMediaItem(newItem: MediaItem) {
        val currentList = _rawMediaItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == newItem.id }
        if (index != -1) {
            currentList[index] = newItem
        } else {
            currentList.add(0, newItem)
        }
        _rawMediaItems.value = currentList
    }

    // --- GRANULAR SETTERS ---
    fun setColorPalette(palette: String) {
        _colorPalette.value = palette
        prefs.edit().putString("color_palette", palette).apply()
    }

    fun setFollowSystemTheme(enabled: Boolean) {
        _followSystemTheme.value = enabled
        prefs.edit().putBoolean("follow_system_theme", enabled).apply()
    }

    fun setUseDarkMode(enabled: Boolean) {
        _useDarkMode.value = enabled
        prefs.edit().putBoolean("use_dark_mode", enabled).apply()
    }

    fun setUseAmoledMode(enabled: Boolean) {
        _useAmoledMode.value = enabled
        prefs.edit().putBoolean("use_amoled_mode", enabled).apply()
    }

    fun setFancyBlur(enabled: Boolean) {
        _fancyBlur.value = enabled
        prefs.edit().putBoolean("fancy_blur", enabled).apply()
    }

    fun setLiquidGlassMode(enabled: Boolean) {
        _liquidGlassMode.value = enabled
        prefs.edit().putBoolean("liquid_glass_mode", enabled).apply()
    }

    fun setAutoContrast(enabled: Boolean) {
        _autoContrast.value = enabled
        prefs.edit().putBoolean("auto_contrast", enabled).apply()
    }

    fun setAnimateMediaItems(enabled: Boolean) {
        _animateMediaItems.value = enabled
        prefs.edit().putBoolean("animate_media_items", enabled).apply()
    }

    fun setUseSystemFont(enabled: Boolean) {
        _useSystemFont.value = enabled
        prefs.edit().putBoolean("use_system_font", enabled).apply()
    }

    fun setGroupSimilarPhotos(enabled: Boolean) {
        _groupSimilarPhotos.value = enabled
        prefs.edit().putBoolean("group_similar_photos", enabled).apply()
    }

    fun setAnimateGifs(enabled: Boolean) {
        _animateGifs.value = enabled
        prefs.edit().putBoolean("animate_gifs", enabled).apply()
    }

    fun setDateFormat(format: String) {
        _dateFormat.value = format
        prefs.edit().putString("date_format", format).apply()
    }

    fun setShowFilterButton(enabled: Boolean) {
        _showFilterButton.value = enabled
        prefs.edit().putBoolean("show_filter_button", enabled).apply()
    }

    fun setShowFavoriteButton(enabled: Boolean) {
        _showFavoriteButton.value = enabled
        prefs.edit().putBoolean("show_favorite_button", enabled).apply()
    }

    fun setStoryCardsEnabled(enabled: Boolean) {
        _storyCardsEnabled.value = enabled
        prefs.edit().putBoolean("story_cards_enabled", enabled).apply()
    }

    fun setHideTimelineForAlbums(enabled: Boolean) {
        _hideTimelineForAlbums.value = enabled
        prefs.edit().putBoolean("hide_timeline_for_albums", enabled).apply()
    }

    fun setMergeAlbumsByName(enabled: Boolean) {
        _mergeAlbumsByName.value = enabled
        prefs.edit().putBoolean("merge_albums_by_name", enabled).apply()
    }

    fun setOrganizeAlbumsSections(enabled: Boolean) {
        _organizeAlbumsSections.value = enabled
        prefs.edit().putBoolean("organize_albums_sections", enabled).apply()
    }

    fun setDateSeparators(enabled: Boolean) {
        _dateSeparators.value = enabled
        prefs.edit().putBoolean("date_separators", enabled).apply()
    }

    fun setMediaGrouping(grouping: String) {
        _mediaGrouping.value = grouping
        prefs.edit().putString("media_grouping", grouping).apply()
    }

    fun setFullBrightness(enabled: Boolean) {
        _fullBrightness.value = enabled
        prefs.edit().putBoolean("full_brightness", enabled).apply()
    }

    fun setShowMediaHeader(enabled: Boolean) {
        _showMediaHeader.value = enabled
        prefs.edit().putBoolean("show_media_header", enabled).apply()
    }

    fun setShowFavoriteInViewer(enabled: Boolean) {
        _showFavoriteInViewer.value = enabled
        prefs.edit().putBoolean("show_favorite_in_viewer", enabled).apply()
    }

    fun setDefaultEditor(editor: String) {
        _defaultEditor.value = editor
        prefs.edit().putString("default_editor", editor).apply()
    }

    fun setHideUiOnVideoPlay(enabled: Boolean) {
        _hideUiOnVideoPlay.value = enabled
        prefs.edit().putBoolean("hide_ui_on_video_play", enabled).apply()
    }

    fun setVideoAutoPlay(enabled: Boolean) {
        _videoAutoPlay.value = enabled
        prefs.edit().putBoolean("video_auto_play", enabled).apply()
    }

    fun setDefaultLaunchScreen(screen: String) {
        _defaultLaunchScreen.value = screen
        prefs.edit().putString("default_launch_screen", screen).apply()
    }

    fun setHideSearchBarOnScroll(enabled: Boolean) {
        _hideSearchBarOnScroll.value = enabled
        prefs.edit().putBoolean("hide_search_bar_on_scroll", enabled).apply()
    }

    fun setHideNavBarOnScroll(enabled: Boolean) {
        _hideNavBarOnScroll.value = enabled
        prefs.edit().putBoolean("hide_nav_bar_on_scroll", enabled).apply()
    }

    fun setDisplayItemTitle(enabled: Boolean) {
        _displayItemTitle.value = enabled
        prefs.edit().putBoolean("display_item_title", enabled).apply()
    }

    fun setCustomSelectionActions(enabled: Boolean) {
        _customSelectionActions.value = enabled
        prefs.edit().putBoolean("custom_selection_actions", enabled).apply()
    }

    fun setUseTrashCan(enabled: Boolean) {
        _useTrashCan.value = enabled
        prefs.edit().putBoolean("use_trash_can", enabled).apply()
    }

    fun setSecureMode(enabled: Boolean) {
        _secureMode.value = enabled
        prefs.edit().putBoolean("secure_mode", enabled).apply()
    }

    fun setMetadataIsolation(enabled: Boolean) {
        _metadataIsolation.value = enabled
        prefs.edit().putBoolean("metadata_isolation", enabled).apply()
    }

    fun setSandboxedDecoding(enabled: Boolean) {
        _sandboxedDecoding.value = enabled
        prefs.edit().putBoolean("sandboxed_decoding", enabled).apply()
    }

    fun setUseBiometricsForVault(enabled: Boolean) {
        _useBiometricsForVault.value = enabled
        prefs.edit().putBoolean("use_biometrics_for_vault", enabled).apply()
    }

    fun setHideEmptySystemAlbums(enabled: Boolean) {
        _hideEmptySystemAlbums.value = enabled
        prefs.edit().putBoolean("hide_empty_system_albums", enabled).apply()
    }

    fun setMuteVideoByDefault(enabled: Boolean) {
        _muteVideoByDefault.value = enabled
        prefs.edit().putBoolean("mute_video_by_default", enabled).apply()
    }

    fun setReorderFloatingActions(enabled: Boolean) {
        _reorderFloatingActions.value = enabled
        prefs.edit().putBoolean("reorder_floating_actions", enabled).apply()
    }

    fun setDoubleConfirmTrash(enabled: Boolean) {
        _doubleConfirmTrash.value = enabled
        prefs.edit().putBoolean("double_confirm_trash", enabled).apply()
    }

    fun setVaultTriggerShortcut(shortcut: String) {
        _vaultTriggerShortcut.value = shortcut
        prefs.edit().putString("vault_trigger_shortcut", shortcut).apply()
    }

    fun setRevokeClipboardOnPause(enabled: Boolean) {
        _revokeClipboardOnPause.value = enabled
        prefs.edit().putBoolean("revoke_clipboard_on_pause", enabled).apply()
    }
}
