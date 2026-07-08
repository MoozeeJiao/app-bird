package com.attentionpet.permissions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun missingPermissionsAreReportedUsageFirst() {
        val bothMissing = PermissionSnapshot(usageAccessGranted = false, overlayGranted = false)
        assertEquals(
            listOf(RequiredPermission.USAGE_ACCESS, RequiredPermission.OVERLAY),
            bothMissing.missingPermissions()
        )
        assertEquals(RequiredPermission.USAGE_ACCESS, bothMissing.nextMissingPermission())

        val overlayMissing = PermissionSnapshot(usageAccessGranted = true, overlayGranted = false)
        assertEquals(listOf(RequiredPermission.OVERLAY), overlayMissing.missingPermissions())
        assertEquals(RequiredPermission.OVERLAY, overlayMissing.nextMissingPermission())

        assertTrue(PermissionSnapshot(usageAccessGranted = true, overlayGranted = true).missingPermissions().isEmpty())
        assertNull(PermissionSnapshot(usageAccessGranted = true, overlayGranted = true).nextMissingPermission())
    }
}
