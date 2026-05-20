package com.commonplace.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Tiny factory that lets each screen construct its ViewModel inline with
 * its own dependencies, without pulling in a DI framework.
 */
@Composable
inline fun <reified VM : ViewModel> rememberViewModel(crossinline create: () -> VM): VM {
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
        }
    }
    return viewModel(factory = factory)
}

/**
 * Variant for VMs that depend on a runtime argument (e.g. fragment id) — we
 * include the key in the factory so navigating to a different id makes a
 * fresh VM instance.
 */
@Composable
inline fun <reified VM : ViewModel> rememberViewModelWithKey(
    key: String,
    crossinline create: () -> VM,
): VM {
    val factory = remember(key) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
        }
    }
    return viewModel(key = key, factory = factory)
}
