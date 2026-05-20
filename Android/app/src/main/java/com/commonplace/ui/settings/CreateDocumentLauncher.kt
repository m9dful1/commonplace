package com.commonplace.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Storage Access Framework launcher for ACTION_CREATE_DOCUMENT with a JSON
 * mime type. Wraps the verbose Activity Result API in a small Composable
 * helper so the Settings screen stays readable.
 */
@Composable
fun rememberCreateDocumentLauncher(
    onResult: (Uri?) -> Unit,
): ActivityResultLauncher<String> =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> onResult(uri) }
