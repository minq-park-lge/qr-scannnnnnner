package com.sssnaplab.snapqr.preview

import android.net.Uri

data class LinkPreview(
    /** 리다이렉트를 모두 따라간 최종 URL */
    val finalUrl: Uri,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val faviconUrl: String?,
    val siteName: String?,
) {
    val finalHost: String get() = finalUrl.host.orEmpty().removePrefix("www.")
}

sealed interface PreviewState {
    data object Loading : PreviewState
    data class Loaded(val preview: LinkPreview) : PreviewState
    data class Failed(val message: String) : PreviewState
}
