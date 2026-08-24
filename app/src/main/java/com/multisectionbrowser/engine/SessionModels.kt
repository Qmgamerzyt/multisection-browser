package com.multisectionbrowser.engine

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.mozilla.geckoview.GeckoSession

@Parcelize
data class BrowserSession(
    val id: String,
    val name: String,
    val profileDir: String,
    val createdAt: Long = System.currentTimeMillis(),
    var isActive: Boolean = false
) : Parcelable

@Parcelize
data class BrowserTab(
    val id: String,
    val sessionId: String,
    val title: String = "New Tab",
    val url: String = "",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val favicon: String? = null,
    var isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable