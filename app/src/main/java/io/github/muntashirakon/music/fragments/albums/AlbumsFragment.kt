package io.github.muntashirakon.music.fragments.albums

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.GridLayoutManager
import io.github.muntashirakon.music.EXTRA_ALBUM_ID
import io.github.muntashirakon.music.R
import io.github.muntashirakon.music.adapter.album.AlbumAdapter
import io.github.muntashirakon.music.fragments.ReloadType
import io.github.muntashirakon.music.fragments.base.AbsRecyclerViewCustomGridSizeFragment
import io.github.muntashirakon.music.util.PreferenceUtil

class AlbumsFragment :
    AbsRecyclerViewCustomGridSizeFragment<AlbumAdapter, GridLayoutManager>(),
    AlbumClickListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.albumsLiveData.observe(viewLifecycleOwner, Observer {
            if (it.isNotEmpty())
                adapter?.swapDataSet(it)
            else
                adapter?.swapDataSet(listOf())
        })
    }

    override val emptyMessage: Int
        get() = R.string.no_albums

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(requireActivity(), getGridSize())
    }

    override fun createAdapter(): AlbumAdapter {
        val dataSet = if (adapter == null) ArrayList() else adapter!!.dataSet
        return AlbumAdapter(
            requireActivity(),
            dataSet,
            R.layout.item_grid,
            null,
            this
        )
    }

    override fun setGridSize(gridSize: Int) {
        layoutManager?.spanCount = gridSize
        adapter?.notifyDataSetChanged()
    }

    override fun loadSortOrder(): String {
        return PreferenceUtil.albumSortOrder
    }

    override fun saveSortOrder(sortOrder: String) {
        PreferenceUtil.albumSortOrder = sortOrder
    }

    override fun loadGridSize(): Int {
        return PreferenceUtil.albumGridSize
    }

    override fun saveGridSize(gridColumns: Int) {
        PreferenceUtil.albumGridSize = gridColumns
    }

    override fun loadGridSizeLand(): Int {
        return PreferenceUtil.albumGridSizeLand
    }

    override fun saveGridSizeLand(gridColumns: Int) {
        PreferenceUtil.albumGridSizeLand = gridColumns
    }

    override fun setSortOrder(sortOrder: String) {
        libraryViewModel.forceReload(ReloadType.Albums)
    }

    override fun loadLayoutRes(): Int {
        return PreferenceUtil.albumGridStyle
    }

    override fun saveLayoutRes(layoutRes: Int) {
        PreferenceUtil.albumGridStyle = layoutRes
    }


    companion object {
        fun newInstance(): AlbumsFragment {
            return AlbumsFragment()
        }
    }

    override fun onAlbumClick(albumId: Int, view: View) {
        val controller = requireActivity().findNavController(R.id.fragment_container)
        controller.navigate(
            R.id.albumDetailsFragment,
            bundleOf(EXTRA_ALBUM_ID to albumId),
            null,
            FragmentNavigatorExtras(
                view to getString(R.string.transition_album_art)
            )
        )
    }
}

interface AlbumClickListener {
    fun onAlbumClick(albumId: Int, view: View)
}