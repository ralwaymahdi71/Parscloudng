package com.v2ray.ang.handler

import android.content.Context
import com.v2ray.ang.RemoteConfig
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.util.CountryFlagUtil

/**
 * Orchestrates the simplified "Free VPN" screen (ui/simple/SimpleVpnActivity) on top of
 * the app's existing subscription, config-parsing, ping-test, and VPN-connect systems.
 *
 * This class does NOT talk to the network or to the VPN core directly — it only
 * coordinates existing pieces (AngConfigManager, MmkvManager, LauncherManager) and
 * enforces the cache/expiry rules the simplified client needs on top of them.
 */
object SimpleVpnRepository {

    /** Fixed subscription id so we always find the same hidden subscription. */
    const val SIMPLE_SUB_ID = "simple_free_vpn"

    data class SimpleServerUiModel(
        val guid: String,
        val displayName: String,
        val countryName: String?,
        val flagEmoji: String,
        /** null = not tested yet, -1 = failed/timeout, >=0 = ms */
        val delayMillis: Long?,
    )

    sealed class RefreshOutcome {
        data class Updated(val serverCount: Int) : RefreshOutcome()
        data object EmptiedByServer : RefreshOutcome()
        data object KeepingCacheAfterNetworkError : RefreshOutcome()
        data object CacheUnavailableAfterNetworkError : RefreshOutcome()
    }

    /** Gets (creating if needed) the hidden subscription pointing at RemoteConfig.SERVER_LIST_URL. */
    fun ensureSubscription(): SubscriptionCache {
        val existing = MmkvManager.decodeSubscription(SIMPLE_SUB_ID)
        val subItem = existing ?: SubscriptionItem(
            remarks = "Free VPN",
            url = RemoteConfig.SERVER_LIST_URL,
            enabled = true,
            autoUpdate = false,
        )
        if (existing == null || existing.url != RemoteConfig.SERVER_LIST_URL) {
            subItem.url = RemoteConfig.SERVER_LIST_URL
            MmkvManager.encodeSubscription(SIMPLE_SUB_ID, subItem)
        }
        return SubscriptionCache(SIMPLE_SUB_ID, subItem)
    }

    fun getServerGuids(): List<String> = MmkvManager.decodeServerList(SIMPLE_SUB_ID)

    fun getServerUiModel(guid: String): SimpleServerUiModel? {
        val profile = MmkvManager.decodeServerConfig(guid) ?: return null
        val delay = MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis
        val name = profile.remarks.ifBlank { profile.description.orEmpty().ifBlank { guid } }
        val info = CountryFlagUtil.extract(profile.remarks)
        return SimpleServerUiModel(
            guid = guid,
            displayName = name,
            countryName = info.countryName,
            flagEmoji = info.flagEmoji,
            delayMillis = delay,
        )
    }

    fun getServers(): List<SimpleServerUiModel> = getServerGuids().mapNotNull(::getServerUiModel)

    fun sortByPing() {
        AngConfigManager.sortByTestResultsForSub(SIMPLE_SUB_ID)
    }

    private fun isCacheExpired(sub: SubscriptionItem): Boolean {
        if (sub.lastUpdated <= 0L) return true
        val expiryMillis = RemoteConfig.REMOTE_DATA_EXPIRY_MINUTES * 60_000L
        return System.currentTimeMillis() - sub.lastUpdated > expiryMillis
    }

    /**
     * Whether the app is currently allowed to start a NEW connection:
     * the server list must be non-empty and the cache must not have expired.
     * (Section 5 of the spec.)
     */
    fun canConnect(): Boolean {
        val sub = MmkvManager.decodeSubscription(SIMPLE_SUB_ID) ?: return false
        if (getServerGuids().isEmpty()) return false
        return !isCacheExpired(sub)
    }

    /**
     * Wipes every locally-cached server for the simple subscription and disconnects
     * an active VPN connection, if any. Used when the server operator intentionally
     * sends back an empty/invalid list (Section 4, case A) and when a network-error
     * cache finally expires (Section 4, case B).
     */
    fun wipeAllAndDisconnect(context: Context) {
        val guids = getServerGuids()
        if (guids.isNotEmpty()) {
            MmkvManager.removeServers(guids, SIMPLE_SUB_ID)
        }
        MmkvManager.decodeSubscription(SIMPLE_SUB_ID)?.let { sub ->
            sub.lastUpdated = -1
            MmkvManager.encodeSubscription(SIMPLE_SUB_ID, sub)
        }
        // No-op if nothing is running; safe to call unconditionally.
        LauncherManager.stopService(context)
    }

    /**
     * Runs one refresh cycle against RemoteConfig.SERVER_LIST_URL and applies the
     * cache/expiry rules from Section 4 of the spec. Ping-testing and sorting the
     * newly-downloaded servers is the caller's responsibility (see SimpleVpnViewModel)
     * since that needs to report live per-server progress to the UI.
     */
    fun refresh(context: Context): RefreshOutcome {
        val sub = ensureSubscription()
        return when (val result = AngConfigManager.refreshSimpleServerList(sub)) {
            is AngConfigManager.SimpleRefreshResult.Success ->
                RefreshOutcome.Updated(result.configCount)

            AngConfigManager.SimpleRefreshResult.EmptyOrInvalid -> {
                wipeAllAndDisconnect(context)
                RefreshOutcome.EmptiedByServer
            }

            AngConfigManager.SimpleRefreshResult.NetworkError -> {
                val current = MmkvManager.decodeSubscription(SIMPLE_SUB_ID)
                val stillUsable = current != null &&
                    !isCacheExpired(current) &&
                    getServerGuids().isNotEmpty()
                if (stillUsable) {
                    RefreshOutcome.KeepingCacheAfterNetworkError
                } else {
                    // Cache is gone or expired and we couldn't refresh it: make sure the
                    // user can't connect (or stay connected) on stale/nonexistent data.
                    LauncherManager.stopService(context)
                    RefreshOutcome.CacheUnavailableAfterNetworkError
                }
            }
        }
    }
}
