package com.extremestudios.portalarekmusik.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenSongUpload: () -> Unit,
    onOpenMusicVideoUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column {
                    Text("About Us", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("Portal Arek Musik adalah wadah pergerakan musik independen yang dikurasi oleh Extreme Studios. Kami percaya bahwa setiap nada lokal layak mendapatkan panggung yang megah.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp), color = MaterialTheme.colorScheme.onSurface)
                }
            }
            item {
                AboutSectionTitle("Submission Guidelines")
                Spacer(Modifier.height(6.dp))
                GuidelineCard("Karya Orisinal", "Pastikan lagu yang disubmit adalah milik sendiri atau band yang bersangkutan.")
                Spacer(Modifier.height(10.dp))
                GuidelineCard("Hak Kekayaan Intelektual", "Tidak melanggar hak cipta pihak manapun dalam bentuk sampel atau lirik.")
                Spacer(Modifier.height(10.dp))
                GuidelineCard("Lisensi Distribusi", "Memberi izin kepada Portal Arek Musik untuk menampilkan karya di dalam platform.")
            }
            item {
                AboutSectionTitle("Kirim Karya")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Punya karya baru? Kirim musik atau music video untuk ditinjau dan ditampilkan di Portal Arek Musik.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onOpenSongUpload,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Upload Music")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onOpenMusicVideoUpload,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Upload Music Video")
                }
            }
        }
    }
}

@Composable
private fun GuidelineCard(title: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AboutSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
}
