package com.moxi.app.di

import android.content.Context
import timber.log.Timber
import com.moxi.app.core.StremioCore
import com.moxi.app.player.PlaybackManager
import com.moxi.app.server.StubStreamingServerController
import com.moxi.app.server.StreamingServerController
import com.moxi.app.server.JniStreamingServerController

import com.moxi.app.data.repository.AddonRepository
import com.moxi.app.data.repository.AuthRepository
import com.moxi.app.data.repository.BoardRepository
import com.moxi.app.data.repository.CatalogRepository
import com.moxi.app.data.repository.PlaybackRepository
import com.moxi.app.update.ApkInstaller
import com.moxi.app.update.UpdateRepository

class AppContainer(context: Context) {
    val playbackManager = PlaybackManager(context.applicationContext)
    val core: StremioCore = StremioCore(context.applicationContext).apply {
        try {
            initialize()
        } catch (e: Throwable) {
            Timber.e(e, "Failed to initialize stremio-core")
        }
    }

    val authRepository = AuthRepository(core, context.applicationContext)
    val boardRepository = BoardRepository(core)
    val catalogRepository = CatalogRepository(core)
    val addonRepository = AddonRepository(core)
    val playbackRepository = PlaybackRepository(core, playbackManager)
    val updateRepository = UpdateRepository(context.applicationContext)
    val apkInstaller = ApkInstaller(context.applicationContext)

    val serverController: StreamingServerController = try {
        JniStreamingServerController(context, useForegroundService = { authRepository.isServerInForeground() })
    } catch (e: UnsatisfiedLinkError) {
        Timber.e(e, "Failed to load stream_server JNI, falling back to Stub")
        StubStreamingServerController()
    }

}

