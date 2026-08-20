package com.kira.kdownloader.engine;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class YtDlpVersionPolicyTest {
    @Test public void installsBundledVersionWhenInstalledCopyIsStaleOrUnreadable() {
        assertTrue(YtDlpVersionPolicy.shouldInstallBundled(null, "2026.07.04"));
        assertTrue(YtDlpVersionPolicy.shouldInstallBundled("2025.12.31", "2026.07.04"));
        assertTrue(YtDlpVersionPolicy.shouldInstallBundled("broken", "2026.07.04"));
    }

    @Test public void keepsMatchingAndNewerSelfUpdatedVersions() {
        assertFalse(YtDlpVersionPolicy.shouldInstallBundled("2026.07.04", "2026.07.04"));
        assertFalse(YtDlpVersionPolicy.shouldInstallBundled("2026.07.05", "2026.07.04"));
        assertFalse(YtDlpVersionPolicy.shouldInstallBundled("stable@2026.07.10", "2026.07.04"));
    }
}
