package com.kira.kdownloader.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YtDlpVersionPolicyTest {
    @Test
    fun `installs bundled version when installed copy is stale or unreadable`() {
        assertTrue(YtDlpVersionPolicy.shouldInstallBundled(null, "2026.07.04"))
        assertTrue(YtDlpVersionPolicy.shouldInstallBundled("2025.12.31", "2026.07.04"))
        assertTrue(YtDlpVersionPolicy.shouldInstallBundled("broken", "2026.07.04"))
    }

    @Test
    fun `keeps matching and newer self-updated versions`() {
        assertFalse(YtDlpVersionPolicy.shouldInstallBundled("2026.07.04", "2026.07.04"))
        assertFalse(YtDlpVersionPolicy.shouldInstallBundled("2026.07.05", "2026.07.04"))
        assertFalse(
            YtDlpVersionPolicy.shouldInstallBundled(
                "stable@2026.07.10",
                "2026.07.04",
            ),
        )
    }
}
