package com.stalkerhek.tv.util

/**
 * Percent-encodes a String for use in a URL path segment.
 * Encodes each UTF-8 byte so non-ASCII channel names (Arabic, Russian,
 * Chinese etc.) produce correct URLs that the HLS server can decode.
 * Unreserved chars (RFC 3986) are passed through unchanged.
 */
fun String.encodeUrl(): String {
    val bytes = toByteArray(Charsets.UTF_8)
    return buildString(bytes.size * 3) {
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            if (i in 0x41..0x5A || i in 0x61..0x7A || i in 0x30..0x39 ||
                i == 0x2D || i == 0x5F || i == 0x2E || i == 0x7E) {
                append(i.toChar())
            } else {
                append('%')
                append(i.toString(16).padStart(2, '0').uppercase())
            }
        }
    }
}
