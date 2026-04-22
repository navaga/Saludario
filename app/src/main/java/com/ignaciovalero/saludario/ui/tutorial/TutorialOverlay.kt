package com.ignaciovalero.saludario.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ignaciovalero.saludario.R

@Composable
fun TutorialOverlay(
    title: String,
    message: String,
    onUnderstood: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onUnderstood,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.tutorial_understood))
                }
            }
        }
    }
}

@Composable
fun TutorialOverlayHost(
    screen: TutorialScreen,
    tutorialViewModel: TutorialViewModel,
    modifier: Modifier = Modifier
) {
    val shouldShow by tutorialViewModel.shouldShow(screen).collectAsState(initial = false)

    if (shouldShow) {
        TutorialOverlay(
            title = stringResource(screen.titleRes),
            message = stringResource(screen.messageRes),
            onUnderstood = { tutorialViewModel.onUnderstood(screen) },
            modifier = modifier
        )
    }
}
