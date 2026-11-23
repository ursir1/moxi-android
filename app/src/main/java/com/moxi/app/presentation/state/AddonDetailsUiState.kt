package com.moxi.app.presentation.state

import com.moxi.app.data.model.AddonItem

data class AddonDetailsUiState(
    val transportUrl: String,
    val localAddon: AddonItem? = null,
    val remoteAddon: AddonItem? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    /** The addon to render: prefer the freshly-fetched manifest, fall back to the installed copy. */
    val addon: AddonItem? get() = remoteAddon ?: localAddon
}

