package sh.swurlz.core.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import sh.swurlz.core.data.MissionBus
import sh.swurlz.core.data.Prefs
import sh.swurlz.core.overlay.ui.OverlayBubble
import sh.swurlz.core.service.MissionRunnerService

/**
 * Floating telemetry overlay.
 * - Sized WRAP_CONTENT so it never blocks the whole screen.
 * - Drag handle (≡ on expanded, the whole puck on collapsed) moves it.
 * - Minimize button collapses to a 56dp puck showing just the ambient dot.
 * - Close button dismisses the overlay (mission keeps running headlessly).
 * - Position + collapsed state persisted to DataStore.
 */
class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val CHANNEL_ID = "swurlz_overlay"
        const val NOTIF_ID = 4243
        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, OverlayService::class.java))
        }
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, OverlayService::class.java))
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle get() = lifecycleRegistry

    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var persistJob: Job? = null
    private lateinit var wm: WindowManager
    private var rootView: View? = null
    private lateinit var lp: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        if (rootView == null) installOverlay()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        return START_STICKY
    }

    override fun onDestroy() {
        rootView?.let { runCatching { wm.removeView(it) } }
        rootView = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    private fun installOverlay() {
        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24; y = 96
        }

        // Restore persisted position synchronously-ish (best-effort; default is fine if missed)
        scope.launch {
            val (px, py) = Prefs.overlayPos(this@OverlayService)
            lp.x = px; lp.y = py
            runCatching { wm.updateViewLayout(rootView, lp) }
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                OverlayBubble(
                    onDrag = { dx, dy -> moveBy(dx, dy) },
                    onDragEnd = { persistPosition() },
                    onPauseToggle = {
                        MissionBus.pauseRequest.value = !MissionBus.pauseRequest.value
                    },
                    onTakeOver = {
                        MissionBus.takeoverRequest.value = true
                        MissionRunnerService.stop(this@OverlayService)
                    },
                    onApprove = { MissionBus.approvalGranted.value = true },
                    onCollapsedChange = { collapsed ->
                        scope.launch { Prefs.setOverlayCollapsed(this@OverlayService, collapsed) }
                    },
                    onClose = { stopSelf() },
                )
            }
        }
        rootView = composeView
        wm.addView(composeView, lp)
    }

    private fun moveBy(dx: Float, dy: Float) {
        lp.x = (lp.x + dx).toInt()
        lp.y = (lp.y + dy).toInt()
        runCatching { wm.updateViewLayout(rootView, lp) }
    }

    private fun persistPosition() {
        persistJob?.cancel()
        persistJob = scope.launch {
            Prefs.setOverlayPos(this@OverlayService, lp.x, lp.y)
        }
    }

    private fun buildNotification(): Notification {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26 && mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "SWRLZ Overlay", NotificationManager.IMPORTANCE_MIN)
            )
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("SWRLZ-CORE · telemetry overlay active")
            .setContentText("Drag the ≡ handle to move · tap — to minimise · ✕ to dismiss")
            .setOngoing(true)
            .build()
    }
}
