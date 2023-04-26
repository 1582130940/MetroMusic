package code.name.monkey.retromusic.fragments.folder

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.os.BundleCompat
import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.appthemehelper.ThemeStore.Companion.accentColor
import code.name.monkey.appthemehelper.common.ATHToolbarActivity
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.SongFileAdapter
import code.name.monkey.retromusic.adapter.Storage
import code.name.monkey.retromusic.adapter.StorageAdapter
import code.name.monkey.retromusic.adapter.StorageClickListener
import code.name.monkey.retromusic.databinding.FragmentFolderBinding
import code.name.monkey.retromusic.extensions.dip
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.extensions.textColorPrimary
import code.name.monkey.retromusic.extensions.textColorSecondary
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import code.name.monkey.retromusic.helper.MusicPlayerRemote.openQueue
import code.name.monkey.retromusic.helper.menu.SongMenuHelper
import code.name.monkey.retromusic.helper.menu.SongsMenuHelper
import code.name.monkey.retromusic.interfaces.ICallbacks
import code.name.monkey.retromusic.interfaces.IMainActivityFragmentCallbacks
import code.name.monkey.retromusic.interfaces.IScrollHelper
import code.name.monkey.retromusic.misc.UpdateToastMediaScannerCompletionListener
import code.name.monkey.retromusic.misc.WrappedAsyncTaskLoader
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.providers.BlacklistStore
import code.name.monkey.retromusic.util.FileUtil
import code.name.monkey.retromusic.util.PreferenceUtil.startDirectory
import code.name.monkey.retromusic.util.ThemedFastScroller.create
import code.name.monkey.retromusic.util.getExternalStorageDirectory
import code.name.monkey.retromusic.util.getExternalStoragePublicDirectory
import code.name.monkey.retromusic.views.BreadCrumbLayout
import code.name.monkey.retromusic.views.BreadCrumbLayout.Crumb
import code.name.monkey.retromusic.views.BreadCrumbLayout.SelectionCallback
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.LinkedList

class FoldersFragment : AbsMainActivityFragment(R.layout.fragment_folder),
    IMainActivityFragmentCallbacks, SelectionCallback, ICallbacks,
    LoaderManager.LoaderCallbacks<List<File>>, StorageClickListener, IScrollHelper {
    private var _binding: FragmentFolderBinding? = null
    private val binding get() = _binding!!

    val toolbar: Toolbar get() = binding.appBarLayout.toolbar

    private var adapter: SongFileAdapter? = null
    private var storageAdapter: StorageAdapter? = null
    private val fileComparator = Comparator { lhs: File, rhs: File ->
        if (lhs.isDirectory && !rhs.isDirectory) {
            return@Comparator -1
        } else if (!lhs.isDirectory && rhs.isDirectory) {
            return@Comparator 1
        } else {
            return@Comparator lhs.name.compareTo(rhs.name, ignoreCase = true)
        }
    }
    private var storageItems = ArrayList<Storage>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFolderBinding.bind(view)
        mainActivity.addMusicServiceEventListener(libraryViewModel)
        mainActivity.setSupportActionBar(toolbar)
        mainActivity.supportActionBar?.title = null
        enterTransition = MaterialFadeThrough()
        reenterTransition = MaterialFadeThrough()

        setUpBreadCrumbs()
        checkForMargins()
        setUpRecyclerView()
        setUpAdapter()
        setUpTitle()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(/* enabled = */ true) {
                override fun handleOnBackPressed() {
                    if (!handleBackPress()) {
                        remove()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            })
        if (savedInstanceState == null) {
            switchToFileAdapter()
            setCrumb(
                crumb = Crumb(
                    /* file = */ FileUtil.safeGetCanonicalFile(startDirectory)
                ),
                addToHistory = true
            )
        } else {
            binding.breadCrumbs.restoreFromStateWrapper(
                BundleCompat.getParcelable(
                    /* in = */ savedInstanceState,
                    /* key = */ CRUMBS,
                    /* clazz = */ BreadCrumbLayout.SavedStateWrapper::class.java
                )
            )
            LoaderManager.getInstance(/* owner = */ this).initLoader(
                /* id = */ LOADER_ID,
                /* args = */ null,
                /* callback = */ this
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (_binding != null) {
            outState.putParcelable(/* key = */ CRUMBS, /* value = */
                binding.breadCrumbs.stateWrapper
            )
        }
    }

    private fun setUpTitle() {
        toolbar.setNavigationOnClickListener {
            findNavController().navigate(
                resId = R.id.action_search,
                args = null,
                navOptions = navOptions
            )
        }
        binding.appBarLayout.title = resources.getString(R.string.folders)
    }

    override fun onPause() {
        super.onPause()
        saveScrollPosition()
        adapter?.actionMode?.finish()
    }

    override fun handleBackPress(): Boolean {
        if (binding.breadCrumbs.popHistory()) {
            setCrumb(crumb = binding.breadCrumbs.lastHistory(), addToHistory = false)
            return true
        }
        return false
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<File>> {
        return AsyncFileLoader(foldersFragment = this)
    }

    override fun onCrumbSelection(crumb: Crumb, index: Int) {
        setCrumb(crumb = crumb, addToHistory = true)
    }

    override fun onFileMenuClicked(file: File, view: View) {
        val popupMenu = PopupMenu(requireActivity(), view)
        if (file.isDirectory) {
            popupMenu.inflate(/* menuRes = */ R.menu.menu_item_directory)
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (val itemId = item.itemId) {
                    R.id.action_play_next, R.id.action_add_to_current_playing, R.id.action_add_to_playlist, R.id.action_delete_from_device -> {
                        lifecycleScope.launch(Dispatchers.IO) {
                            listSongs(
                                context = requireContext(),
                                files = listOf(file),
                                fileFilter = AUDIO_FILE_FILTER,
                                fileComparator = fileComparator
                            ) { songs ->
                                if (songs.isNotEmpty()) {
                                    SongsMenuHelper.handleMenuClick(
                                        activity = requireActivity(),
                                        songs = songs,
                                        menuItemId = itemId
                                    )
                                }
                            }
                        }
                        return@setOnMenuItemClickListener true
                    }

                    R.id.action_add_to_blacklist -> {
                        BlacklistStore.getInstance(requireContext()).addPath(file)
                        return@setOnMenuItemClickListener true
                    }

                    R.id.action_set_as_start_directory -> {
                        startDirectory = file
                        showToast(
                            String.format(
                                getString(/* resId = */ R.string.new_start_directory),
                                file.path
                            )
                        )
                        return@setOnMenuItemClickListener true
                    }

                    R.id.action_scan -> {
                        lifecycleScope.launch {
                            listPaths(file, AUDIO_FILE_FILTER) { paths -> scanPaths(paths) }
                        }
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
        } else {
            popupMenu.inflate(R.menu.menu_item_file)
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (val itemId = item.itemId) {
                    R.id.action_play_next, R.id.action_add_to_current_playing, R.id.action_add_to_playlist, R.id.action_go_to_album, R.id.action_go_to_artist, R.id.action_share, R.id.action_tag_editor, R.id.action_details, R.id.action_set_as_ringtone, R.id.action_delete_from_device -> {
                        lifecycleScope.launch(Dispatchers.IO) {
                            listSongs(
                                context = requireContext(),
                                files = listOf(file),
                                fileFilter = AUDIO_FILE_FILTER,
                                fileComparator = fileComparator
                            ) { songs ->
                                if (songs.isNotEmpty()) {
                                    val song = songs.first()
                                    SongMenuHelper.handleMenuClick(
                                        activity = requireActivity(),
                                        song = song,
                                        menuItemId = itemId
                                    )
                                }
                            }
                        }
                        return@setOnMenuItemClickListener true
                    }

                    R.id.action_scan -> {
                        lifecycleScope.launch {
                            listPaths(file, AUDIO_FILE_FILTER) { paths -> scanPaths(paths) }
                        }
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
        }
        popupMenu.show()
    }

    override fun onFileSelected(file: File) {
        var mFile = file
        mFile = tryGetCanonicalFile(mFile) // important as we compare the path value later
        if (mFile.isDirectory) {
            setCrumb(crumb = Crumb(mFile), addToHistory = true)
        } else {
            val fileFilter = FileFilter { pathname: File ->
                !pathname.isDirectory && AUDIO_FILE_FILTER.accept(pathname)
            }
            lifecycleScope.launch(Dispatchers.IO) {
                listSongs(
                    context = requireContext(),
                    files = listOf(mFile.parentFile),
                    fileFilter = fileFilter,
                    fileComparator = fileComparator
                ) { songs ->
                    if (songs.isNotEmpty()) {
                        var startIndex = -1
                        for (i in songs.indices) {
                            if (mFile.path
                                == songs[i].data
                            ) { // path is already canonical here
                                startIndex = i
                                break
                            }
                        }
                        if (startIndex > -1) {
                            openQueue(
                                queue = songs,
                                startPosition = startIndex,
                                startPlaying = true
                            )
                        } else {
                            Snackbar.make(
                                mainActivity.slidingPanel,
                                String.format(
                                    getString(R.string.not_listed_in_media_store), mFile.name

                                ).parseAsHtml(),
                                Snackbar.LENGTH_LONG
                            )
                                .setAction(
                                    R.string.action_scan
                                ) {
                                    lifecycleScope.launch {
                                        listPaths(mFile, AUDIO_FILE_FILTER) { paths ->
                                            scanPaths(
                                                toBeScanned = paths
                                            )
                                        }
                                    }
                                }
                                .setActionTextColor(accentColor(requireActivity()))
                                .show()
                        }
                    }
                }
            }
        }
    }

    override fun onLoadFinished(
        loader: Loader<List<File>>,
        data: List<File>,
    ) {
        updateAdapter(data)
    }

    override fun onLoaderReset(loader: Loader<List<File>>) {
        updateAdapter(LinkedList())
    }

    override fun onMultipleItemAction(
        item: MenuItem,
        files: ArrayList<File>,
    ) {
        val itemId = item.itemId

        lifecycleScope.launch(Dispatchers.IO) {
            listSongs(requireContext(), files, AUDIO_FILE_FILTER, fileComparator) { songs ->
                if (songs.isNotEmpty()) {
                    SongsMenuHelper.handleMenuClick(
                        activity = requireActivity(), songs = songs, menuItemId = itemId
                    )
                }
            }
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        ToolbarContentTintHelper.handleOnPrepareOptionsMenu(/* activity = */ requireActivity(), /* toolbar = */
            toolbar
        )
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(
            /* p0 = */ 0,
            /* p1 = */ R.id.action_scan,
            /* p2 = */ 0,
            /* p3 = */ R.string.scan_media
        )
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(
            /* p0 = */ 0,
            /* p1 = */ R.id.action_go_to_start_directory,
            /* p2 = */ 1,
            /* p3 = */ R.string.action_go_to_start_directory
        )
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(
            /* p0 = */ 0,
            /* p1 = */ R.id.action_settings,
            /* p2 = */ 2,
            /* p3 = */ R.string.action_settings
        )
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.removeItem(/* p0 = */ R.id.action_grid_size)
        menu.removeItem(/* p0 = */ R.id.action_layout_type)
        menu.removeItem(/* p0 = */ R.id.action_sort_order)
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(
            /* context = */ requireContext(),
            /* toolbar = */ toolbar,
            /* menu = */ menu,
            /* toolbarColor = */ ATHToolbarActivity.getToolbarBackgroundColor(
                /* toolbar = */ toolbar
            )
        )
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_go_to_start_directory -> {
                setCrumb(
                    crumb = Crumb(
                        tryGetCanonicalFile(startDirectory)
                    ),
                    addToHistory = true
                )
                return true
            }

            R.id.action_scan -> {
                val crumb = activeCrumb
                if (crumb != null) {
                    lifecycleScope.launch {
                        listPaths(
                            file = crumb.file,
                            fileFilter = AUDIO_FILE_FILTER
                        ) { paths -> scanPaths(paths) }
                    }
                }
                return true
            }

            R.id.action_settings -> {
                findNavController().navigate(
                    resId = R.id.settings_fragment,
                    args = null,
                    navOptions = navOptions
                )
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        checkForMargins()
    }

    private fun checkForMargins() {
        if (mainActivity.isBottomNavVisible) {
            binding.recyclerView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = dip(R.dimen.bottom_nav_height)
            }
        }
    }

    private fun checkIsEmpty() {
        if (_binding != null) {
            binding.emptyEmoji.text = getEmojiByUnicode(unicode = 0x1F631)
            binding.empty.isVisible = adapter?.itemCount == 0
        }
    }

    private val activeCrumb: Crumb?
        get() = if (_binding != null) {
            if (binding.breadCrumbs.size() > 0) binding.breadCrumbs.getCrumb(binding.breadCrumbs.activeIndex) else null
        } else null

    private fun getEmojiByUnicode(unicode: Int): String {
        return String(Character.toChars(unicode))
    }

    private fun saveScrollPosition() {
        activeCrumb?.scrollPosition =
            (binding.recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    }


    private fun scanPaths(toBeScanned: Array<String?>) {
        if (activity == null) {
            return
        }
        if (toBeScanned.isEmpty()) {
            showToast(R.string.nothing_to_scan)
        } else {
            MediaScannerConnection.scanFile(
                /* context = */ requireContext(),
                /* paths = */ toBeScanned,
                /* mimeTypes = */ null,
                /* callback = */ UpdateToastMediaScannerCompletionListener(
                    /* activity = */ activity,
                    /* toBeScanned = */ listOf(*toBeScanned)
                )
            )
        }
    }

    private fun setCrumb(crumb: Crumb?, addToHistory: Boolean) {
        if (crumb == null) {
            return
        }
        val path = crumb.file.path
        if (path == "/" || path == "/storage" || path == "/storage/emulated") {
            switchToStorageAdapter()
        } else {
            saveScrollPosition()
            binding.breadCrumbs.setActiveOrAdd(/* crumb = */ crumb, /* forceRecreate = */ false)
            if (addToHistory) {
                binding.breadCrumbs.addHistory(/* crumb = */ crumb)
            }
            LoaderManager.getInstance(/* owner = */ this).restartLoader(
                /* id = */ LOADER_ID,
                /* args = */ null,
                /* callback = */ this
            )
        }
    }

    private fun setUpAdapter() {
        switchToFileAdapter()
    }

    private fun setUpBreadCrumbs() {
        binding.breadCrumbs.setActivatedContentColor(
            textColorPrimary()
        )
        binding.breadCrumbs.setDeactivatedContentColor(
            textColorSecondary()

        )
        binding.breadCrumbs.setCallback(this)
    }

    private fun setUpRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        create(
            binding.recyclerView
        )
    }

    private fun updateAdapter(files: List<File>) {
        adapter?.swapDataSet(files)
        val crumb = activeCrumb
        if (crumb != null) {
            (binding.recyclerView.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(/* position = */ crumb.scrollPosition, /* offset = */ 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private suspend fun listPaths(
        file: File,
        fileFilter: FileFilter,
        doOnPathListed: (paths: Array<String?>) -> Unit,
    ) {
        val paths = try {
            val paths: Array<String?>
            if (file.isDirectory) {
                val files = FileUtil.listFilesDeep(file, fileFilter)
                paths = arrayOfNulls(files.size)
                for (i in files.indices) {
                    val f = files[i]
                    paths[i] = FileUtil.safeGetCanonicalPath(f)
                }
            } else {
                paths = arrayOfNulls(size = 1)
                paths[0] = file.path
            }
            paths
        } catch (e: Exception) {
            e.printStackTrace()
            arrayOf()
        }
        withContext(Dispatchers.Main) {
            doOnPathListed(paths)
        }
    }

    private class AsyncFileLoader(foldersFragment: FoldersFragment) :
        WrappedAsyncTaskLoader<List<File>>(foldersFragment.requireActivity()) {
        private val fragmentWeakReference: WeakReference<FoldersFragment> =
            WeakReference(foldersFragment)

        override fun loadInBackground(): List<File> {
            val foldersFragment = fragmentWeakReference.get()
            var directory: File? = null
            if (foldersFragment != null) {
                val crumb = foldersFragment.activeCrumb
                if (crumb != null) {
                    directory = crumb.file
                }
            }
            return if (directory != null) {
                val files = FileUtil.listFiles(
                    directory,
                    AUDIO_FILE_FILTER
                )
                Collections.sort(/* list = */ files, /* c = */ foldersFragment!!.fileComparator)
                files
            } else {
                LinkedList()
            }
        }
    }

    private suspend fun listSongs(
        context: Context,
        files: List<File?>,
        fileFilter: FileFilter,
        fileComparator: Comparator<File>,
        doOnSongsListed: (songs: List<Song>) -> Unit,
    ) {
        val songs = try {
            val fileList =
                FileUtil.listFilesDeep(/* files = */ files, /* fileFilter = */ fileFilter)
            Collections.sort(/* list = */ fileList, /* c = */ fileComparator)
            FileUtil.matchFilesWithMediaStore(/* context = */ context, /* files = */ fileList)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
        withContext(Dispatchers.Main) {
            doOnSongsListed(songs)
        }
    }

    override fun onStorageClicked(storage: Storage) {
        switchToFileAdapter()
        setCrumb(
            crumb = Crumb(
                FileUtil.safeGetCanonicalFile(storage.file)
            ),
            addToHistory = true
        )
    }

    override fun scrollToTop() {
        binding.recyclerView.scrollToPosition(/* position = */ 0)
        binding.appBarLayout.setExpanded(/* expanded = */ true, /* animate = */ true)
    }

    private fun switchToFileAdapter() {
        adapter = SongFileAdapter(
            activity = mainActivity,
            dataSet = LinkedList(),
            itemLayoutRes = R.layout.item_list,
            iCallbacks = this
        )
        adapter?.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    super.onChanged()
                    checkIsEmpty()
                }
            })
        binding.recyclerView.adapter = adapter
        checkIsEmpty()
    }

    private fun switchToStorageAdapter() {
        storageItems = FileUtil.listRoots()
        storageAdapter = StorageAdapter(storageList = storageItems, storageClickListener = this)
        binding.recyclerView.adapter = storageAdapter
        binding.breadCrumbs.clearCrumbs()
    }

    companion object {
        val TAG: String = FoldersFragment::class.java.simpleName
        val AUDIO_FILE_FILTER = FileFilter { file: File ->
            (!file.isHidden
                    && (file.isDirectory
                    || FileUtil.fileIsMimeType(
                /* file = */ file,
                /* mimeType = */ "audio/*",
                /* mimeTypeMap = */ MimeTypeMap.getSingleton()
            )
                    || FileUtil.fileIsMimeType(
                /* file = */ file,
                /* mimeType = */ "application/opus",
                /* mimeTypeMap = */ MimeTypeMap.getSingleton()
            )
                    || FileUtil.fileIsMimeType(
                /* file = */ file,
                /* mimeType = */ "application/ogg",
                /* mimeTypeMap = */ MimeTypeMap.getSingleton()
            )))
        }
        private const val CRUMBS = "crumbs"
        private const val LOADER_ID = 5

        // root
        val defaultStartDirectory: File
            get() {
                val musicDir =
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val startFolder = if (musicDir.exists() && musicDir.isDirectory) {
                    musicDir
                } else {
                    val externalStorage = getExternalStorageDirectory()
                    if (externalStorage.exists() && externalStorage.isDirectory) {
                        externalStorage
                    } else {
                        File(/* pathname = */ "/") // root
                    }
                }
                return startFolder
            }

        private fun tryGetCanonicalFile(file: File): File {
            return try {
                file.canonicalFile
            } catch (e: IOException) {
                e.printStackTrace()
                file
            }
        }
    }
}
