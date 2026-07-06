package com.attentionpet.permissions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionSnapshotTest {
    @Test
    fun canStartMonitoringOnlyWhenUsageAndOverlayPermissionsAreGranted() {
        assertTrue(PermissionSnapshot(usageAccessGranted = true, overlayGranted = true).canStartMonitoring)
        assertFalse(PermissionSnapshot(usageAccessGranted = false, overlayGranted = true).canStartMonitoring)
        assertFalse(PermissionSnapshot(usageAccessGranted = true, overlayGranted = false).canStartMonitoring)
        assertFalse(PermissionSnapshot(usageAccessGranted = false, overlayGranted = false).canStartMonitoring)
    }
}
