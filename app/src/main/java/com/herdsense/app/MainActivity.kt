package com.herdsense.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.herdsense.app.data.Herd
import com.herdsense.app.ui.HerdSenseApp
import com.herdsense.app.ui.HerdSenseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
        )
        super.onCreate(savedInstanceState)
        Herd.init(applicationContext)
        setContent {
            HerdSenseTheme { HerdSenseApp() }
        }
    }
}
