package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.cloud.CallerCategory
import com.example.data.model.AppLanguage
import com.example.ui.theme.LocalNovaColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagCallerDialog(
    initialNumber: String,
    initialName: String,
    initialCategory: CallerCategory,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (number: String, name: String, category: CallerCategory) -> Unit
) {
    val nova = LocalNovaColors.current
    val isBn = language == AppLanguage.BENGALI
    var number by remember(initialNumber) { mutableStateOf(initialNumber) }
    var name by remember(initialName) { mutableStateOf(initialName) }
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isBn) "কমিউনিটি কলার আইডি যোগ করুন" else "Tag Caller ID",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isBn) {
                        "এই নম্বরের নাম বা স্প্যাম ট্যাগ সেভ করলে অন্য Shajamal ইউজারদের ফোনেও কল এলে নাম শো করবে।"
                    } else {
                        "Save a caller name or spam tag to the Shajamal Community Caller ID directory."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(if (isBn) "ফোন নম্বর" else "Phone Number") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_caller_number_input")
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it },
                    label = { Text(if (isBn) "কলারের নাম" else "Caller Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_caller_name_input")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CallerCategory.entries.forEach { cat ->
                        val isSpamCat = cat == CallerCategory.SPAM || cat == CallerCategory.SCAM
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = {
                                Text(
                                    text = if (isBn) cat.labelBn else cat.labelEn,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isSpamCat) nova.missedContainer else nova.sim1Container,
                                selectedLabelColor = if (isSpamCat) nova.missed else nova.sim1
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (number.isNotBlank() && name.isNotBlank()) {
                        onSubmit(number.trim(), name.trim(), category)
                        onDismiss()
                    }
                },
                enabled = number.filter { it.isDigit() }.length >= 4 && name.isNotBlank(),
                modifier = Modifier.testTag("tag_caller_save_button")
            ) {
                Text(if (isBn) "ক্লাউডে সেভ করুন" else "Publish Tag")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBn) "বাতিল" else "Cancel")
            }
        }
    )
}
