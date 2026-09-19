package com.extremestudios.portalarekmusik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.extremestudios.portalarekmusik.navigation.PortalArekMusicApp
import com.extremestudios.portalarekmusik.ui.theme.PortalArekMusikTheme
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        // Draw app content behind the system bars on every supported Android version.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            PortalArekMusikTheme {
                var splashFinished by remember { mutableStateOf(false) }
                var continueToApp by remember { mutableStateOf(false) }
                if (!splashFinished) {
                    SplashScreen(onFinish = { splashFinished = true })
                } else if (!continueToApp) {
                    LaunchedEffect(Unit) {
                        continueToApp = true
                    }
                } else {
                    PortalArekMusicApp()
                }
            }
        }
    }

}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3500)
        onFinish()
    }
    val versionName = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0A12)),
        contentAlignment = Alignment.Center
    ) {
        val logoPainter = painterResource(id = R.drawable.splash)
        Image(
            painter = logoPainter,
            contentDescription = "Splash",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 98.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Powered By EXTREME STUDIOS",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
            Text(
                text = versionName,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp)
                    .zIndex(10f),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            CircularProgressIndicator(
                color = Color(0xFFA100FF).copy(alpha = 0.85f),
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun GlitterEffect() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val points = listOf(
            0.1f to 0.18f, 0.25f to 0.12f, 0.4f to 0.22f, 0.66f to 0.15f, 0.84f to 0.2f,
            0.17f to 0.44f, 0.35f to 0.38f, 0.58f to 0.48f, 0.77f to 0.42f, 0.9f to 0.36f
        )
        points.forEach { (x, y) ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFA100FF).copy(alpha = 0.6f), Color.Transparent)
                ),
                radius = 10.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(size.width * x, size.height * y)
            )
        }
    }
}
