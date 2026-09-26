package com.aripd.kodokur

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aripd.kodokur.platform.AppLocale
import com.aripd.kodokur.ui.KodokurApp
import com.aripd.kodokur.ui.KodokurTheme

class MainActivity : ComponentActivity() {

    /** Applies the chosen language on Android 8–12; on 13+ the system has already applied it. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KodokurTheme {
                KodokurApp(viewModel())
            }
        }
    }
}
