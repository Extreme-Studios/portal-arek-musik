package com.extremestudios.portalarekmusik.ui

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

@Composable
fun rememberRewardedSubmissionGate(
    context: Context,
    adUnitId: String,
    preferenceName: String,
    cooldownKey: String,
    cooldownMs: Long,
    requiresReward: Boolean = false
): RewardedSubmissionGate {
    val preferences = remember { context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE) }
    var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
    var isAdLoading by remember { mutableStateOf(false) }

    fun loadAd() {
        if (isAdLoading) return
        isAdLoading = true
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isAdLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isAdLoading = false
                }
            }
        )
    }

    fun remainingCooldownMs(): Long {
        val lastAt = preferences.getLong(cooldownKey, 0L)
        val elapsed = System.currentTimeMillis() - lastAt
        return (cooldownMs - elapsed).coerceAtLeast(0L)
    }

    LaunchedEffect(adUnitId) {
        loadAd()
    }

    return remember(context, adUnitId, preferenceName, cooldownKey, cooldownMs, requiresReward, rewardedAd, isAdLoading) {
        RewardedSubmissionGate(
            isAdLoading = isAdLoading,
            remainingCooldownMs = ::remainingCooldownMs,
            ensureAdLoaded = ::loadAd,
            markSubmissionCompleted = {
                if (requiresReward) preferences.edit().putLong(cooldownKey, System.currentTimeMillis()).apply()
            },
            show = { activity, onRewardEarned, onMessage, onLoadingChanged ->
                if (!requiresReward) {
                    onRewardEarned()
                } else if (activity == null) {
                    onMessage("Iklan reward membutuhkan layar aplikasi yang aktif.")
                } else {
                    val remainingMs = remainingCooldownMs()
                    if (remainingMs > 0L) {
                        val remainingMinutes = kotlin.math.ceil(remainingMs / 60_000.0).toInt()
                        onMessage("Silakan tunggu $remainingMinutes menit sebelum mengirim lagi.")
                        return@RewardedSubmissionGate
                    }
                    val ad = rewardedAd
                    if (ad == null) {
                        loadAd()
                        onMessage("Iklan reward sedang dimuat. Coba kirim lagi beberapa saat.")
                    } else {
                        var rewardEarned = false
                        onLoadingChanged(true)
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                rewardedAd = null
                                onLoadingChanged(false)
                                loadAd()
                                if (!rewardEarned) {
                                    onMessage("Tonton iklan reward sampai selesai untuk mengirim poster.")
                                }
                            }

                            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                rewardedAd = null
                                onLoadingChanged(false)
                                loadAd()
                                onMessage("Iklan reward belum dapat ditampilkan. Coba lagi.")
                            }
                        }
                        ad.show(activity) {
                            rewardEarned = true
                            onRewardEarned()
                        }
                    }
                }
            }
        )
    }
}

data class RewardedSubmissionGate(
    val isAdLoading: Boolean,
    val remainingCooldownMs: () -> Long,
    val ensureAdLoaded: () -> Unit,
    val markSubmissionCompleted: () -> Unit,
    val show: (
        activity: Activity?,
        onRewardEarned: () -> Unit,
        onMessage: (String) -> Unit,
        onLoadingChanged: (Boolean) -> Unit
    ) -> Unit
)

const val PORTAL_REWARD_PREFERENCE_NAME = "portal_reward_ads"
const val HUB_REGISTER_REWARD_COOLDOWN_KEY = "last_hub_register_reward_at_ms"
const val HUB_LOGIN_REWARD_COOLDOWN_KEY = "last_hub_login_reward_at_ms"
const val PORTAL_POSTER_REWARD_COOLDOWN_KEY = "last_portal_poster_reward_at_ms"
const val SONG_REGISTRATION_REWARD_COOLDOWN_KEY = "last_song_registration_reward_at_ms"
const val PORTAL_REWARD_COOLDOWN_MS = 5 * 60 * 1000L
const val POSTER_SUBMISSION_REWARD_COOLDOWN_MS = 60 * 60 * 1000L
const val NON_ADMIN_SONG_REWARD_COOLDOWN_MS = 30 * 60 * 1000L
const val REWARD_AD_UNIT_ID = "ca-app-pub-8451272966172172/9941394221"
