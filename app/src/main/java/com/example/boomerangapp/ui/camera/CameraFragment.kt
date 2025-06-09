package com.example.boomerangapp.ui.camera

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.boomerangapp.databinding.FragmentCameraBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Timestamp
import com.google.firebase.storage.FirebaseStorage
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
// CameraX video capture
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent

class CameraFragment : Fragment() {
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private lateinit var cameraExecutor: ExecutorService
    private var videoFile: File? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        binding.recordButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> startRecording()
                MotionEvent.ACTION_UP -> stopRecording()
            }
            true
        }
        
        startCamera()
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, 
                    cameraSelector, 
                    preview
                )
            } catch(exc: Exception) {
                Log.e("CameraFragment", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    
    private fun startRecording() {
        val outputOptions = FileOutputOptions.Builder(
            File(requireContext().filesDir, "boomerang_temp.mp4")
        ).build()
        
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        val videoCapture = VideoCapture.withOutput(recorder)
        
        videoFile = File(requireContext().filesDir, "boomerang_${System.currentTimeMillis()}.mp4")
        
        val recording = recorder.prepareRecording(requireContext(), outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(requireContext())) { event ->
                when(event) {
                    is VideoRecordEvent.Start -> {
                        // Recording started
                        binding.recordButton.setBackgroundColor(Color.RED)
                        
                        // Auto-stop after 1 second
                        Handler(Looper.getMainLooper()).postDelayed({
                            stopRecording()
                        }, 1000)
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            // Handle error
                        } else {
                            // Apply boomerang effect
                            applyBoomerangEffect(event.outputResults.outputUri)
                        }
                    }
                }
            }
    }
    
    private fun stopRecording() {
        // Handled by the 1-second timeout in startRecording
    }
    
    private fun applyBoomerangEffect(videoUri: Uri) {
        val outputFile = File(requireContext().filesDir, "boomerang_${System.currentTimeMillis()}.mp4")
        
        FFmpegKit.executeAsync(
            "-i $videoUri -filter_complex \"[0]reverse[r];[0][r]concat=n=2:v=1:a=1\" ${outputFile.absolutePath}",
            { session ->
                if (ReturnCode.isSuccess(session.returnCode)) {
                    // Upload to Firebase
                    uploadVideo(outputFile)
                }
            }
        )
    }
    
    private fun uploadVideo(videoFile: File) {
        val storageRef = FirebaseStorage.getInstance().reference
        val videoRef = storageRef.child("videos/${videoFile.name}")
        
        videoRef.putFile(Uri.fromFile(videoFile))
            .addOnSuccessListener {
                videoRef.downloadUrl.addOnSuccessListener { uri ->
                    saveVideoToFirestore(uri.toString())
                }
            }
    }
    
    private fun saveVideoToFirestore(videoUrl: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val video = Video(
            id = UUID.randomUUID().toString(),
            userId = currentUser?.uid ?: "",
            username = currentUser?.displayName ?: "Anonymous",
            videoUrl = videoUrl,
            timestamp = Timestamp.now(),
            likes = 0,
            comments = emptyList(),
            caption = ""
        )
        
        FirebaseFirestore.getInstance().collection("videos")
            .document(video.id)
            .set(video)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Video uploaded!", Toast.LENGTH_SHORT).show()
            }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
