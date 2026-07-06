package com.attentionpet

import com.attentionpet.domain.PetState

object AttentionPetTestIds {
    const val START_MONITORING = "attention_pet_start_monitoring"
    const val OVERLAY_CAPSULE = "attention_pet_overlay_capsule"
    const val OVERLAY_PANEL = "attention_pet_overlay_panel"
    const val OVERLAY_PANEL_DISMISS = "attention_pet_overlay_panel_dismiss"
    const val TIMEOUT_SHEET = "attention_pet_timeout_sheet"
    const val OVERLAY_STATE_PREFIX = "attention_pet_overlay_state:"
    const val OVERLAY_SESSION_MILLIS_PREFIX = "attention_pet_overlay_session_ms:"

    fun overlayState(state: PetState): String = "$OVERLAY_STATE_PREFIX${state.serialized}"

    fun overlaySessionMillis(millis: Long): String = "$OVERLAY_SESSION_MILLIS_PREFIX$millis"
}
