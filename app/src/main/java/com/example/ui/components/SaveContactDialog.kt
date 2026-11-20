package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.telephony.NewContactPayload
import com.example.ui.i18n.NovaStrings
import com.example.ui.theme.LocalNovaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveContactBottomSheet(
    initialNumber: String,
    initialName: String,
    language: AppLanguage,
    strings: NovaStrings,
    onDismiss: () -> Unit,
    onSave: (NewContactPayload) -> Unit,
    onOpenSystemContactsFallback: (number: String, name: String) -> Unit
) {
    val nova = LocalNovaColors.current
    val isBn = language == AppLanguage.BENGALI
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var fullName by remember { mutableStateOf(initialName) }
    var phoneNumber by remember { mutableStateOf(initialNumber) }
    var alternatePhone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedPhotoUri = uri
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isBn) "নতুন কন্টাক্ট সেভ করুন" else "Save New Contact",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isBn) "ছবি ও যাবতীয় তথ্যসহ সরাসরি ফোনে সেভ হবে" else "Saves with photo and full details to your phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = strings.cancel)
                }
            }

            // Photo Picker Avatar Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(nova.goldContainer.copy(alpha = 0.7f))
                        .border(2.dp, nova.gold, CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("save_contact_photo_picker"),
                    contentAlignment = Alignment.Center
                ) {
                    if (pickedPhotoUri != null) {
                        AsyncImage(
                            model = pickedPhotoUri,
                            contentDescription = if (isBn) "কন্টাক্ট ছবি" else "Contact Photo",
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = if (isBn) "ছবি যোগ করুন" else "Add Photo",
                                tint = nova.gold,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isBn) "ছবি দিন" else "Add Photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = nova.gold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Text(
                            text = if (pickedPhotoUri == null) {
                                if (isBn) "গ্যালারি থেকে ছবি বাছুন" else "Pick Photo from Gallery"
                            } else {
                                if (isBn) "ছবি পরিবর্তন করুন" else "Change Photo"
                            }
                        )
                    }
                    if (pickedPhotoUri != null) {
                        TextButton(onClick = { pickedPhotoUri = null }) {
                            Text(
                                text = if (isBn) "ছবি সরান" else "Remove",
                                color = nova.missed
                            )
                        }
                    }
                }
            }

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = nova.gold,
                unfocusedBorderColor = nova.glassBorder
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text(if (isBn) "পুরো নাম *" else "Full Name *") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = nova.gold) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_contact_name_input"),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text(if (isBn) "ফোন নম্বর *" else "Phone Number *") },
                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = nova.sim1) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_contact_phone_input"),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors
            )

            OutlinedTextField(
                value = alternatePhone,
                onValueChange = { alternatePhone = it },
                label = { Text(if (isBn) "বিকল্প নম্বর (ঐচ্ছিক)" else "Alternate Phone (Optional)") },
                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = nova.sim2) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_contact_alt_phone_input"),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(if (isBn) "ইমেইল (ঐচ্ছিক)" else "Email Address (Optional)") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = nova.gold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_contact_email_input"),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text(if (isBn) "কোম্পানি / অফিস" else "Company") },
                    leadingIcon = { Icon(Icons.Filled.Business, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text(if (isBn) "পদবি" else "Job Title") },
                    leadingIcon = { Icon(Icons.Filled.Work, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors
                )
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(if (isBn) "ঠিকানা (ঐচ্ছিক)" else "Address (Optional)") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = nova.sim1) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(if (isBn) "নোট / অতিরিক্ত তথ্য" else "Notes / Extra Info") },
                leadingIcon = { Icon(Icons.Filled.Notes, contentDescription = null, tint = nova.gold) },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(4.dp))

            val canSave = fullName.trim().isNotEmpty() && phoneNumber.trim().isNotEmpty()
            Button(
                onClick = {
                    if (canSave) {
                        onSave(
                            NewContactPayload(
                                fullName = fullName.trim(),
                                phoneNumber = phoneNumber.trim(),
                                alternatePhone = alternatePhone.trim(),
                                email = email.trim(),
                                company = company.trim(),
                                jobTitle = jobTitle.trim(),
                                address = address.trim(),
                                notes = notes.trim(),
                                photoUri = pickedPhotoUri
                            )
                        )
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_save_contact_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = nova.sim1,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isBn) "ছবি ও তথ্যসহ কন্টাক্ট সেভ করুন" else "Save Contact with Photo & Info",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenSystemContactsFallback(phoneNumber.trim(), fullName.trim())
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, nova.glassBorder)
            ) {
                Text(
                    text = if (isBn) "ফোনের কন্টাক্ট অ্যাপে খুলুন" else "Open in Phone Contacts App",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
