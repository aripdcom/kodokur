package com.aripd.kodokur.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.aripd.kodokur.R
import com.aripd.kodokur.core.ContentParser
import com.aripd.kodokur.core.Record
import com.aripd.kodokur.platform.Actions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(record: Record, onBack: () -> Unit) {
    val context = LocalContext.current
    val content = remember(record) { ContentParser.parse(record.scan) }
    val actions = remember(content) { content.actions(record.scan.text) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(content.kindLabel())) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        content.kindIcon(), null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    SelectionContainer {
                        Text(
                            content.headline(),
                            style = if (content.isCode) {
                                MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace)
                            } else {
                                MaterialTheme.typography.titleLarge
                            },
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actions.forEachIndexed { i, action ->
                    val label = stringResource(action.label)
                    val inner: @Composable () -> Unit = {
                        Icon(action.icon, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                    if (i == 0) {
                        Button(onClick = { action.run(context) }) { inner() }
                    } else {
                        FilledTonalButton(onClick = { action.run(context) }) { inner() }
                    }
                }
            }
            if (actions.any { it.external }) {
                Text(
                    stringResource(R.string.external_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val details = remember(content) { content.details() }
            val rows = details + listOfNotNull(
                Detail(R.string.label_format, record.scan.symbology.label, copyable = false),
                record.scan.addOn?.let { Detail(R.string.label_add_on, it, copyable = false) },
                Detail(R.string.label_scanned_at, formatTime(record.timeMillis), copyable = false),
            )
            Card(Modifier.fillMaxWidth()) {
                rows.forEachIndexed { i, detail ->
                    if (i > 0) HorizontalDivider()
                    DetailRow(stringResource(detail.label), detail)
                }
            }

            // Ham içerik, ayrıştırılmış görünümden farklıysa (kişi kartı, e-posta…).
            // Gizli alan (Wi-Fi parolası) varsa gösterilmez: maske anlamsız kalırdı.
            if (record.scan.text != content.headline() && details.none { it.sensitive }) {
                Text(stringResource(R.string.label_raw), style = MaterialTheme.typography.labelLarge)
                SelectionContainer {
                    Text(
                        record.scan.text,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, detail: Detail) {
    val context = LocalContext.current
    var revealed by remember { mutableStateOf(!detail.sensitive) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SelectionContainer {
                Text(
                    if (revealed) detail.value else "•".repeat(minOf(detail.value.length, 12)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (!revealed) {
            TextButton(onClick = { revealed = true }) { Text(stringResource(R.string.act_show)) }
        }
        if (detail.copyable) {
            IconButton(onClick = { Actions.copy(context, detail.value, detail.sensitive) }) {
                Icon(KodokurIcons.Copy, stringResource(R.string.act_copy), Modifier.size(20.dp))
            }
        }
    }
}
