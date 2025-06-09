package com.example.boomerangapp.ui.friends

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.boomerangapp.databinding.FriendItemBinding
import com.example.boomerangapp.databinding.FragmentFriendsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FriendsFragment : Fragment() {
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!
    private lateinit var friendsAdapter: FriendsAdapter
    private val friends = mutableListOf<User>()
    private val friendRequests = mutableListOf<User>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        friendsAdapter = FriendsAdapter(friends)
        binding.friendsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = friendsAdapter
        }
        
        binding.shareProfileButton.setOnClickListener {
            shareProfile()
        }
        
        loadFriends()
        loadFriendRequests()
    }
    
    private fun loadFriends() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        FirebaseFirestore.getInstance().collection("users")
            .document(currentUserId)
            .collection("friends")
            .get()
            .addOnSuccessListener { documents ->
                friends.clear()
                for (document in documents) {
                    val user = document.toObject(User::class.java)
                    friends.add(user)
                }
                binding.friendsCountText.text = "Friends: ${friends.size}"
                friendsAdapter.notifyDataSetChanged()
            }
    }
    
    private fun loadFriendRequests() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        FirebaseFirestore.getInstance().collection("friend_requests")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                friendRequests.clear()
                for (document in documents) {
                    val request = document.toObject(FriendRequest::class.java)
                    FirebaseFirestore.getInstance().collection("users")
                        .document(request.senderId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val user = userDoc.toObject(User::class.java)
                            user?.let { friendRequests.add(it) }
                        }
                }
            }
    }
    
    private fun shareProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val profileLink = "https://boomerangapp.com/profile/${currentUser.uid}"
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out my Boomerang profile: $profileLink")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Profile"))
    }
    
    inner class FriendsAdapter(private val friends: List<User>) : 
        RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {
        
        inner class FriendViewHolder(val binding: FriendItemBinding) : 
            RecyclerView.ViewHolder(binding.root)
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
            val binding = FriendItemBinding.inflate(
                LayoutInflater.from(parent.context), 
                parent, 
                false
            )
            return FriendViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
            val friend = friends[position]
            holder.binding.apply {
                usernameText.text = friend.username
                
                Glide.with(requireContext())
                    .load(friend.profileImageUrl)
                    .circleCrop()
                    .into(profileImage)
                
                // Handle friend requests if needed
                if (friendRequests.contains(friend)) {
                    acceptButton.visibility = View.VISIBLE
                    rejectButton.visibility = View.VISIBLE
                    
                    acceptButton.setOnClickListener {
                        acceptFriendRequest(friend.id)
                    }
                    
                    rejectButton.setOnClickListener {
                        rejectFriendRequest(friend.id)
                    }
                } else {
                    acceptButton.visibility = View.GONE
                    rejectButton.visibility = View.GONE
                }
            }
        }
        
        override fun getItemCount() = friends.size
    }
    
    private fun acceptFriendRequest(friendId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        // Update request status
        FirebaseFirestore.getInstance().collection("friend_requests")
            .whereEqualTo("senderId", friendId)
            .whereEqualTo("receiverId", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.update("status", "accepted")
                }
                
                // Add to each other's friends list
                val batch = FirebaseFirestore.getInstance().batch()
                
                val currentUserRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                
                val friendRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(friendId)
                
                // Add friend to current user's friends
                batch.set(
                    currentUserRef.collection("friends").document(friendId),
                    hashMapOf("id" to friendId, "timestamp" to FieldValue.serverTimestamp())
                )
                
                // Add current user to friend's friends
                batch.set(
                    friendRef.collection("friends").document(currentUserId),
                    hashMapOf("id" to currentUserId, "timestamp" to FieldValue.serverTimestamp())
                )
                
                batch.commit().addOnSuccessListener {
                    loadFriends()
                    loadFriendRequests()
                }
            }
    }
    
    private fun rejectFriendRequest(friendId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        FirebaseFirestore.getInstance().collection("friend_requests")
            .whereEqualTo("senderId", friendId)
            .whereEqualTo("receiverId", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                }
                loadFriendRequests()
            }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
