package com.kira.kdownloader.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-process download progress bus so the UI can mirror what the
 * foreground-service notification shows, keyed by (url, format label).
 */
object DownloadEvents {

    enum class Phase { PREPARING, RUNNING, COMPLETED, FAILED }

    data class State(
        val phase: Phase,
        val percent: Int = -1,
        val title: String = "",
        val kind: String = "",
        val fileUri: String? = null,
        val message: String? = null,
        /** Estimated seconds remaining as reported by the engine, or -1 when unknown. */
        val etaSeconds: Long = -1,
        /** Engine process id of the running download, used to cancel it from the UI. */
        val processId: String? = null,
    )

    private val mutableStates = MutableStateFlow<Map<String, State>>(emptyMap())
    val states: StateFlow<Map<String, State>> = mutableStates.asStateFlow()

    fun keyOf(url: String, formatLabel: String): String = "$url|$formatLabel"

    fun update(key: String, state: State) {
        mutableStates.update { it + (key to state) }
    }

    fun clear(key: String) {
        mutableStates.update { it - key }
    }
}
