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

    /** Android 8–12'de seçili dili uygular; 13+'ta sistem zaten uygulamıştır. */
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
