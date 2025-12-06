package com.kernelflux.anifluxsample.test.memoryleak.recyclerview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kernelflux.aniflux.AniFlux
import com.kernelflux.aniflux.into
import com.kernelflux.anifluxsample.R
import com.kernelflux.pag.PAGImageView

/**
 * RecyclerView scroll test scenario
 * Tests memory leak prevention when views are recycled in RecyclerView
 */
class RecyclerViewTestFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvInfo: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_recycler_view_test, container, false)
        recyclerView = view.findViewById(R.id.recycler_view)
        tvInfo = view.findViewById(R.id.tv_info)
        
        setupRecyclerView()
        updateInfo()
        
        return view
    }

    private fun setupRecyclerView() {
        val items = (1..100).map { "Item $it" }
        val adapter = AnimationRecyclerAdapter(items)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun updateInfo() {
        tvInfo.text = """
            RecyclerView Scroll Test
            ========================
            • Scroll up and down to trigger view recycling
            • Check if animations are paused when views are detached
            • Verify resources are not released during recycling
            • Monitor memory usage during scrolling
        """.trimIndent()
    }
}

/**
 * RecyclerView adapter with animation items
 */
class AnimationRecyclerAdapter(private val items: List<String>) :
    RecyclerView.Adapter<AnimationRecyclerAdapter.ViewHolder>() {

    private val pagUrl = "https://peanut-oss.wemogu.net/client/test/anim_linglu.pag"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recycler_animation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], pagUrl)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val pagImageView: PAGImageView = itemView.findViewById(R.id.pag_image_view)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)

        fun bind(title: String, url: String) {
            tvTitle.text = title
            
            // Load animation using AniFlux
            AniFlux.with(itemView.context)
                .asPAG()
                .load(url)
                .into(pagImageView)
        }
    }
}

