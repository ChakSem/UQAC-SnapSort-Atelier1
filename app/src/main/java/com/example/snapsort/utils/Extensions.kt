// utils/Extensions.kt
package com.example.snapsort.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

// Date extensions
fun Date.formatToReadableString(): String {
    val formatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    return formatter.format(this)
}

fun Date.formatToShortString(): String {
    val formatter = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    return formatter.format(this)
}

fun Long.toFormattedDate(): String {
    val date = Date(this)
    return date.formatToReadableString()
}

// Flow extensions for Compose
@Composable
fun <T> Flow<T>.collectAsLifecycleAware(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend (T) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    LaunchedEffect(this, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            this@collectAsLifecycleAware.collect(collector)
        }
    }
}

fun String.capitalizeWords(): String {
    return this.lowercase().split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
    }
}
