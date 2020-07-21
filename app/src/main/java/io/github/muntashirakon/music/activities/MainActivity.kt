package io.github.muntashirakon.music.activities

import android.app.ActivityOptions
import android.content.*
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import code.name.monkey.appthemehelper.util.ATHUtil.resolveColor
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import io.github.muntashirakon.music.R
import io.github.muntashirakon.music.activities.base.AbsSlidingMusicPanelActivity
import io.github.muntashirakon.music.dialogs.CreatePlaylistDialog.Companion.create
import io.github.muntashirakon.music.fragments.LibraryViewModel
import io.github.muntashirakon.music.fragments.albums.AlbumsFragment
import io.github.muntashirakon.music.fragments.artists.ArtistsFragment
import io.github.muntashirakon.music.fragments.base.AbsLibraryPagerRecyclerViewCustomGridSizeFragment
import io.github.muntashirakon.music.fragments.genres.GenresFragment
import io.github.muntashirakon.music.fragments.home.BannerHomeFragment
import io.github.muntashirakon.music.fragments.mainactivity.FoldersFragment
import io.github.muntashirakon.music.fragments.playlists.PlaylistsFragment
import io.github.muntashirakon.music.fragments.queue.PlayingQueueFragment
import io.github.muntashirakon.music.fragments.songs.SongsFragment
import io.github.muntashirakon.music.helper.MusicPlayerRemote.isPlaying
import io.github.muntashirakon.music.helper.MusicPlayerRemote.openAndShuffleQueue
import io.github.muntashirakon.music.helper.MusicPlayerRemote.openQueue
import io.github.muntashirakon.music.helper.MusicPlayerRemote.playFromUri
import io.github.muntashirakon.music.helper.MusicPlayerRemote.shuffleMode
import io.github.muntashirakon.music.helper.SearchQueryHelper.getSongs
import io.github.muntashirakon.music.helper.SortOrder.*
import io.github.muntashirakon.music.interfaces.CabHolder
import io.github.muntashirakon.music.interfaces.MainActivityFragmentCallbacks
import io.github.muntashirakon.music.loaders.AlbumLoader.getAlbum
import io.github.muntashirakon.music.loaders.ArtistLoader.getArtist
import io.github.muntashirakon.music.loaders.PlaylistSongsLoader.getPlaylistSongList
import io.github.muntashirakon.music.model.Song
import io.github.muntashirakon.music.service.MusicService
import io.github.muntashirakon.music.util.AppRater.appLaunched
import io.github.muntashirakon.music.util.NavigationUtil
import io.github.muntashirakon.music.util.PreferenceUtil
import io.github.muntashirakon.music.util.RetroColorUtil
import io.github.muntashirakon.music.util.RetroUtil
import com.afollestad.materialcab.MaterialCab
import com.google.android.material.appbar.AppBarLayout
import io.github.muntashirakon.music.*
import kotlinx.android.synthetic.main.activity_main_content.*
import org.koin.android.ext.android.inject
import java.util.*

class MainActivity : AbsSlidingMusicPanelActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener, CabHolder {
    companion object {
        const val TAG = "MainActivity"
        const val EXPAND_PANEL = "expand_panel"
    }

    val libraryViewModel: LibraryViewModel by inject()
    private var cab: MaterialCab? = null
    private val intentFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
    private lateinit var currentFragment: MainActivityFragmentCallbacks
    private var blockRequestPermissions = false
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null && action == Intent.ACTION_SCREEN_OFF) {
                if (PreferenceUtil.isLockScreen && isPlaying) {
                    val activity = Intent(context, LockScreenActivity::class.java)
                    activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    activity.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    ActivityCompat.startActivity(context, activity, null)
                }
            }
        }
    }

    override fun createContentView(): View {
        return wrapSlidingMusicPanel(R.layout.activity_main_content)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setDrawUnderStatusBar()
        super.onCreate(savedInstanceState)
        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setLightNavigationBar(true)
        setTaskDescriptionColorAuto()
        hideStatusBar()
        setBottomBarVisibility(View.VISIBLE)

        addMusicServiceEventListener(libraryViewModel)
        if (savedInstanceState == null) {
            selectedFragment(PreferenceUtil.lastPage)
        } else {
            restoreCurrentFragment()
        }

        appLaunched(this)
        setupToolbar()
        updateTabs()
        getBottomNavigationView().selectedItemId = PreferenceUtil.lastPage
        getBottomNavigationView().setOnNavigationItemSelectedListener {
            PreferenceUtil.lastPage = it.itemId
            selectedFragment(it.itemId)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastReceiver, intentFilter)
        PreferenceUtil.registerOnSharedPreferenceChangedListener(this)
        if (intent.hasExtra(EXPAND_PANEL) &&
            intent.getBooleanExtra(EXPAND_PANEL, false) &&
            PreferenceUtil.isExpandPanel
        ) {
            expandPanel()
            intent.removeExtra(EXPAND_PANEL)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        PreferenceUtil.unregisterOnSharedPreferenceChangedListener(this)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        ToolbarContentTintHelper.handleOnPrepareOptionsMenu(this, toolbar)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu ?: return super.onCreateOptionsMenu(menu)
        if (isPlaylistPage()) {
            menu.add(0, R.id.action_new_playlist, 1, R.string.new_playlist_title)
                .setIcon(R.drawable.ic_playlist_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        if (isHomePage()) {
            menu.add(0, R.id.action_mic, 1, getString(R.string.action_search))
                .setIcon(R.drawable.ic_mic)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        if (isFolderPage()) {
            menu.add(0, R.id.action_scan, 0, R.string.scan_media)
                .setIcon(R.drawable.ic_scanner)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            menu.add(0, R.id.action_go_to_start_directory, 1, R.string.action_go_to_start_directory)
                .setIcon(R.drawable.ic_bookmark_music)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        val fragment: Fragment? = getCurrentFragment()
        if (fragment != null && fragment is AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>) {
            val gridSizeItem: MenuItem = menu.findItem(R.id.action_grid_size)
            if (RetroUtil.isLandscape()) {
                gridSizeItem.setTitle(R.string.action_grid_size_land)
            }
            setUpGridSizeMenu(fragment, gridSizeItem.subMenu)
            setupLayoutMenu(fragment, menu.findItem(R.id.action_layout_type).subMenu)
            setUpSortOrderMenu(fragment, menu.findItem(R.id.action_sort_order).subMenu)
        } else {
            menu.removeItem(R.id.action_layout_type)
            menu.removeItem(R.id.action_grid_size)
            menu.removeItem(R.id.action_sort_order)
        }
        menu.add(0, R.id.action_settings, 6, getString(R.string.action_settings))
            .setIcon(R.drawable.ic_settings)
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu.add(0, R.id.action_search, 0, getString(R.string.action_search))
            .setIcon(R.drawable.ic_search)
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(
            this,
            toolbar,
            menu,
            getToolbarBackgroundColor(toolbar)
        )
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val fragment = getCurrentFragment()
        if (fragment is AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>) {
            if (handleGridSizeMenuItem(fragment, item)) {
                return true
            }
            if (handleLayoutResType(fragment, item)) {
                return true
            }
            if (handleSortOrderMenuItem(fragment, item)) {
                return true
            }
        }
        when (item.itemId) {
            R.id.action_search -> NavigationUtil.goToSearch(this)
            R.id.action_new_playlist -> {
                create().show(supportFragmentManager, "CREATE_PLAYLIST")
                return true
            }
            R.id.action_mic -> {
                val options = ActivityOptions.makeSceneTransitionAnimation(
                    this, toolbar,
                    getString(R.string.transition_toolbar)
                )
                NavigationUtil.goToSearch(this, true, options)
                return true
            }
            R.id.action_settings -> {
                NavigationUtil.goToSettings(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleSortOrderMenuItem(
        fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>,
        item: MenuItem
    ): Boolean {
        var sortOrder: String? = null
        when (fragment) {
            is AlbumsFragment -> {
                when (item.itemId) {
                    R.id.action_album_sort_order_asc -> sortOrder = AlbumSortOrder.ALBUM_A_Z
                    R.id.action_album_sort_order_desc -> sortOrder = AlbumSortOrder.ALBUM_Z_A
                    R.id.action_album_sort_order_artist -> sortOrder = AlbumSortOrder.ALBUM_ARTIST
                    R.id.action_album_sort_order_year -> sortOrder = AlbumSortOrder.ALBUM_YEAR
                }
            }
            is ArtistsFragment -> {
                when (item.itemId) {
                    R.id.action_artist_sort_order_asc -> sortOrder = ArtistSortOrder.ARTIST_A_Z
                    R.id.action_artist_sort_order_desc -> sortOrder = ArtistSortOrder.ARTIST_Z_A
                }
            }
            is SongsFragment -> {
                when (item.itemId) {
                    R.id.action_song_sort_order_asc -> sortOrder = SongSortOrder.SONG_A_Z
                    R.id.action_song_sort_order_desc -> sortOrder = SongSortOrder.SONG_Z_A
                    R.id.action_song_sort_order_artist -> sortOrder = SongSortOrder.SONG_ARTIST
                    R.id.action_song_sort_order_album -> sortOrder = SongSortOrder.SONG_ALBUM
                    R.id.action_song_sort_order_year -> sortOrder = SongSortOrder.SONG_YEAR
                    R.id.action_song_sort_order_date -> sortOrder = SongSortOrder.SONG_DATE
                    R.id.action_song_sort_order_composer -> sortOrder = SongSortOrder.COMPOSER
                    R.id.action_song_sort_order_date_modified ->
                        sortOrder = SongSortOrder.SONG_DATE_MODIFIED
                }
            }
        }

        if (sortOrder != null) {
            item.isChecked = true
            fragment.setAndSaveSortOrder(sortOrder)
            return true
        }

        return false
    }

    private fun handleLayoutResType(
        fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>,
        item: MenuItem
    ): Boolean {
        var layoutRes = -1
        when (item.itemId) {
            R.id.action_layout_normal -> layoutRes = R.layout.item_grid
            R.id.action_layout_card -> layoutRes = R.layout.item_card
            R.id.action_layout_colored_card -> layoutRes = R.layout.item_card_color
            R.id.action_layout_circular -> layoutRes = R.layout.item_grid_circle
            R.id.action_layout_image -> layoutRes = R.layout.image
            R.id.action_layout_gradient_image -> layoutRes = R.layout.item_image_gradient
        }
        if (layoutRes != -1) {
            item.isChecked = true
            fragment.setAndSaveLayoutRes(layoutRes)
            return true
        }
        return false
    }

    private fun handleGridSizeMenuItem(
        fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>,
        item: MenuItem
    ): Boolean {
        var gridSize = 0
        when (item.itemId) {
            R.id.action_grid_size_1 -> gridSize = 1
            R.id.action_grid_size_2 -> gridSize = 2
            R.id.action_grid_size_3 -> gridSize = 3
            R.id.action_grid_size_4 -> gridSize = 4
            R.id.action_grid_size_5 -> gridSize = 5
            R.id.action_grid_size_6 -> gridSize = 6
            R.id.action_grid_size_7 -> gridSize = 7
            R.id.action_grid_size_8 -> gridSize = 8
        }
        if (gridSize > 0) {
            item.isChecked = true
            fragment.setAndSaveGridSize(gridSize)
            return true
        }
        return false
    }

    private fun setUpGridSizeMenu(
        fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>,
        gridSizeMenu: SubMenu
    ) {
        when (fragment.getGridSize()) {
            1 -> gridSizeMenu.findItem(R.id.action_grid_size_1).isChecked = true
            2 -> gridSizeMenu.findItem(R.id.action_grid_size_2).isChecked = true
            3 -> gridSizeMenu.findItem(R.id.action_grid_size_3).isChecked = true
            4 -> gridSizeMenu.findItem(R.id.action_grid_size_4).isChecked = true
            5 -> gridSizeMenu.findItem(R.id.action_grid_size_5).isChecked = true
            6 -> gridSizeMenu.findItem(R.id.action_grid_size_6).isChecked = true
            7 -> gridSizeMenu.findItem(R.id.action_grid_size_7).isChecked = true
            8 -> gridSizeMenu.findItem(R.id.action_grid_size_8).isChecked = true
        }
        val maxGridSize = fragment.maxGridSize
        if (maxGridSize < 8) {
            gridSizeMenu.findItem(R.id.action_grid_size_8).isVisible = false
        }
        if (maxGridSize < 7) {
            gridSizeMenu.findItem(R.id.action_grid_size_7).isVisible = false
        }
        if (maxGridSize < 6) {
            gridSizeMenu.findItem(R.id.action_grid_size_6).isVisible = false
        }
        if (maxGridSize < 5) {
            gridSizeMenu.findItem(R.id.action_grid_size_5).isVisible = false
        }
        if (maxGridSize < 4) {
            gridSizeMenu.findItem(R.id.action_grid_size_4).isVisible = false
        }
        if (maxGridSize < 3) {
            gridSizeMenu.findItem(R.id.action_grid_size_3).isVisible = false
        }
    }

    private fun setupLayoutMenu(
        fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>,
        subMenu: SubMenu
    ) {
        when (fragment.itemLayoutRes()) {
            R.layout.item_card ->
                subMenu.findItem(R.id.action_layout_card).isChecked = true
            R.layout.item_card_color ->
                subMenu.findItem(R.id.action_layout_colored_card).isChecked = true
            R.layout.item_grid_circle ->
                subMenu.findItem(R.id.action_layout_circular).isChecked = true
            R.layout.image ->
                subMenu.findItem(R.id.action_layout_image).isChecked = true
            R.layout.item_image_gradient ->
                subMenu.findItem(R.id.action_layout_gradient_image).isChecked = true
            R.layout.item_grid ->
                subMenu.findItem(R.id.action_layout_normal).isChecked = true
            else ->
                subMenu.findItem(R.id.action_layout_normal).isChecked = true
        }
    }

    private fun setUpSortOrderMenu(
        fragment: AbsLibraryPagerRecyclerViewCustomGridSizeFragment<*, *>,
        sortOrderMenu: SubMenu
    ) {
        val currentSortOrder = fragment.getSortOrder()
        sortOrderMenu.clear()
        when (fragment) {
            is AlbumsFragment -> {
                sortOrderMenu.add(
                    0,
                    R.id.action_album_sort_order_asc,
                    0,
                    R.string.sort_order_a_z
                ).isChecked = currentSortOrder == AlbumSortOrder.ALBUM_A_Z
                sortOrderMenu.add(
                    0,
                    R.id.action_album_sort_order_desc,
                    1,
                    R.string.sort_order_z_a
                ).isChecked =
                    currentSortOrder == AlbumSortOrder.ALBUM_Z_A
                sortOrderMenu.add(
                    0,
                    R.id.action_album_sort_order_artist,
                    2,
                    R.string.sort_order_artist
                ).isChecked =
                    currentSortOrder == AlbumSortOrder.ALBUM_ARTIST
                sortOrderMenu.add(
                    0,
                    R.id.action_album_sort_order_year,
                    3,
                    R.string.sort_order_year
                ).isChecked =
                    currentSortOrder == AlbumSortOrder.ALBUM_YEAR
            }
            is ArtistsFragment -> {
                sortOrderMenu.add(
                    0,
                    R.id.action_artist_sort_order_asc,
                    0,
                    R.string.sort_order_a_z
                ).isChecked =
                    currentSortOrder == ArtistSortOrder.ARTIST_A_Z
                sortOrderMenu.add(
                    0,
                    R.id.action_artist_sort_order_desc,
                    1,
                    R.string.sort_order_z_a
                ).isChecked =
                    currentSortOrder == ArtistSortOrder.ARTIST_Z_A
            }
            is SongsFragment -> {
                sortOrderMenu.add(
                    0,
                    R.id.action_song_sort_order_asc,
                    0,
                    R.string.sort_order_a_z
                ).isChecked =
                    currentSortOrder == SongSortOrder.SONG_A_Z
                sortOrderMenu.add(
                    0,
                    R.id.action_song_sort_order_desc,
                    1,
                    R.string.sort_order_z_a
                ).isChecked =
                    currentSortOrder == SongSortOrder.SONG_Z_A
                sortOrderMenu.add(
                    0,
                    R.id.action_song_sort_order_artist,
                    2,
                    R.string.sort_order_artist
                ).isChecked =
                    currentSortOrder == SongSortOrder.SONG_ARTIST
                sortOrderMenu.add(
                    0,
                    R.id.action_song_sort_order_album,
                    3,
                    R.string.sort_order_album
                ).isChecked =
                    currentSortOrder == SongSortOrder.SONG_ALBUM
                sortOrderMenu.add(
                    0,
                    R.id.action_song_sort_order_year,
                    4,
                    R.string.sort_order_year
                ).isChecked =
                    currentSortOrder == SongSortOrder.SONG_YEAR
                sortOrderMenu.add(
                    0,
                    R.id.action_song_sort_order_date,
                    5,
                    R.string.sort_order_date
                ).isChecked =
                    currentSortOrder == SongSortOrder.SONG_DATE
                sortOrderMenu.add(
                    0,
                    R.id.action_song_sort_order_date_modified,
                    6,
                    R.string.sort_order_date_modified
                ).isChecked = currentSortOrder == SongSortOrder.SONG_DATE_MODIFIED
                sortOrderMenu.add(
                    0,
                    R.id.action_song_sort_order_composer,
                    7,
                    R.string.sort_order_composer
                ).isChecked = currentSortOrder == SongSortOrder.COMPOSER
            }
        }
        sortOrderMenu.setGroupCheckable(0, true, true)
    }

    private fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.fragment_container)
    }

    private fun isFolderPage(): Boolean {
        return supportFragmentManager.findFragmentByTag(FoldersFragment.TAG) is FoldersFragment
    }

    private fun isHomePage(): Boolean {
        return supportFragmentManager.findFragmentByTag(BannerHomeFragment.TAG) is BannerHomeFragment
    }

    private fun isPlaylistPage(): Boolean {
        return supportFragmentManager.findFragmentByTag(PlaylistsFragment.TAG) is PlaylistsFragment
    }

    fun addOnAppBarOffsetChangedListener(
        changedListener: AppBarLayout.OnOffsetChangedListener
    ) {
        appBarLayout.addOnOffsetChangedListener(changedListener)
    }

    fun removeOnAppBarOffsetChangedListener(
        changedListener: AppBarLayout.OnOffsetChangedListener
    ) {
        appBarLayout.removeOnOffsetChangedListener(changedListener)
    }

    fun getTotalAppBarScrollingRange(): Int {
        return appBarLayout.totalScrollRange
    }

    override fun requestPermissions() {
        if (!blockRequestPermissions) {
            super.requestPermissions()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!hasPermissions()) {
            requestPermissions()
        }
    }

    private fun setupToolbar() {
        toolbar.setBackgroundColor(resolveColor(this, R.attr.colorSurface))
        appBarLayout.setBackgroundColor(resolveColor(this, R.attr.colorSurface))
        setSupportActionBar(toolbar)
    }

    private fun setCurrentFragment(
        fragment: Fragment,
        tag: String
    ) {

        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment, tag)
        }
        currentFragment = fragment as MainActivityFragmentCallbacks
    }

    private fun selectedFragment(itemId: Int) {
        when (itemId) {
            R.id.action_album -> setCurrentFragment(
                AlbumsFragment.newInstance(),
                AlbumsFragment.TAG
            )
            R.id.action_artist -> setCurrentFragment(
                ArtistsFragment.newInstance(),
                ArtistsFragment.TAG
            )
            R.id.action_playlist -> setCurrentFragment(
                PlaylistsFragment.newInstance(),
                PlaylistsFragment.TAG
            )
            R.id.action_genre -> setCurrentFragment(
                GenresFragment.newInstance(),
                GenresFragment.TAG
            )
            R.id.action_playing_queue -> setCurrentFragment(
                PlayingQueueFragment.newInstance(),
                PlayingQueueFragment.TAG
            )
            R.id.action_song -> setCurrentFragment(
                SongsFragment.newInstance(),
                SongsFragment.TAG
            )
            R.id.action_folder -> setCurrentFragment(
                FoldersFragment.newInstance(this),
                FoldersFragment.TAG
            )
            R.id.action_home -> setCurrentFragment(
                BannerHomeFragment.newInstance(),
                BannerHomeFragment.TAG
            )
            else -> setCurrentFragment(
                BannerHomeFragment.newInstance(),
                BannerHomeFragment.TAG
            )
        }
    }

    private fun restoreCurrentFragment() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment != null) {
            currentFragment = fragment as MainActivityFragmentCallbacks
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == GENERAL_THEME || key == BLACK_THEME || key == ADAPTIVE_COLOR_APP || key == USER_NAME || key == TOGGLE_FULL_SCREEN || key == TOGGLE_VOLUME || key == ROUND_CORNERS || key == CAROUSEL_EFFECT || key == NOW_PLAYING_SCREEN_ID || key == TOGGLE_GENRE || key == BANNER_IMAGE_PATH || key == PROFILE_IMAGE_PATH || key == CIRCULAR_ALBUM_ART || key == KEEP_SCREEN_ON || key == TOGGLE_SEPARATE_LINE || key == TOGGLE_HOME_BANNER || key == TOGGLE_ADD_CONTROLS || key == ALBUM_COVER_STYLE || key == HOME_ARTIST_GRID_STYLE || key == ALBUM_COVER_TRANSFORM || key == DESATURATED_COLOR || key == EXTRA_SONG_INFO || key == TAB_TEXT_MODE || key == LANGUAGE_NAME || key == LIBRARY_CATEGORIES
        ) {
            postRecreate()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        handlePlaybackIntent(intent)
    }

    private fun handlePlaybackIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        val uri = intent.data
        val mimeType = intent.type
        var handled = false
        if (intent.action != null && (intent.action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
        ) {
            val songs: List<Song> =
                getSongs(this, intent.extras!!)
            if (shuffleMode == MusicService.SHUFFLE_MODE_SHUFFLE) {
                openAndShuffleQueue(songs, true)
            } else {
                openQueue(songs, 0, true)
            }
            handled = true
        }
        if (uri != null && uri.toString().isNotEmpty()) {
            playFromUri(uri)
            handled = true
        } else if (MediaStore.Audio.Playlists.CONTENT_TYPE == mimeType) {
            val id = parseIdFromIntent(intent, "playlistId", "playlist").toInt()
            if (id >= 0) {
                val position = intent.getIntExtra("position", 0)
                val songs: List<Song> =
                    ArrayList(getPlaylistSongList(this, id))
                openQueue(songs, position, true)
                handled = true
            }
        } else if (MediaStore.Audio.Albums.CONTENT_TYPE == mimeType) {
            val id = parseIdFromIntent(intent, "albumId", "album").toInt()
            if (id >= 0) {
                val position = intent.getIntExtra("position", 0)
                openQueue(getAlbum(this, id).songs!!, position, true)
                handled = true
            }
        } else if (MediaStore.Audio.Artists.CONTENT_TYPE == mimeType) {
            val id = parseIdFromIntent(intent, "artistId", "artist").toInt()
            if (id >= 0) {
                val position = intent.getIntExtra("position", 0)
                openQueue(getArtist(this, id).songs, position, true)
                handled = true
            }
        }
        if (handled) {
            setIntent(Intent())
        }
    }

    private fun parseIdFromIntent(
        intent: Intent, longKey: String,
        stringKey: String
    ): Long {
        var id = intent.getLongExtra(longKey, -1)
        if (id < 0) {
            val idString = intent.getStringExtra(stringKey)
            if (idString != null) {
                try {
                    id = idString.toLong()
                } catch (e: NumberFormatException) {
                    Log.e(TAG, e.message)
                }
            }
        }
        return id
    }

    override fun handleBackPress(): Boolean {
        if (cab != null && cab!!.isActive) {
            cab?.finish()
            return true
        }
        return super.handleBackPress() || currentFragment.handleBackPress()
    }

    override fun openCab(menuRes: Int, callback: MaterialCab.Callback): MaterialCab {
        cab?.let {
            if (it.isActive) it.finish()
        }
        cab = MaterialCab(this, R.id.cab_stub)
            .setMenu(menuRes)
            .setCloseDrawableRes(R.drawable.ic_close)
            .setBackgroundColor(
                RetroColorUtil.shiftBackgroundColorForLightText(
                    resolveColor(
                        this,
                        R.attr.colorSurface
                    )
                )
            )
            .start(callback)
        return cab as MaterialCab
    }
}