package com.example.boomerangapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.boomerangapp.databinding.FragmentHomeBinding
import com.example.boomerangapp.databinding.VideoItemBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var videoAdapter: VideoAdapter
    private val videos = mutableListOf<Video>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        videoAdapter = VideoAdapter(videos)
        binding.videoRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = videoAdapter
        }
        
        loadVideos()
        
        binding.dailyRecapButton.setOnClickListener {
            // Show daily recap
        }
    }
    
    private fun loadVideos() {
        val db = FirebaseFirestore.getInstance()
        db.collection("videos")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { documents ->
                videos.clear()
                for (document in documents) {
                    val video = document.toObject(Video::class.java)
                    videos.add(video)
                }
                videoAdapter.notifyDataSetChanged()
            }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    inner class VideoAdapter(private val videos: List<Video>) : 
        RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {
        
        inner class VideoViewHolder(val binding: VideoItemBinding) : 
            RecyclerView.ViewHolder(binding.root)
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
            val binding = VideoItemBinding.inflate(
                LayoutInflater.from(parent.context), 
                parent, 
                false
            )
            return VideoViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
            val video = videos[position]
            holder.binding.apply {
                // Load video with ExoPlayer
                setupVideoPlayer(video.videoUrl)
                
                captionTextView.text = video.caption
                usernameTextView.text = video.username
                
                likeButton.setOnClickListener {
                    // Handle like
                }
                
                commentButton.setOnClickListener {
                    // Open comments
                }
            }
        }
        
        override fun getItemCount() = videos.size
    }
    
    private fun VideoItemBinding.setupVideoPlayer(videoUrl: String) {
        val player = ExoPlayer.Builder(requireContext()).build()
        videoPlayer.player = player
        
        val mediaItem = MediaItem.fromUri(videoUrl)
        player.setMediaItem(mediaItem)
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.prepare()
        player.play()
    }
}
