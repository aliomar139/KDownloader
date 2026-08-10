package com.kira.kdownloader.settings

import com.kira.kdownloader.settings.store.InMemoryKeyValueStore
import com.kira.kdownloader.settings.store.InMemorySecureStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    private fun repo(
        store: InMemoryKeyValueStore = InMemoryKeyValueStore(),
        secure: InMemorySecureStore = InMemorySecureStore(),
    ) = SettingsRepository(store, secure)

    @Test
    fun `fresh install returns documented defaults`() {
        assertEquals(AppSettings.DEFAULTS, repo().read())
    }

    @Test
    fun `settings persist across repository recreation`() {
        val store = InMemoryKeyValueStore()
        repo(store).update {
            it.copy(download = it.download.copy(downloadType = DownloadType.AUDIO_ONLY, audioFormat = AudioFormat.OPUS))
        }
        // Simulate an app restart by reading through a new repository over the same store.
        val restored = repo(store).read()
        assertEquals(DownloadType.AUDIO_ONLY, restored.download.downloadType)
        assertEquals(AudioFormat.OPUS, restored.download.audioFormat)
    }

    @Test
    fun `out of range values are clamped on save`() {
        val store = InMemoryKeyValueStore()
        repo(store).update {
            it.copy(
                behavior = it.behavior.copy(maxSimultaneousDownloads = 99, maxRetryCount = -4),
                network = it.network.copy(proxyPort = 99999),
            )
        }
        val read = repo(store).read()
        assertEquals(5, read.behavior.maxSimultaneousDownloads)
        assertEquals(0, read.behavior.maxRetryCount)
        assertEquals(65535, read.network.proxyPort)
    }

    @Test
    fun `invalid filename template is coerced to a safe default`() {
        val store = InMemoryKeyValueStore()
        repo(store).update { it.copy(storage = it.storage.copy(filenameTemplate = "{oops")) }
        assertEquals("{title}", repo(store).read().storage.filenameTemplate)
    }

    @Test
    fun `malformed stored value falls back to default instead of crashing`() {
        val store = InMemoryKeyValueStore(
            mapOf(
                SettingsKeys.DL_TYPE to "not_a_real_type",
                SettingsKeys.BH_MAX_PARALLEL to "not_a_number",
            ),
        )
        val read = repo(store).read()
        assertEquals(DownloadType.VIDEO, read.download.downloadType)
        assertEquals(2, read.behavior.maxSimultaneousDownloads)
    }

    @Test
    fun `reset category only affects that category`() {
        val store = InMemoryKeyValueStore()
        val r = repo(store)
        r.update {
            it.copy(
                download = it.download.copy(downloadType = DownloadType.AUDIO_ONLY),
                appearance = it.appearance.copy(theme = AppTheme.DARK),
            )
        }
        r.resetCategory(SettingsCategory.DOWNLOAD)
        val read = r.read()
        assertEquals(DownloadType.VIDEO, read.download.downloadType) // reset
        assertEquals(AppTheme.DARK, read.appearance.theme)           // preserved
    }

    @Test
    fun `reset all restores every default`() {
        val store = InMemoryKeyValueStore()
        val r = repo(store)
        r.update { it.copy(appearance = it.appearance.copy(theme = AppTheme.DARK, compactList = true)) }
        assertNotEquals(AppSettings.DEFAULTS, r.read())
        r.resetAll()
        assertEquals(AppSettings.DEFAULTS, r.read())
    }

    @Test
    fun `proxy password uses secure storage and never appears in the store or export`() {
        val store = InMemoryKeyValueStore()
        val secure = InMemorySecureStore()
        val r = repo(store, secure)
        r.update { it.copy(network = it.network.copy(proxyUsername = "alice")) }
        r.setProxyPassword("s3cret-pass")

        // Retrievable for engine use.
        assertEquals("s3cret-pass", r.proxyPassword())
        // Marker reflected in the snapshot.
        assertTrue(r.read().network.proxyPasswordSet)
        // Never written to the plain store.
        assertFalse(store.snapshot().values.any { it.contains("s3cret-pass") })
        // Excluded from export (and username too).
        val json = r.exportToJson()
        assertFalse(json.contains("s3cret-pass"))
        assertFalse(json.contains("alice"))
    }

    @Test
    fun `export then import reproduces non-sensitive settings`() {
        val source = InMemoryKeyValueStore()
        val sourceRepo = repo(source)
        sourceRepo.update {
            it.copy(
                download = it.download.copy(videoQuality = VideoQuality.P720),
                behavior = it.behavior.copy(maxSimultaneousDownloads = 4),
                appearance = it.appearance.copy(theme = AppTheme.LIGHT, languageTag = "fr"),
            )
        }
        val json = sourceRepo.exportToJson()

        val target = InMemoryKeyValueStore()
        val result = repo(target).importFromJson(json)
        assertTrue(result is SettingsRepository.ImportResult.Success)

        val imported = repo(target).read()
        assertEquals(VideoQuality.P720, imported.download.videoQuality)
        assertEquals(4, imported.behavior.maxSimultaneousDownloads)
        assertEquals("fr", imported.appearance.languageTag)
    }

    @Test
    fun `import ignores unknown keys and rejects malformed files`() {
        val store = InMemoryKeyValueStore()
        val r = repo(store)
        val withUnknown = """{"appearance.theme": "dark", "some.future.key": "value"}"""
        assertTrue(r.importFromJson(withUnknown) is SettingsRepository.ImportResult.Success)
        assertEquals(AppTheme.DARK, r.read().appearance.theme)

        assertTrue(r.importFromJson("garbage, not json") is SettingsRepository.ImportResult.Failure)
    }

    @Test
    fun `boolean dependencies persist independently`() {
        val store = InMemoryKeyValueStore()
        val r = repo(store)
        r.update {
            it.copy(
                subtitles = it.subtitles.copy(downloadSubtitles = true, includeAllLanguages = true),
                behavior = it.behavior.copy(preventDuplicates = false, duplicateDetection = DuplicateDetection.MEDIA_ID),
            )
        }
        val read = r.read()
        assertTrue(read.subtitles.downloadSubtitles)
        assertTrue(read.subtitles.includeAllLanguages)
        assertFalse(read.behavior.preventDuplicates)
        assertEquals(DuplicateDetection.MEDIA_ID, read.behavior.duplicateDetection)
    }
}
