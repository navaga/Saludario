package com.ignaciovalero.saludario.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.ignaciovalero.saludario.R
import com.ignaciovalero.saludario.core.localization.parseDecimalOrNull
import com.ignaciovalero.saludario.ui.theme.AppSpacing

/**
 * Diálogo reutilizable para añadir stock a un medicamento. Centraliza el
 * parseo decimal (acepta coma o punto) y la validación de valor positivo,
 * evitando duplicar la misma lógica en cada pantalla que lo invoca.
 *
 * @param medicationName Nombre del medicamento mostrado en el título.
 * @param onDismiss      Callback invocado al cancelar o pulsar fuera del diálogo.
 * @param onConfirm      Callback invocado con la cantidad parseada cuando el
 *                       usuario confirma una entrada válida.
 */
@Composable
fun RestockDialog(
    medicationName: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val amountValue = amountText.parseDecimalOrNull()
    val canConfirm = amountValue != null && amountValue > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.medications_restock_title, medicationName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                Text(
                    text = stringResource(R.string.medications_restock_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.medications_restock_amount_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { amountValue?.let(onConfirm) },
                enabled = canConfirm
            ) {
                Text(stringResource(R.string.medications_restock_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.time_picker_cancel))
            }
        }
    )
}
