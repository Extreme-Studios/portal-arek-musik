package com.extremestudios.portalarekmusik.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.extremestudios.portalarekmusik.data.model.MvRegistrationPayload
import com.extremestudios.portalarekmusik.data.model.PortalUserSession
import com.extremestudios.portalarekmusik.data.repository.SongRegistrationRepository
import com.extremestudios.portalarekmusik.ui.PORTAL_REWARD_COOLDOWN_MS
import com.extremestudios.portalarekmusik.ui.PORTAL_REWARD_PREFERENCE_NAME
import com.extremestudios.portalarekmusik.ui.REWARD_AD_UNIT_ID
import com.extremestudios.portalarekmusik.ui.rememberRewardedSubmissionGate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitMvScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    adminSession: PortalUserSession? = null,
    repository: SongRegistrationRepository = remember { SongRegistrationRepository() }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var releaseYear by remember { mutableStateOf("") }
    var albumName by remember { mutableStateOf("Single") }
    var genre by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var channelUrl by remember { mutableStateOf("") }
    var submissionMode by remember { mutableStateOf("manual") }
    
    var isLoading by remember { mutableStateOf(false) }
    val fieldErrors = remember { mutableStateMapOf<String, String>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isActiveAdmin = adminSession?.status.equals("ACTIVE", ignoreCase = true)
    
    val mvRegistrationRewardGate = rememberRewardedSubmissionGate(
        context = context,
        adUnitId = REWARD_AD_UNIT_ID,
        preferenceName = PORTAL_REWARD_PREFERENCE_NAME,
        cooldownKey = "portal_mv_registration_cooldown",
        cooldownMs = PORTAL_REWARD_COOLDOWN_MS,
        requiresReward = true
    )

    fun validate(): Boolean {
        fieldErrors.clear()
        if (artist.isBlank()) {
            fieldErrors["artist"] = "Nama band wajib diisi"
        }
        if (submissionMode == "channel" && channelUrl.isBlank()) {
            fieldErrors["channelUrl"] = "Link channel YouTube wajib diisi"
        } else if (submissionMode == "manual" && videoUrl.isBlank()) {
            fieldErrors["videoUrl"] = "Link video YouTube wajib diisi"
        } else if (submissionMode == "manual" &&
            !videoUrl.contains("youtube.com", ignoreCase = true) &&
            !videoUrl.contains("youtu.be", ignoreCase = true)
        ) {
            fieldErrors["videoUrl"] = "Gunakan link YouTube yang valid"
        }
        return fieldErrors.isEmpty()
    }

    fun clearForm() {
        title = ""
        artist = ""
        releaseYear = ""
        albumName = "Single"
        genre = ""
        videoUrl = ""
        channelUrl = ""
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
                                text = if (submissionMode == "channel") "Tambah Channel YouTube" else "Tambah Link Video YouTube",
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
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("manual" to "Link Video", "channel" to "Channel YouTube").forEach { (mode, label) ->
                        val selected = submissionMode == mode
                        Button(
                            onClick = { submissionMode = mode; fieldErrors.clear() },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        ) { Text(label, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            item { FormField(artist, { artist = it }, if (submissionMode == "channel") "Nama Band / Channel" else "Nama Band", fieldErrors["artist"]) }
            if (submissionMode == "manual") {
                item { FormField(title, { title = it }, "Judul Video (opsional)", null) }
                item { FormField(videoUrl, { videoUrl = it }, "Link Video YouTube", fieldErrors["videoUrl"]) }
                item {
                    Text(
                        "Satu link menambahkan satu video. Untuk mengambil semua video dari satu channel, gunakan menu Channel YouTube.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                item {
                    FormField(
                        channelUrl,
                        { channelUrl = it },
                        "Link Channel YouTube",
                        fieldErrors["channelUrl"]
                    )
                }
                item {
                    Text(
                        "Video publik dari channel akan muncul otomatis di menu MV dan ikut terbarui saat channel upload video baru.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            item {
                Button(
                    onClick = {
                        if (!validate()) {
                            scope.launch { snackbarHostState.showSnackbar("Lengkapi nama band dan link yang diperlukan.") }
                            return@Button
                        }

                        mvRegistrationRewardGate.show(
                            activity,
                            {
                                isLoading = true
                                scope.launch {
                                    val result = if (submissionMode == "channel") {
                                        repository.submitYoutubeChannel(
                                            artist = artist,
                                            channelUrl = channelUrl,
                                            adminSession = adminSession?.takeIf { isActiveAdmin }
                                        )
                                    } else {
                                        repository.submitMvRegistration(
                                            MvRegistrationPayload(
                                                title = title.trim(),
                                                artist = artist.trim(),
                                                releaseYear = releaseYear.trim(),
                                                albumName = albumName.trim(),
                                                genre = genre.trim(),
                                                videoUrl = videoUrl.trim()
                                            ),
                                            adminSession = adminSession?.takeIf { isActiveAdmin }
                                        )
                                    }
                                    isLoading = false

                                    result.onSuccess {
                                        mvRegistrationRewardGate.markSubmissionCompleted()
                                        snackbarHostState.showSnackbar(
                                            if (submissionMode == "channel") {
                                                if (isActiveAdmin) "Channel YouTube berhasil ditambahkan. Tarik ulang menu MV untuk melihat videonya."
                                                else "Pengajuan channel YouTube berhasil dikirim dan menunggu persetujuan admin."
                                            } else {
                                                if (isActiveAdmin) "MV berhasil ditambahkan."
                                                else "Registrasi MV berhasil dikirim dan menunggu persetujuan admin."
                                            }
                                        )
                                        clearForm()
                                    }.onFailure { error ->
                                        snackbarHostState.showSnackbar(
                                            "Gagal mengirim registrasi MV: ${error.message ?: "Terjadi kesalahan, coba lagi."}"
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
                    modifier = Modifier.fillMaxWidth().height(54.dp).padding(top = 8.dp),
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
                        Text("Kirim Registrasi MV", fontWeight = FontWeight.Bold)
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
    error: String?
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
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done
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
