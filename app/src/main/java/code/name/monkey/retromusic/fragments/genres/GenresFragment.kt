package code.name.monkey.retromusic.fragments.genres

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.retromusic.EXTRA_GENRE
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.GenreAdapter
import code.name.monkey.retromusic.fragments.ReloadType
import code.name.monkey.retromusic.fragments.base.AbsRecyclerViewFragment
import code.name.monkey.retromusic.interfaces.IGenreClickListener
import code.name.monkey.retromusic.model.Genre
import code.name.monkey.retromusic.util.RetroUtil
import com.google.android.material.transition.MaterialSharedAxis

class
GenresFragment : AbsRecyclerViewFragment<GenreAdapter, LinearLayoutManager>(),
    IGenreClickListener {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getGenre().observe(viewLifecycleOwner) {
            if (it.isNotEmpty())
                adapter?.swapDataSet(it)
            else
                adapter?.swapDataSet(listOf())
        }
    }

    override fun createLayoutManager(): LinearLayoutManager {
        return if (RetroUtil.isLandscape) {
            GridLayoutManager(/* context = */ activity, /* spanCount = */ 4)
        } else {
            GridLayoutManager(/* context = */ activity, /* spanCount = */ 2)
        }
    }

    override fun createAdapter(): GenreAdapter {
        val dataSet = if (adapter == null) ArrayList() else adapter!!.dataSet
        return GenreAdapter(activity = requireActivity(), dataSet = dataSet, listener = this)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        menu.removeItem(R.id.action_grid_size)
        menu.removeItem(R.id.action_layout_type)
        menu.removeItem(R.id.action_sort_order)
        menu.findItem(R.id.action_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Genres)
    }


    override val titleRes: Int
        get() = R.string.genres

    override val emptyMessage: Int
        get() = R.string.no_genres

    override val isShuffleVisible: Boolean
        get() = false

    companion object {
        @JvmField
        val TAG: String = GenresFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(): GenresFragment {
            return GenresFragment()
        }
    }

    override fun onClickGenre(genre: Genre, view: View) {
        exitTransition =
            MaterialSharedAxis(/* axis = */ MaterialSharedAxis.Z, /* forward = */ true).addTarget(
                requireView()
            )
        reenterTransition =
            MaterialSharedAxis(/* axis = */ MaterialSharedAxis.Z, /* forward = */ false)
        findNavController().navigate(
            resId = R.id.genreDetailsFragment,
            args = bundleOf(EXTRA_GENRE to genre)
        )
    }
}
