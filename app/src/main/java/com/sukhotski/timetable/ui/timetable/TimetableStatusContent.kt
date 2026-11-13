package com.sukhotski.timetable.ui.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sukhotski.timetable.R
import com.sukhotski.timetable.domain.LoadError

@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.loading_description)
    Box(modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

@Composable
fun ErrorContent(error: LoadError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val (title, body) = when (error) {
        LoadError.NoConnection ->
            R.string.error_no_connection_title to
                R.string.error_no_connection_body

        LoadError.Unexpected -> R.string.error_unexpected_title to R.string.error_unexpected_body
    }
    Column(
        modifier = modifier
            .padding(24.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.action_retry))
        }
    }
}
