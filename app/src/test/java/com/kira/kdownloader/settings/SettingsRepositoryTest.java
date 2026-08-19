package com.kira.kdownloader.settings;

import com.kira.kdownloader.settings.store.InMemoryKeyValueStore;
import com.kira.kdownloader.settings.store.InMemorySecureStore;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class SettingsRepositoryTest {
    private SettingsRepository repo() { return repo(new InMemoryKeyValueStore()); }
    private SettingsRepository repo(InMemoryKeyValueStore store) { return repo(store, new InMemorySecureStore()); }
    private SettingsRepository repo(InMemoryKeyValueStore store, InMemorySecureStore secure) { return new SettingsRepository(store, secure); }

    @Test public void freshInstallReturnsDocumentedDefaults() {
        assertEquals(AppSettings.DEFAULTS, repo().read());
    }

    @Test public void settingsPersistAcrossRepositoryRecreation() {
        InMemoryKeyValueStore store = new InMemoryKeyValueStore();
        repo(store).update(settings -> settings.withDownload(
                settings.getDownload().withDownloadType(DownloadType.AUDIO_ONLY).withAudioFormat(AudioFormat.OPUS)));
        AppSettings restored = repo(store).read();
        assertEquals(DownloadType.AUDIO_ONLY, restored.getDownload().getDownloadType());
        assertEquals(AudioFormat.OPUS, restored.getDownload().getAudioFormat());
    }

    @Test public void outOfRangeValuesAreClampedOnSave() {
        InMemoryKeyValueStore store = new InMemoryKeyValueStore();
        repo(store).update(settings -> settings
                .withBehavior(settings.getBehavior().withMaxSimultaneousDownloads(99).withMaxRetryCount(-4))
                .withNetwork(settings.getNetwork().withProxyPort(99999)));
        AppSettings read = repo(store).read();
        assertEquals(5, read.getBehavior().getMaxSimultaneousDownloads());
        assertEquals(0, read.getBehavior().getMaxRetryCount());
        assertEquals(65535, read.getNetwork().getProxyPort());
    }

    @Test public void invalidFilenameTemplateIsCoercedToASafeDefault() {
        InMemoryKeyValueStore store = new InMemoryKeyValueStore();
        repo(store).update(settings -> settings.withStorage(settings.getStorage().withFilenameTemplate("{oops")));
        assertEquals("{title}", repo(store).read().getStorage().getFilenameTemplate());
    }

    @Test public void malformedStoredValueFallsBackToDefaultInsteadOfCrashing() {
        Map<String, String> initial = new HashMap<>();
        initial.put(SettingsKeys.DL_TYPE, "not_a_real_type");
        initial.put(SettingsKeys.BH_MAX_PARALLEL, "not_a_number");
        AppSettings read = repo(new InMemoryKeyValueStore(initial)).read();
        assertEquals(DownloadType.VIDEO, read.getDownload().getDownloadType());
        assertEquals(2, read.getBehavior().getMaxSimultaneousDownloads());
    }

    @Test public void resetCategoryOnlyAffectsThatCategory() {
        InMemoryKeyValueStore store = new InMemoryKeyValueStore();
        SettingsRepository repository = repo(store);
        repository.update(settings -> settings
                .withDownload(settings.getDownload().withDownloadType(DownloadType.AUDIO_ONLY))
                .withAppearance(settings.getAppearance().withTheme(AppTheme.DARK)));
        repository.resetCategory(SettingsCategory.DOWNLOAD);
        AppSettings read = repository.read();
        assertEquals(DownloadType.VIDEO, read.getDownload().getDownloadType());
        assertEquals(AppTheme.DARK, read.getAppearance().getTheme());
    }

    @Test public void resetAllRestoresEveryDefault() {
        InMemoryKeyValueStore store = new InMemoryKeyValueStore();
        SettingsRepository repository = repo(store);
        repository.update(settings -> settings.withAppearance(
                settings.getAppearance().withTheme(AppTheme.DARK).withCompactList(true)));
        assertNotEquals(AppSettings.DEFAULTS, repository.read());
        repository.resetAll();
        assertEquals(AppSettings.DEFAULTS, repository.read());
    }

    @Test public void proxyPasswordUsesSecureStorageAndNeverAppearsInTheStoreOrExport() {
        InMemoryKeyValueStore store = new InMemoryKeyValueStore();
        InMemorySecureStore secure = new InMemorySecureStore();
        SettingsRepository repository = repo(store, secure);
        repository.update(settings -> settings.withNetwork(settings.getNetwork().withProxyUsername("alice")));
        repository.setProxyPassword("s3cret-pass");
        assertEquals("s3cret-pass", repository.proxyPassword());
        assertTrue(repository.read().getNetwork().getProxyPasswordSet());
        for (String value : store.snapshot().values()) assertFalse(value.contains("s3cret-pass"));
        String json = repository.exportToJson();
        assertFalse(json.contains("s3cret-pass"));
        assertFalse(json.contains("alice"));
    }

    @Test public void exportThenImportReproducesNonSensitiveSettings() {
        InMemoryKeyValueStore source = new InMemoryKeyValueStore();
        SettingsRepository sourceRepository = repo(source);
        sourceRepository.update(settings -> settings
                .withDownload(settings.getDownload().withVideoQuality(VideoQuality.P720))
                .withBehavior(settings.getBehavior().withMaxSimultaneousDownloads(4))
                .withAppearance(settings.getAppearance().withTheme(AppTheme.LIGHT).withLanguageTag("fr")));
        InMemoryKeyValueStore target = new InMemoryKeyValueStore();
        Object result = repo(target).importFromJson(sourceRepository.exportToJson());
        assertTrue(result instanceof SettingsRepository.ImportResult.Success);
        AppSettings imported = repo(target).read();
        assertEquals(VideoQuality.P720, imported.getDownload().getVideoQuality());
        assertEquals(4, imported.getBehavior().getMaxSimultaneousDownloads());
        assertEquals("fr", imported.getAppearance().getLanguageTag());
    }

    @Test public void importIgnoresUnknownKeysAndRejectsMalformedFiles() {
        SettingsRepository repository = repo(new InMemoryKeyValueStore());
        assertTrue(repository.importFromJson("{\"appearance.theme\": \"dark\", \"some.future.key\": \"value\"}")
                instanceof SettingsRepository.ImportResult.Success);
        assertEquals(AppTheme.DARK, repository.read().getAppearance().getTheme());
        assertTrue(repository.importFromJson("garbage, not json") instanceof SettingsRepository.ImportResult.Failure);
    }

    @Test public void booleanDependenciesPersistIndependently() {
        SettingsRepository repository = repo(new InMemoryKeyValueStore());
        repository.update(settings -> settings
                .withSubtitles(settings.getSubtitles().withDownloadSubtitles(true).withIncludeAllLanguages(true))
                .withBehavior(settings.getBehavior().withPreventDuplicates(false).withDuplicateDetection(DuplicateDetection.MEDIA_ID)));
        AppSettings read = repository.read();
        assertTrue(read.getSubtitles().getDownloadSubtitles());
        assertTrue(read.getSubtitles().getIncludeAllLanguages());
        assertFalse(read.getBehavior().getPreventDuplicates());
        assertEquals(DuplicateDetection.MEDIA_ID, read.getBehavior().getDuplicateDetection());
    }
}
