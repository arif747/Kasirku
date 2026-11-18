package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.signOut
import com.example.data.model.StoreProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun StoreSettingsScreen(
    storeProfile: StoreProfile,
    isLocalMode: Boolean,
    isSaving: Boolean,
    onSaveProfile: (StoreProfile) -> Unit,
    onSwitchToGoogleAuth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val user = if (isLocalMode) null else Firebase.auth.currentUser

    var storeName by remember(storeProfile) { mutableStateOf(storeProfile.storeName) }
    var address by remember(storeProfile) { mutableStateOf(storeProfile.address) }
    var phone by remember(storeProfile) { mutableStateOf(storeProfile.phone) }
    var taxText by remember(storeProfile) {
        mutableStateOf(if (storeProfile.taxPercent > 0.0) storeProfile.taxPercent.toString() else "0")
    }
    var receiptFooter by remember(storeProfile) { mutableStateOf(storeProfile.receiptFooter) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.store_title),
                style = MaterialTheme.typography.headlineSmall,
            )

            // Account / Mode Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        color = if (isLocalMode) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isLocalMode) Icons.Filled.PhoneAndroid else Icons.Filled.Person,
                                contentDescription = null,
                                tint = if (isLocalMode) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLocalMode) {
                                stringResource(R.string.store_local_mode_title)
                            } else {
                                user?.displayName?.ifBlank { "Pemilik Toko" } ?: "Pemilik Toko"
                            },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = if (isLocalMode) {
                                stringResource(R.string.store_local_mode_desc)
                            } else {
                                user?.email ?: "Akun Google Terhubung"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Store Profile Form
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Identitas Toko & Struk",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it.take(60) },
                        label = { Text(stringResource(R.string.store_name_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("store_name_input"),
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it.take(150) },
                        label = { Text(stringResource(R.string.store_address_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("store_address_input"),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.take(30) },
                            label = { Text(stringResource(R.string.store_phone_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("store_phone_input"),
                        )
                        OutlinedTextField(
                            value = taxText,
                            onValueChange = { raw ->
                                taxText = raw.filter { it.isDigit() || it == '.' }.take(5)
                            },
                            label = { Text(stringResource(R.string.store_tax_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("store_tax_input"),
                        )
                    }

                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it.take(120) },
                        label = { Text(stringResource(R.string.store_footer_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("store_footer_input"),
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        enabled = !isSaving && storeName.trim().isNotEmpty(),
                        onClick = {
                            val parsedTax = (taxText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 30.0)
                            onSaveProfile(
                                storeProfile.copy(
                                    storeName = storeName.trim(),
                                    address = address.trim(),
                                    phone = phone.trim(),
                                    taxPercent = parsedTax,
                                    receiptFooter = receiptFooter.trim(),
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_store_button"),
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.store_save))
                    }
                }
            }

            if (isLocalMode) {
                OutlinedButton(
                    onClick = onSwitchToGoogleAuth,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("switch_to_google_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.store_switch_to_google))
                }
            } else {
                OutlinedButton(
                    onClick = { signOut(credentialManager, scope) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_out_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.store_sign_out))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
