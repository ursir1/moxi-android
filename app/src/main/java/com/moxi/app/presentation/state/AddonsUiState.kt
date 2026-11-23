package com.moxi.app.presentation.state

import com.moxi.app.data.model.AddonItem

data class AddonSelectableOption(
    val label: String,
    val selected: Boolean,
    val request: com.stremio.core.types.addon.ResourceRequest,
)

data class AddonsUiState(
    val isBrowsingRemote: Boolean = false,
    val items: List<AddonItem> = emptyList(),
    val selectableTypes: List<AddonSelectableOption> = emptyList(),
    val selectableCatalogs: List<AddonSelectableOption> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

