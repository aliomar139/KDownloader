package com.kira.kdownloader.settings

/**
 * Validates proxy configuration before it can be saved (Section 5, Section 14).
 *
 * Pure logic: no network access. The "Test connection" action is a separate runtime concern; this
 * only guarantees the stored fields are well-formed so an invalid host/port can never be persisted.
 */
object ProxyValidator {

    sealed interface Result {
        data object Valid : Result
        data class Invalid(val field: Field, val reason: String) : Result
    }

    enum class Field { HOST, PORT, USERNAME, PASSWORD }

    /**
     * Validates a full proxy config. When [type] is [ProxyType.DISABLED] the config is always valid
     * regardless of other fields (they are simply unused).
     */
    fun validate(
        type: ProxyType,
        host: String,
        port: Int,
        username: String,
        password: String,
    ): Result {
        if (type == ProxyType.DISABLED) return Result.Valid

        val trimmedHost = host.trim()
        if (trimmedHost.isEmpty()) {
            return Result.Invalid(Field.HOST, "Host is required")
        }
        if (!isValidHost(trimmedHost)) {
            return Result.Invalid(Field.HOST, "Enter a valid host name or IP address")
        }
        if (port !in 1..65535) {
            return Result.Invalid(Field.PORT, "Port must be between 1 and 65535")
        }
        // Credentials are optional, but if a password is set a username is expected.
        if (password.isNotEmpty() && username.isBlank()) {
            return Result.Invalid(Field.USERNAME, "Username is required when a password is set")
        }
        return Result.Valid
    }

    /**
     * Validates a host as a bracketed IPv6 literal, an IPv4 address, or an RFC 1123 hostname.
     * Implemented with plain character checks (no regex) so there is nothing that can fail at class
     * initialization on a device.
     */
    fun isValidHost(host: String): Boolean {
        val h = host.trim()
        if (h.isEmpty() || h.length > 253) return false
        if (h.startsWith("[") && h.endsWith("]")) return isIpv6(h.substring(1, h.length - 1))
        if (isIpv4(h)) return true
        return isHostname(h)
    }

    fun isValidPort(port: Int): Boolean = port in 1..65535

    private fun isIpv4(h: String): Boolean {
        val parts = h.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() && part.length <= 3 && part.all(Char::isDigit) &&
                (part.toIntOrNull()?.let { it in 0..255 } == true)
        }
    }

    private fun isIpv6(h: String): Boolean =
        h.isNotEmpty() && h.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == ':' }

    private fun isHostname(h: String): Boolean {
        if (h.endsWith(".")) return false
        val labels = h.split('.')
        // A fully-numeric final label means this is really a (malformed) IP address, not a host.
        if (labels.last().isNotEmpty() && labels.last().all(Char::isDigit)) return false
        return labels.all(::isLabel)
    }

    private fun isLabel(label: String): Boolean {
        if (label.isEmpty() || label.length > 63) return false
        if (label.startsWith("-") || label.endsWith("-")) return false
        return label.all { it.isLetterOrDigit() && it.code < 128 || it == '-' }
    }
}
