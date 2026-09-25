package com.aripd.kodokur.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.aripd.kodokur.KodokurViewModel
import com.aripd.kodokur.Screen

@Composable
fun KodokurApp(vm: KodokurViewModel) {
    BackHandler(enabled = vm.screen != Screen.Scanner) { vm.back() }
    when (val screen = vm.screen) {
        Screen.Scanner -> ScannerScreen(
            vm = vm,
            onHistory = { vm.navigate(Screen.History) },
            onAbout = { vm.navigate(Screen.About) },
        )
        Screen.History -> HistoryScreen(
            store = vm.history,
            onOpen = { vm.navigate(Screen.Result(it, from = Screen.History)) },
            onBack = { vm.back() },
        )
        Screen.About -> AboutScreen(
            onLanguage = { vm.navigate(Screen.Language) },
            onBack = { vm.back() },
        )
        Screen.Language -> LanguageScreen(onBack = { vm.back() })
        is Screen.Result -> ResultScreen(screen.record, onBack = { vm.back() })
    }
}
