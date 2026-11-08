package com.memoise.ledger.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.compose.ui.graphics.Color
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.memoise.ledger.MainActivity

class QuickCaptureWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val captureIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_QUICK_CAPTURE
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF777B50)))
                    .cornerRadius(18.dp)
                    .semantics { contentDescription = "Create a new entry" }
                    .clickable(actionStartActivity(captureIntent)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFF6F3EC)),
                        fontSize = 34.sp,
                    ),
                )
            }
        }
    }
}

class QuickCaptureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickCaptureWidget()
}
