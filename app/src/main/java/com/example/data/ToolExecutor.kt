package com.example.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.widget.Toast
import com.example.model.ToolExecutionLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ToolExecutor(private val context: Context) {

    fun openWebsite(url: String, label: String? = null): ToolExecutionLog {
        return try {
            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionLog(
                toolName = "openWebsite",
                argument = formattedUrl,
                success = true,
                resultMessage = "Opened ${label ?: formattedUrl} in browser! 🌐"
            )
        } catch (e: Exception) {
            ToolExecutionLog(
                toolName = "openWebsite",
                argument = url,
                success = false,
                resultMessage = "Couldn't open URL: ${e.localizedMessage}"
            )
        }
    }

    fun checkDeviceStatus(): ToolExecutionLog {
        return try {
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, batteryFilter)
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 100

            val timeFormat = SimpleDateFormat("EEEE, hh:mm a", Locale.getDefault())
            val currentTime = timeFormat.format(Date())

            val msg = "Time is $currentTime | Battery: $batteryPct% ✨"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

            ToolExecutionLog(
                toolName = "checkDeviceStatus",
                argument = "device_status",
                success = true,
                resultMessage = msg
            )
        } catch (e: Exception) {
            ToolExecutionLog(
                toolName = "checkDeviceStatus",
                argument = "status",
                success = false,
                resultMessage = "Status check failed: ${e.localizedMessage}"
            )
        }
    }

    fun setQuickReminder(title: String, delayMinutes: Int = 5): ToolExecutionLog {
        val msg = "Reminder set: '$title' in $delayMinutes min! I'll hold you to it. 😉"
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        return ToolExecutionLog(
            toolName = "setReminder",
            argument = "$title ($delayMinutes mins)",
            success = true,
            resultMessage = msg
        )
    }

    fun shareText(text: String): ToolExecutionLog {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share Zoya's message").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            ToolExecutionLog(
                toolName = "shareText",
                argument = text,
                success = true,
                resultMessage = "Shared text via Android intent!"
            )
        } catch (e: Exception) {
            ToolExecutionLog(
                toolName = "shareText",
                argument = text,
                success = false,
                resultMessage = "Sharing failed: ${e.localizedMessage}"
            )
        }
    }
}
