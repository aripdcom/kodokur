package com.aripd.kodokur.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aripd.kodokur.R
import com.aripd.kodokur.core.Isbn
import com.aripd.kodokur.platform.Actions
import com.aripd.kodokur.platform.AppLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleScaffold(title: String, onBack: () -> Unit, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding -> content(Modifier.padding(padding)) }
}

@Composable
fun AboutScreen(onLanguage: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    val current = LocalConfiguration.current.locales[0]
    SimpleScaffold(stringResource(R.string.about_title), onBack) { modifier ->
        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(KodokurIcons.Barcode, null, tint = Amber, modifier = Modifier.size(48.dp))
            Column {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.app_tagline), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.about_version, version),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(stringResource(R.string.about_promise))
            Card {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.language_title)) },
                    supportingContent = { Text(AppLocale.endonym(AppLocale.normalize(current) ?: "en")) },
                    modifier = Modifier.clickable(onClick = onLanguage),
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.about_privacy)) },
                    trailingContent = { Icon(KodokurIcons.OpenInNew, null) },
                    modifier = Modifier.clickable {
                        // The policy is a single page, with an anchor per language.
                        val lang = AppLocale.normalize(current) ?: "en"
                        Actions.browse(context, "${Actions.PRIVACY_URL}#$lang")
                    },
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.about_source)) },
                    supportingContent = { Text(Actions.SOURCE_URL.removePrefix("https://")) },
                    trailingContent = { Icon(KodokurIcons.OpenInNew, null) },
                    modifier = Modifier.clickable { Actions.browse(context, Actions.SOURCE_URL) },
                )
            }
            Text(stringResource(R.string.about_license), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.about_libraries),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.about_isbn_ranges, Isbn.rangesDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Language picker: the first row is "phone language", followed by the supported
 * languages under their own names. Applying a choice recreates the activity.
 */
@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val selected = remember { AppLocale.selected(context) }
    val effective = AppLocale.normalize(LocalConfiguration.current.locales[0]) ?: "en"
    SimpleScaffold(stringResource(R.string.language_title), onBack) { modifier ->
        LazyColumn(modifier.fillMaxSize()) {
            item {
                LanguageRow(
                    label = stringResource(R.string.language_system),
                    detail = if (selected == AppLocale.SYSTEM) AppLocale.endonym(effective) else null,
                    checked = selected == AppLocale.SYSTEM,
                    onClick = { AppLocale.choose(context, AppLocale.SYSTEM) },
                )
            }
            items(AppLocale.TAGS) { tag ->
                LanguageRow(
                    label = AppLocale.endonym(tag),
                    detail = null,
                    checked = selected == tag,
                    onClick = { AppLocale.choose(context, tag) },
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(label: String, detail: String?, checked: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = detail?.let { { Text(it) } },
        trailingContent = if (checked) {
            { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }
        } else {
            null
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
