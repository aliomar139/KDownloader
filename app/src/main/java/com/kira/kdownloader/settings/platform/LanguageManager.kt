package com.kira.kdownloader.settings.platform

import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Applies the app's language preference (Section 8).
 *
 * On Android 13+ the per-app language is delegated to the system [LocaleManager] so it is honoured
 * consistently and shows up in system settings. On older versions the stored tag is applied by
 * wrapping the base context's configuration in `attachBaseContext`.
 */
object LanguageManager {

    /** An empty tag means "Follow system". */
    data class Language(val tag: String, val display: String)

    val SUPPORTED = listOf(
        Language("", "Follow system"),
        Language("en", "English"),
        Language("es", "Español"),
        Language("fr", "Français"),
        Language("de", "Deutsch"),
        Language("pt", "Português"),
        Language("ru", "Русский"),
        Language("ar", "العربية"),
        Language("hi", "हिन्दी"),
        Language("id", "Bahasa Indonesia"),
        Language("ja", "日本語"),
        Language("zh", "中文"),
    )

    fun displayName(tag: String): String =
        SUPPORTED.firstOrNull { it.tag == tag }?.display
            ?: tag.takeIf { it.isNotBlank() }?.let { Locale.forLanguageTag(it).displayName }
            ?: "Follow system"

    /** Applies the language at runtime. On <33 the caller must also recreate the activity. */
    fun apply(context: Context, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val lm = context.getSystemService(LocaleManager::class.java)
            lm?.applicationLocales = if (tag.isBlank()) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(tag)
            }
        }
        // On <33 nothing to do here; [wrap] applies the locale to newly created contexts.
    }

    /** Wraps [base] with the configured locale. Used from `attachBaseContext` on API < 33. */
    fun wrap(base: Context, tag: String): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || tag.isBlank()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return ContextWrapper(base.createConfigurationContext(config))
    }
}
