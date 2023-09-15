package com.katilijiwoadiwiyono.filterrecord.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.katilijiwoadiwiyono.filterrecord.databinding.VideoQualityItemBinding


typealias BindCallback = (view: VideoQualityItemBinding, data: String, position: Int) -> Unit

class VideoQualityAdapter(
    private val dataset: List<String>,
    private val onBind: BindCallback
) : RecyclerView.Adapter<VideoQualityAdapter.VideoQualityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VideoQualityViewHolder(
        VideoQualityItemBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VideoQualityViewHolder, position: Int) {
        if (position < 0 || position > dataset.size) return
        onBind(holder.binding, dataset[position], position)
    }

    override fun getItemCount() = dataset.size

    class VideoQualityViewHolder(val binding: VideoQualityItemBinding) : RecyclerView.ViewHolder(binding.root)
}