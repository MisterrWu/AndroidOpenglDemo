package com.example.demo

import android.os.Bundle
import android.view.Surface
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.demo.databinding.ActivityPlayerBinding
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private var backgroundPlayer: ExoPlayer? = null
    private var mediaPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        createPlayer()
        setupRenderer()
    }

    private fun createPlayer() {
        if(MediaManager.backgroundMedias.isNotEmpty()) {
            backgroundPlayer = ExoPlayer.Builder(this, DefaultRenderersFactory(this)).build()
            MediaManager.backgroundMedias.also { medias ->
                if(medias.size > 1) {
                    val mediaSourceFactory = DefaultMediaSourceFactory(this)
                    val mediaSourceList = mutableListOf<MediaSource>()
                    medias.forEach { media ->
                        mediaSourceList.add(mediaSourceFactory.createMediaSource(MediaItem.fromUri(media.path)))
                    }
                    backgroundPlayer?.setMediaSource(ConcatenatingMediaSource(*mediaSourceList.toTypedArray()))
                } else {
                    backgroundPlayer?.setMediaItem(MediaItem.fromUri(medias[0].path))
                }
            }
            backgroundPlayer?.repeatMode = Player.REPEAT_MODE_ALL
            backgroundPlayer?.playWhenReady = true
        }
        if(MediaManager.frontMedias.isNotEmpty()) {
            mediaPlayer = ExoPlayer.Builder(this, DefaultRenderersFactory(this)).build()
            mediaPlayer?.setMediaItem(MediaItem.fromUri(MediaManager.frontMedias[0].path))
            mediaPlayer?.repeatMode = Player.REPEAT_MODE_ALL
            mediaPlayer?.playWhenReady = true
        }
    }

    private fun setupRenderer() {
        val renderer = SampleRenderer(this)
        renderer.setTextureSurfaceListener(object : SampleRenderer.TextureSurfaceListener{
            override fun onSurfaceCreated(surface1: Surface, surface2: Surface) {
                if(!isFinishing) {
                    binding.root.post {
                        startPlayer(backgroundPlayer, surface1)
                        startPlayer(mediaPlayer, surface2)
                    }
                }
            }
        })
        binding.glSurfaceView.setEGLContextClientVersion(3)
        binding.glSurfaceView.setRenderer(renderer)
    }

    fun startPlayer(player: ExoPlayer?, surface: Surface) {
        player?.setVideoSurface(surface)
        player?.prepare()
    }

    override fun onResume() {
        super.onResume()
        backgroundPlayer?.play()
        mediaPlayer?.play()
    }

    override fun onPause() {
        super.onPause()
        backgroundPlayer?.pause()
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        try {
            backgroundPlayer?.release()
        } catch (e:Exception) {
            e.printStackTrace()
        }
        try {
            mediaPlayer?.release()
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }
}