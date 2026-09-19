package com.extremestudios.portalarekmusik.ui.screens

import android.app.Activity
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.extremestudios.portalarekmusik.data.model.SongRegistrationPayload
import com.extremestudios.portalarekmusik.data.model.PortalUserSession
import com.extremestudios.portalarekmusik.data.repository.SongRegistrationRepository
import com.extremestudios.portalarekmusik.ui.NON_ADMIN_SONG_REWARD_COOLDOWN_MS
import com.extremestudios.portalarekmusik.ui.PORTAL_REWARD_PREFERENCE_NAME
import com.extremestudios.portalarekmusik.ui.REWARD_AD_UNIT_ID
import com.extremestudios.portalarekmusik.ui.SONG_REGISTRATION_REWARD_COOLDOWN_KEY
import com.extremestudios.portalarekmusik.ui.SelectedImagePayload
import com.extremestudios.portalarekmusik.ui.readSelectedImage
import com.extremestudios.portalarekmusik.ui.rememberRewardedSubmissionGate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitSongScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    adminSession: PortalUserSession? = null,
    repository: SongRegistrationRepository = remember { SongRegistrationRepository() }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    var artistName by remember { mutableStateOf("") }
    var songTitle by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var releaseYear by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedCoverImage by remember { mutableStateOf<SelectedImagePayload?>(null) }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAudioName by remember { mutableStateOf("") }
    val isActiveAdmin = adminSession?.status.equals("ACTIVE", ignoreCase = true)
    val songSubmissionDeviceId = remember(context) {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty().trim()
    }

    val fieldErrors = remember { mutableStateMapOf<String, String>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val songRegistrationRewardGate = rememberRewardedSubmissionGate(
        context = context,
        adUnitId = REWARD_AD_UNIT_ID,
        preferenceName = PORTAL_REWARD_PREFERENCE_NAME,
        cooldownKey = SONG_REGISTRATION_REWARD_COOLDOWN_KEY,
        cooldownMs = NON_ADMIN_SONG_REWARD_COOLDOWN_MS,
        requiresReward = true
    )
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            readSelectedImage(context, uri)
                .onSuccess {
                    selectedCoverImage = it
                    fieldErrors.remove("coverImage")
                }
                .onFailure { error ->
                    snackbarHostState.showSnackbar(
                        error.message ?: "Gagal memuat cover artwork."
                    )
                }
        }
    }
    val audioPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val displayName = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else "audio.mp3"
        }.orEmpty().ifBlank { "audio.mp3" }
        selectedAudioUri = uri
        selectedAudioName = displayName
        fieldErrors.remove("mp3Link")
    }

    fun validate(): Boolean {
        fieldErrors.clear()
        if (artistName.isBlank()) fieldErrors["artistName"] = "Nama Band / Artist wajib diisi"
        if (songTitle.isBlank()) fieldErrors["songTitle"] = "Judul Lagu wajib diisi"
        if (releaseYear.isBlank()) {
            fieldErrors["releaseYear"] = "Tanggal rilis wajib diisi"
        } else if (!releaseYear.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            fieldErrors["releaseYear"] = "Gunakan format YYYY-MM-DD"
        }
        if (selectedAudioUri == null) {
            fieldErrors["mp3Link"] = "File MP3 wajib dipilih"
        }
        return fieldErrors.isEmpty()
    }

    fun clearForm() {
        artistName = ""
        songTitle = ""
        genre = ""
        city = ""
        releaseYear = ""
        selectedCoverImage = null
        selectedAudioUri = null
        selectedAudioName = ""
        fieldErrors.clear()
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SnackbarHost(hostState = snackbarHostState)
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBack != null) {
                item {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Registrasi Lagu",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    painter = painterResource(android.R.drawable.ic_menu_revert),
                                    contentDescription = "Kembali",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            }
            if (onBack == null) {
                item {
                    Text(
                        text = "Registrasi Lagu",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            item { FormField(artistName, { artistName = it }, "1. Nama Band / Artist", fieldErrors["artistName"]) }
            item { FormField(songTitle, { songTitle = it }, "2. Judul Lagu", fieldErrors["songTitle"]) }
            item { FormField(genre, { genre = it }, "3. Genre", null) }
            item { FormField(city, { city = it }, "4. Asal Kota", null) }
            item { FormField(releaseYear, { releaseYear = it }, "5. Tanggal rilis (YYYY-MM-DD)", fieldErrors["releaseYear"]) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("6. Upload MP3 dari HP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Maksimal 10 MB. File disimpan ke Cloudflare dan menunggu approve.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = { audioPickerLauncher.launch("audio/*") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), contentColor = MaterialTheme.colorScheme.onSurface)
                        ) { Text(if (selectedAudioUri == null) "Pilih MP3 dari HP" else "Ganti MP3") }
                        if (selectedAudioUri != null) {
                            Text(selectedAudioName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "7. Pilih Cover Artwork",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ambil cover JPG atau PNG dari galeri HP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(if (selectedCoverImage == null) "Pilih Cover Artwork" else "Ganti Cover Artwork")
                    }
                    selectedCoverImage?.let { cover ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                bitmap = cover.previewBitmap.asImageBitmap(),
                                contentDescription = "Preview cover artwork",
                                modifier = Modifier
                                    .size(84.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                    .padding(2.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = cover.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = cover.mimeType,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        if (!validate()) {
                            scope.launch { snackbarHostState.showSnackbar("Form belum lengkap. Cek kembali data wajib.") }
                            return@Button
                        }

                        songRegistrationRewardGate.show(
                            activity,
                            {
                                isLoading = true
                                scope.launch {
                                    val audioUrlResult = selectedAudioUri?.let { audioUri ->
                                        if (isActiveAdmin) {
                                            adminSession?.let { session ->
                                                repository.uploadAdminAudio(
                                                    session = session,
                                                    contentResolver = context.contentResolver,
                                                    uri = audioUri,
                                                    artistName = artistName.trim(),
                                                    songTitle = songTitle.trim(),
                                                    releaseDate = releaseYear.trim()
                                                )
                                            } ?: Result.failure(IllegalStateException("Login admin aktif diperlukan untuk upload MP3."))
                                        } else {
                                            repository.uploadPublicAudio(
                                                contentResolver = context.contentResolver,
                                                uri = audioUri,
                                                artistName = artistName.trim(),
                                                songTitle = songTitle.trim(),
                                                releaseDate = releaseYear.trim(),
                                                deviceId = songSubmissionDeviceId
                                            )
                                        }
                                    } ?: Result.failure(IllegalStateException("File MP3 wajib dipilih."))
                                    val resolvedAudioUrl = audioUrlResult.getOrElse { error ->
                                        isLoading = false
                                        snackbarHostState.showSnackbar("Gagal upload MP3: ${error.message ?: "Terjadi kesalahan."}")
                                        return@launch
                                    }
                                    val payload = SongRegistrationPayload(
                                        bandName = artistName.trim(),
                                        songTitle = songTitle.trim(),
                                        genre = genre.trim(),
                                        city = city.trim(),
                                        releaseYear = releaseYear.trim(),
                                        mp3Link = resolvedAudioUrl,
                                        coverImageBase64 = selectedCoverImage?.base64,
                                        coverImageFileName = selectedCoverImage?.fileName,
                                        coverImageMimeType = selectedCoverImage?.mimeType
                                    )

                                    val result = repository.submitRegistration(
                                        payload = payload,
                                        adminSession = adminSession?.takeIf { isActiveAdmin },
                                        deviceId = songSubmissionDeviceId.takeIf { !isActiveAdmin }
                                    )
                                    isLoading = false

                                    result.onSuccess {
                                        songRegistrationRewardGate.markSubmissionCompleted()
                                        snackbarHostState.showSnackbar(
                                            if (isActiveAdmin) "MP3 berhasil diupload dan lagu sudah ditambahkan."
                                            else "Registrasi lagu berhasil dikirim."
                                        )
                                        clearForm()
                                    }.onFailure { error ->
                                        snackbarHostState.showSnackbar(
                                            "Gagal mengirim registrasi: ${error.message ?: "Terjadi kesalahan, coba lagi."}"
                                        )
                                    }
                                }
                            },
                            { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            },
                            { isLoading = it }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Kirim Registrasi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    minLines: Int = 1
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        shape = RoundedCornerShape(14.dp),
        isError = error != null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            focusedLabelColor = MaterialTheme.colorScheme.primary
        ),
        minLines = minLines,
        singleLine = minLines == 1,
        keyboardOptions = KeyboardOptions(
            imeAction = if (minLines == 1) ImeAction.Done else ImeAction.Default
        ),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus(force = true) }
        ),
        supportingText = {
            if (error != null) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
