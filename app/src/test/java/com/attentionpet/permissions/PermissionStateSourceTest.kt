package com.attentionpet.permissions

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStateSourceTest {
    @Test
    fun usageAccessCheckUsesMinSdkSafeAppOpsApi() {
        val source = Files.readString(
            Paths.get("src/main/java/com/attentionpet/permissions/PermissionState.kt")
        )

        assertTrue(source.contains(".checkOpNoThrow("))
        assertFalse(source.contains("unsafeCheckOpNoThrow"))
    }
}
