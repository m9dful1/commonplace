package com.commonplace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.commonplace.ui.nav.CommonplaceNavHost
import com.commonplace.ui.theme.CommonplaceTheme
import com.commonplace.ui.theme.Paper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CommonplaceApp
        setContent {
            CommonplaceTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Paper),
                    color = Paper,
                ) {
                    var welcomeChecked by remember { mutableStateOf(false) }
                    var welcomeSeen by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        welcomeSeen = app.settings.isWelcomeSeen()
                        welcomeChecked = true
                    }
                    if (welcomeChecked) {
                        CommonplaceNavHost(app = app, startWithWelcome = !welcomeSeen)
                    }
                }
            }
        }
    }
}
