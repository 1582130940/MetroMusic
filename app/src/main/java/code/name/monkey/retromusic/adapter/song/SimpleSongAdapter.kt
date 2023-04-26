package code.name.monkey.retromusic.adapter.song

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil

class SimpleSongAdapter(
    context: FragmentActivity,
    songs: ArrayList<Song>,
    layoutRes: Int,
) : SongAdapter(context, songs, layoutRes) {

    @SuppressLint("NotifyDataSetChanged")
    override fun swapDataSet(dataSet: List<Song>) {
        this.dataSet = dataSet.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(activity).inflate(
                /* resource = */ itemLayoutRes,
                /* root = */ parent,
                /* attachToRoot = */ false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val fixedTrackNumber = MusicUtil.getFixedTrackNumber(dataSet[position].trackNumber)
        val trackAndTime = (if (fixedTrackNumber > 0) "$fixedTrackNumber | " else "") +
                MusicUtil.getReadableDurationString(dataSet[position].duration)

        holder.time?.text = trackAndTime
        holder.text2?.text = dataSet[position].artistName
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}
