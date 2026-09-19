package com.extremestudios.portalarekmusik.ui.screens

import android.content.Intent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import coil.compose.SubcomposeAsyncImage
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.extremestudios.portalarekmusik.data.local.PortalSessionStore
import com.extremestudios.portalarekmusik.data.model.PortalPost
import com.extremestudios.portalarekmusik.data.model.PortalUserSession
import com.extremestudios.portalarekmusik.data.repository.PortalRepository
import com.extremestudios.portalarekmusik.ui.PORTAL_POSTER_REWARD_COOLDOWN_KEY
import com.extremestudios.portalarekmusik.ui.POSTER_SUBMISSION_REWARD_COOLDOWN_MS
import com.extremestudios.portalarekmusik.ui.PORTAL_REWARD_PREFERENCE_NAME
import com.extremestudios.portalarekmusik.ui.REWARD_AD_UNIT_ID
import com.extremestudios.portalarekmusik.ui.SelectedImagePayload
import com.extremestudios.portalarekmusik.ui.readSelectedImage
import com.extremestudios.portalarekmusik.ui.rememberRewardedSubmissionGate
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PortalKitaScreen(
    posts: List<PortalPost>,
    categories: List<String>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onPostPreview: (PortalPost) -> Unit,
    onOpenPosterSubmission: () -> Unit,
    canDeletePosts: Boolean,
    onDeletePost: (PortalPost) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    var showCategories by remember { mutableStateOf(false) }
    var postPendingDeletion by remember { mutableStateOf<PortalPost?>(null) }
    val filteredPosts = remember(posts, selectedCategory) {
        posts.filter { selectedCategory == "All" || it.category == selectedCategory }
    }

    postPendingDeletion?.let { post ->
        AlertDialog(
            onDismissRequest = { postPendingDeletion = null },
            title = { Text("Hapus poster?") },
            text = { Text("Poster ${post.category.ifBlank { "ini" }} akan dihapus dari Portal Kita.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePost(post)
                    postPendingDeletion = null
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { postPendingDeletion = null }) { Text("Batal") }
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PORTAL", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("KITA", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Komunitas Musik Indie Indonesia",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = onOpenPosterSubmission, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("+ Pasang Poster", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                FilterChip(
                    selected = selectedCategory == "All",
                    onClick = {
                        selectedCategory = "All"
                        showCategories = !showCategories
                    },
                    label = { Text(if (showCategories) "All  ▲" else "All  ▼") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(12.dp)
                )
                if (showCategories) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        categories.filterNot { it == "All" }.forEach { category ->
                            item {
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = null,
                            shape = RoundedCornerShape(12.dp)
                        )
                        }
                    }
                }
                }
            }
        }

        if (errorMessage != null && filteredPosts.isNotEmpty()) {
            item {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (isLoading) {
            item {
                Text(
                    text = "Memuat poster...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (errorMessage != null && filteredPosts.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onRefresh) {
                        Text("Coba Lagi")
                    }
                }
            }
        } else if (filteredPosts.isEmpty()) {
            item {
                Text(
                    text = "Belum ada poster di kategori ini.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(filteredPosts, key = { it.id }) { post ->
                PortalPostCard(
                    post = post,
                    onPrimaryClick = {
                        if (post.targetUrl.isNotBlank()) {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(post.targetUrl))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } else {
                            onPostPreview(post)
                        }
                    },
                    onDelete = if (canDeletePosts) {{ postPendingDeletion = post }} else null
                )
            }
        }
    }
}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortalPosterSubmissionScreen(
    modifier: Modifier = Modifier,
    repository: PortalRepository = remember { PortalRepository() },
    onBack: () -> Unit,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionStore = remember { PortalSessionStore(context) }
    val session by sessionStore.sessionFlow.collectAsState(initial = null)
    var resolvedSession by remember { mutableStateOf<PortalUserSession?>(null) }
    androidx.compose.runtime.LaunchedEffect(session?.username, session?.status) {
        if (session?.status.equals("ACTIVE", ignoreCase = true)) {
            resolvedSession = session
        } else if (session == null) {
            // keep the last active session latched while submit flow is open
        } else {
            resolvedSession = null
        }
    }
    val canPostPoster = resolvedSession?.status.equals("ACTIVE", ignoreCase = true)
    val posterDeviceId = remember(context) { getPosterDeviceId(context) }
    val categories = remember { repository.getSubmissionCategories() }
    val fieldErrors = remember { mutableStateMapOf<String, String>() }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedPosterImage by remember { mutableStateOf<SelectedImagePayload?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val posterRewardGate = rememberRewardedSubmissionGate(
        context = context,
        adUnitId = REWARD_AD_UNIT_ID,
        preferenceName = PORTAL_REWARD_PREFERENCE_NAME,
        cooldownKey = PORTAL_POSTER_REWARD_COOLDOWN_KEY,
        cooldownMs = POSTER_SUBMISSION_REWARD_COOLDOWN_MS,
        requiresReward = true
    )
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            readSelectedImage(context, uri)
                .onSuccess {
                    selectedPosterImage = it
                    fieldErrors.remove("posterImage")
                }
                .onFailure { error ->
                    snackbarHostState.showSnackbar(error.message ?: "Gagal memuat poster.")
                }
        }
    }

    fun validate(): Boolean {
        fieldErrors.clear()
        if (selectedCategory.isBlank()) fieldErrors["category"] = "Kategori poster wajib dipilih"
        if (selectedPosterImage == null) fieldErrors["posterImage"] = "Poster wajib dipilih"
        return fieldErrors.isEmpty()
    }

    fun clearForm() {
        selectedCategory = ""
        selectedPosterImage = null
        fieldErrors.clear()
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SnackbarHost(hostState = snackbarHostState)
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TextButton(onClick = onBack) {
                    Text("Kembali")
                }
            }
            item {
                Text(
                    text = "Pasang Poster",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pilih kategori poster", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = {
                                        selectedCategory = category
                                        fieldErrors.remove("category")
                                    },
                                    label = { Text(category) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                        fieldErrors["category"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(if (selectedPosterImage == null) "Pilih Poster" else "Ganti Poster")
                        }
                        selectedPosterImage?.let { poster ->
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Image(
                                    bitmap = poster.previewBitmap.asImageBitmap(),
                                    contentDescription = "Preview poster",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.72f)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                                Text(
                                    text = poster.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        fieldErrors["posterImage"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
                item {
                    Button(
                        onClick = {
                            if (!validate()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Form poster belum lengkap.")
                                }
                                return@Button
                            }

                            posterRewardGate.show(
                                context as? android.app.Activity,
                                {
                                    isSubmitting = true
                                    scope.launch {
                                        val activeSession = resolvedSession
                                        val result = repository.submitPoster(
                                            session = activeSession,
                                            deviceId = posterDeviceId,
                                            category = selectedCategory,
                                            posterImageBase64 = requireNotNull(selectedPosterImage).base64,
                                            posterImageFileName = requireNotNull(selectedPosterImage).fileName,
                                            posterImageMimeType = requireNotNull(selectedPosterImage).mimeType
                                        )
                                        isSubmitting = false
                                        result.onSuccess {
                                            posterRewardGate.markSubmissionCompleted()
                                            snackbarHostState.showSnackbar("Poster berhasil dikirim untuk review admin.")
                                            clearForm()
                                            onSubmitted()
                                        }.onFailure { error ->
                                            snackbarHostState.showSnackbar(
                                                error.message ?: "Gagal mengirim poster."
                                            )
                                        }
                                    }
                                },
                                { message ->
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                },
                                { isSubmitting = it }
                            )
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isSubmitting) {
                            Text("Mengirim...")
                        } else {
                            Text("Kirim Poster", fontWeight = FontWeight.Bold)
                        }
                    }
            }
        }
    }
}

private fun getPosterDeviceId(context: Context): String {
    return Settings.Secure.getString(
        context.applicationContext.contentResolver,
        Settings.Secure.ANDROID_ID
    ).orEmpty().trim()
}

@Composable
private fun LoggedInPortalHeader(
    session: PortalUserSession
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Logged in as Admin: ${session.username}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Status: ${session.status}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PortalPostDetailScreen(
    post: PortalPost,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isFullscreenPreview by remember { mutableStateOf(false) }
    if (isFullscreenPreview) {
        FullscreenPosterViewer(
            imageUrl = post.posterUrl,
            title = post.title,
            onExit = { isFullscreenPreview = false }
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            TextButton(onClick = onBack) {
                Text("Kembali")
            }
        }
        item {
            RemotePoster(
                imageUrl = post.posterUrl,
                title = post.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clickable { isFullscreenPreview = true }
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = post.displayDate(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                AssistChip(
                    onClick = {},
                    label = { Text(post.category) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        labelColor = MaterialTheme.colorScheme.primary
                    )
                )
                if (post.shortCaption.isNotBlank()) {
                    Text(
                        text = post.shortCaption,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (post.eventDate.isNotBlank()) {
                    Text(
                        text = post.eventDate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (post.targetUrl.isNotBlank()) {
                    FilledTonalButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(post.targetUrl))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    ) {
                        Text("Buka Link")
                    }
                }
            }
        }
    }
}

@Composable
private fun PortalPostCard(
    post: PortalPost,
    onPrimaryClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPrimaryClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box {
                RemotePoster(
                    imageUrl = post.posterUrl,
                    title = post.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.1f)
                )
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                            .size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus poster",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!post.title.equals(post.category, ignoreCase = true)) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = post.submitterName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(post.category) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        border = null
                    )
                    if (post.eventDate.isNotBlank()) {
                        Text(
                            text = post.eventDate,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (post.shortCaption.isNotBlank()) {
                    Text(
                        text = post.shortCaption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (post.targetUrl.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onPrimaryClick) {
                            Text("Buka Link")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemotePoster(
    imageUrl: String,
    title: String,
    modifier: Modifier = Modifier
) {
    if (imageUrl.isBlank()) {
        Surface(
            modifier = modifier.clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada poster",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = "$title poster",
            modifier = modifier.clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Fit,
            loading = {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Loading poster...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            error = {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Poster gagal dimuat",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun FullscreenPosterViewer(
    imageUrl: String,
    title: String,
    onExit: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = nextScale
        if (nextScale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(scale) {
                detectTapGestures(
                    onTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            onExit()
                        }
                    },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.4f
                        }
                    }
                )
            }
            .transformable(transformableState),
        contentAlignment = Alignment.Center
    ) {
        RemotePoster(
            imageUrl = imageUrl,
            title = title,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        )
    }
}

private fun PortalPost.displayDate(): String {
    val rawDate = eventDate.ifBlank { createdAt }.trim()
    if (rawDate.isBlank()) return "Tanggal posting belum tersedia"
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val parsed = OffsetDateTime.parse(rawDate)
            "Diposting ${parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID")))}"
        } else {
            "Diposting $rawDate"
        }
    }.getOrElse {
        "Diposting $rawDate"
    }
}
