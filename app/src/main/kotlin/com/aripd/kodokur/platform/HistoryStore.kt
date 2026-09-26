package com.aripd.kodokur.platform

import android.content.Context
import android.util.Log
import com.aripd.kodokur.core.Record
import com.aripd.kodokur.core.Scan
import com.aripd.kodokur.core.Symbology
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

/**
 * Scan history: a single JSON file in the app's private storage. Changes go to
 * memory on the main thread and to disk in order on a background thread. The file
 * is written under a temporary name and then moved into place, so a crash while
 * writing never leaves the history half-written.
 */
class HistoryStore(context: Context) {

    private val file = File(context.filesDir, "history.json")
    private val io = Executors.newSingleThreadExecutor()

    private val _records = MutableStateFlow(load())

    /** Newest first. */
    val records: StateFlow<List<Record>> = _records.asStateFlow()

    /** Adds a record; if the same code was read twice in a row, it replaces the top entry. */
    fun add(scan: Scan, timeMillis: Long = System.currentTimeMillis()): Record {
        val record = Record(timeMillis, scan)
        update { list ->
            val rest = if (list.firstOrNull()?.scan == scan) list.drop(1) else list
            (listOf(record) + rest).take(MAX)
        }
        return record
    }

    fun remove(record: Record) = update { it - record }

    /** Undoes a delete: puts the record back in its place by timestamp. */
    fun restore(record: Record) = update { list ->
        (list + record).sortedByDescending { it.timeMillis }.take(MAX)
    }

    fun clear() = update { emptyList() }

    private fun update(change: (List<Record>) -> List<Record>) {
        val next = change(_records.value)
        _records.value = next
        io.execute { save(next) }
    }

    private fun load(): List<Record> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            (0 until array.length()).mapNotNull { i ->
                val o = array.getJSONObject(i)
                val symbology = Symbology.fromName(o.optString("f")) ?: return@mapNotNull null
                Record(
                    timeMillis = o.getLong("t"),
                    scan = Scan(
                        text = o.getString("x"),
                        symbology = symbology,
                        addOn = o.optString("a").ifEmpty { null },
                        gs1 = o.optBoolean("g", false),
                    ),
                )
            }
        } catch (e: Exception) {
            // A corrupt file must not stop the app from opening; move it aside.
            Log.w(TAG, "Could not read history, starting a new one", e)
            file.renameTo(File(file.parentFile, "history.broken.json"))
            emptyList()
        }
    }

    private fun save(records: List<Record>) {
        val array = JSONArray()
        for (r in records) {
            array.put(
                JSONObject()
                    .put("t", r.timeMillis)
                    .put("f", r.scan.symbology.name)
                    .put("x", r.scan.text)
                    .apply {
                        r.scan.addOn?.let { put("a", it) }
                        if (r.scan.gs1) put("g", true)
                    },
            )
        }
        val tmp = File(file.parentFile, "history.json.tmp")
        tmp.writeText(array.toString())
        if (!tmp.renameTo(file)) Log.w(TAG, "Could not save history")
    }

    private companion object {
        const val TAG = "Kodokur"
        const val MAX = 5000
    }
}
