package com.ignaciovalero.saludario.ui.medications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.ui.theme.AppSpacing
import com.ignaciovalero.saludario.ui.theme.SaludarioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationListScreen(
    uiState: MedicationListUiState,
    onDelete: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onAddStock: (Long, Double) -> Unit,
    onMessageShown: () -> Unit,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var restockTarget by remember { mutableStateOf<MedicationItem?>(null) }
    var restockAmount by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<MedicationItem?>(null) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            onMessageShown()
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        TopAppBar(title = { Text(stringResource(R.string.medications_title)) })

        if (uiState.medications.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppSpacing.lg),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.medications_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = AppSpacing.lg,
                    top = AppSpacing.md,
                    end = AppSpacing.lg,
                    bottom = AppSpacing.xxl + 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                items(uiState.medications, key = { it.id }) { med ->
                    MedicationCard(
                        item = med,
                        onEdit = { onEdit(med.id) },
                        onDelete = { deleteTarget = med },
                        onAddStock = {
                            restockTarget = med
                            restockAmount = ""
                        }
                    )
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.medications_delete_confirm_title)) },
            text = {
                Text(
                    text = stringResource(R.string.medications_delete_confirm_message, target.name),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(target.id)
                        deleteTarget = null
                    }
                ) {
                    Text(stringResource(R.string.medications_delete_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.time_picker_cancel))
                }
            }
        )
    }

    val amountValue = restockAmount.replace(',', '.').toDoubleOrNull()
    val canConfirmRestock = amountValue != null && amountValue > 0.0

    restockTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { restockTarget = null },
            title = { Text(stringResource(R.string.medications_restock_title, target.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Text(
                        text = stringResource(R.string.medications_restock_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = restockAmount,
                        onValueChange = { restockAmount = it },
                        label = { Text(stringResource(R.string.medications_restock_amount_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddStock(target.id, amountValue!!)
                        restockTarget = null
                    },
                    enabled = canConfirmRestock
                ) {
                    Text(stringResource(R.string.medications_restock_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { restockTarget = null }) {
                    Text(stringResource(R.string.time_picker_cancel))
                }
            }
        )
    }
}

@Composable
private fun MedicationCard(
    item: MedicationItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddStock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stockProgressDescription = stringResource(
        R.string.medications_stock_progress_cd,
        item.stockLabel
    )
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppSpacing.xs)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm + 2.dp),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.dosage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.schedule,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.hasQuantityTracking) {
                        Text(
                            text = item.stockLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.isLowStock) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.hasQuantityTracking) {
                        IconButton(onClick = onAddStock) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = stringResource(R.string.medications_restock_item_cd, item.name),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.medications_more_actions_cd, item.name)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.medications_edit_menu_action)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.medications_delete_menu_action)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            if (item.hasQuantityTracking) {
                StockProgressBar(
                    progress = item.stockProgress,
                    isLowStock = item.isLowStock,
                    contentDescription = stockProgressDescription
                )
            }
        }
    }
}

@Composable
private fun StockProgressBar(
    progress: Float,
    isLowStock: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val progressColor = if (isLowStock) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .background(progressColor)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MedicationListScreenPreview() {
    SaludarioTheme {
        MedicationListScreen(
            uiState = MedicationListUiState(
                medications = listOf(
                    MedicationItem(1, "Paracetamol", "1 tableta", "Diario · 08:00, 20:00", true, "Te quedan 18 de 24", 0.75f, false),
                    MedicationItem(2, "Ibuprofeno", "1 tableta", "Diario · 14:00", true, "Te quedan 2 de 20", 0.10f, true)
                )
            ),
            onEdit = {},
            onAddStock = { _, _ -> },
            onDelete = {},
            onMessageShown = {},
            snackbarHostState = androidx.compose.material3.SnackbarHostState(),
            contentPadding = PaddingValues(0.dp)
        )
    }
}
