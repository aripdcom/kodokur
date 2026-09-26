package com.aripd.kodokur

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.aripd.kodokur.core.Record
import com.aripd.kodokur.core.Scan
import com.aripd.kodokur.platform.HistoryStore

/** Screens. The result screen knows where Back returns to. */
sealed interface Screen {
    data object Scanner : Screen
    data object History : Screen
    data object About : Screen
    data object Language : Screen
    data class Result(val record: Record, val from: Screen) : Screen
}

class KodokurViewModel(app: Application) : AndroidViewModel(app) {

    val history = HistoryStore(app)

    var screen: Screen by mutableStateOf(Screen.Scanner)
        private set

    /** Batch scanning: each code read goes into history without opening the result screen. */
    var batchMode by mutableStateOf(false)
        private set

    /** Codes read in this batch; the same book is not added twice. */
    private val batchSeen = LinkedHashSet<String>()

    var batchCount by mutableStateOf(0)
        private set

    fun navigate(to: Screen) {
        screen = to
    }

    fun back(): Boolean {
        screen = when (val s = screen) {
            Screen.Scanner -> return false
            is Screen.Result -> s.from
            Screen.Language -> Screen.About
            else -> Screen.Scanner
        }
        return true
    }

    /** Single read: writes to history and opens the result. */
    fun open(scan: Scan) {
        val record = history.add(scan)
        screen = Screen.Result(record, from = Screen.Scanner)
    }

    fun toggleBatch() {
        batchMode = !batchMode
        batchSeen.clear()
        batchCount = 0
    }

    /** Adds to the batch; false if already in this batch. */
    fun addToBatch(scan: Scan): Boolean {
        if (!batchSeen.add(scan.text)) return false
        history.add(scan)
        batchCount = batchSeen.size
        return true
    }
}
