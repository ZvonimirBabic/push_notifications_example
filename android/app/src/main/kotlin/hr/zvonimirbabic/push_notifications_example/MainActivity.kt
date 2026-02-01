package hr.zvonimirbabic.push_notifications_example

import android.content.Intent
import android.os.Bundle
import androidx.core.app.RemoteInput
import hr.zvonimirbabic.notifications.notification_helper.NotificationChannels
import hr.zvonimirbabic.push_notifications_example.notification_helper.NotificationActions
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val channelName = "notification_open"
    private var pendingOpenPayload: Map<String, Any?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationChannels.createHighImportanceNotificationChannel(this)
        NotificationActions.cancelIfPresent(this, intent)
        captureNotificationOpen(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotificationActions.cancelIfPresent(this, intent)
        captureNotificationOpen(intent)
        flushIfPossible()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            channelName
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "getInitialNotification" -> {
                    val payload = pendingOpenPayload
                    pendingOpenPayload = null
                    result.success(payload)
                }
                else -> result.notImplemented()
            }
        }
        flushIfPossible()
    }

    private fun captureNotificationOpen(intent: Intent?) {
        if (intent == null) return

        val notificationID = intent.getIntExtra(NotificationActions.EXTRA_NOTIFICATION_ID, -1)
        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(NotificationActions.REMOTE_INPUT_KEY)
            ?.toString()
        if (notificationID != -1) return
        pendingOpenPayload = mapOf(
            "action" to intent.action,
            "reply" to replyText,
        )
    }

    private fun flushIfPossible() {
        val engine = flutterEngine ?: return
        val payload = pendingOpenPayload ?: return
        MethodChannel(engine.dartExecutor.binaryMessenger, channelName)
            .invokeMethod("onOpen", payload)
        pendingOpenPayload = null
    }
}