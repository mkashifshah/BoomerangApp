package com.example.boomerangapp.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.boomerangapp.databinding.FragmentProfileBinding
import com.example.boomerangapp.databinding.VideoGridItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var videosAdapter: ProfileVideoAdapter
    private val videos = mutableListOf<Video>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        videosAdapter = ProfileVideoAdapter(videos)
        binding.videosGrid.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = videosAdapter
        }
        
        loadUserProfile()
        loadUserVideos()
        
        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_settingsFragment)
        }
        
        binding.profileImage.setOnClickListener {
            changeProfilePicture()
        }
    }
    
    private fun loadUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        
        binding.usernameText.text = currentUser.displayName ?: "User"
        
        if (currentUser.photoUrl != null) {
            Glide.with(this)
                .load(currentUser.photoUrl)
                .circleCrop()
                .into(binding.profileImage)
        }
    }
    
    private fun loadUserVideos() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        FirebaseFirestore.getInstance().collection("videos")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                videos.clear()
                for (document in documents) {
                    val video = document.toObject(Video::class.java)
                    videos.add(video)
                }
                videosAdapter.notifyDataSetChanged()
                binding.videosCountText.text = "Videos: ${videos.size}"
            }
    }
    
    private fun changeProfilePicture() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            data?.data?.let { imageUri ->
                uploadProfilePicture(imageUri)
            }
        }
    }
    
    private fun uploadProfilePicture(imageUri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val storageRef = FirebaseStorage.getInstance().reference
        val profileImageRef = storageRef.child("profile_images/${currentUser.uid}.jpg")
        
        profileImageRef.putFile(imageUri)
            .addOnSuccessListener {
                profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateUserProfile(uri)
                }
            }
    }
    
    private fun updateUserProfile(imageUrl: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setPhotoUri(imageUrl)
            .build()
        
        currentUser.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Glide.with(this)
                        .load(imageUrl)
                        .circleCrop()
                        .into(binding.profileImage)
                    
                    // Also save to Firestore
                    FirebaseFirestore.getInstance().collection("users")
                        .document(currentUser.uid)
                        .update("profileImageUrl", imageUrl.toString())
                }
            }
    }
    
    inner class ProfileVideoAdapter(private val videos: List<Video>) : 
        RecyclerView.Adapter<ProfileVideoAdapter.VideoViewHolder>() {
        
        inner class VideoViewHolder(val binding: VideoGridItemBinding) : 
            RecyclerView.ViewHolder(binding.root)
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
            val binding = VideoGridItemBinding.inflate(
                LayoutInflater.from(parent.context), 
                parent, 
                false
            )
            return VideoViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
            val video = videos[position]
            
            Glide.with(requireContext())
                .load(video.videoUrl)
                .thumbnail(0.1f)
                .into(holder.binding.videoThumbnail)
            
            holder.itemView.setOnClickListener {
                // Open video in full screen
                val action = ProfileFragmentDirections.actionNavigationProfileToVideoDetailFragment(video.id)
                findNavController().navigate(action)
            }
        }
        
        override fun getItemCount() = videos.size
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
    }
}
