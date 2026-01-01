package com.kleos.education.ui.common

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.util.Locale

/**
 * Formats phone number as +CC-(xxx)-xxx-xx-xx and enforces country code prefix.
 * Country code is chosen by current language:
 *  - ru -> +7
 *  - en -> +1
 *  - zh -> +86
 * Otherwise leaves as is (no forced prefix).
 */
class PhoneMaskTextWatcher(
    private val editText: EditText,
    languageCode: String
) : TextWatcher {

    private val countryPrefix: String = when (languageCode.lowercase(Locale.ROOT)) {
        "ru" -> "+7"
        "en" -> "+1"
        "zh" -> "+86"
        else -> "+7"
    }

    private var isUpdating = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(s: Editable?) {
        if (isUpdating) return
        val raw = s?.toString().orEmpty()
        val formatted = applyMask(raw)
        if (formatted != raw) {
            isUpdating = true
            val cursor = formatted.length
            editText.setText(formatted)
            editText.setSelection(cursor.coerceAtMost(formatted.length))
            isUpdating = false
        }
    }

    private fun applyMask(input: String): String {
        // Remove everything except digits
        val digits = StringBuilder()
        for (ch in input) {
            if (ch.isDigit()) digits.append(ch)
        }
        // Ensure starts with our country digits
        val ccDigits = countryPrefix.filter { it.isDigit() }
        var normalized = digits.toString()
        if (!normalized.startsWith(ccDigits)) {
            // Strip leading zeros and re-apply prefix
            normalized = normalized.trimStart('0')
            normalized = ccDigits + normalized
        }
        // Build: +CC-(xxx)-xxx-xx-xx
        val sb = StringBuilder()
        sb.append(countryPrefix)
        sb.append('-')
        var idx = ccDigits.length
        fun take(n: Int): String {
            val end = (idx + n).coerceAtMost(normalized.length)
            val part = if (idx < end) normalized.substring(idx, end) else ""
            idx = end
            return part
        }
        val p1 = take(3)
        if (p1.isNotEmpty()) {
            sb.append('(').append(p1)
            if (p1.length == 3) sb.append(')') else return sb.toString()
        } else return sb.toString()
        val p2 = take(3)
        if (p2.isNotEmpty()) {
            sb.append('-').append(p2)
            if (p2.length < 3) return sb.toString()
        } else return sb.toString()
        val p3 = take(2)
        if (p3.isNotEmpty()) {
            sb.append('-').append(p3)
            if (p3.length < 2) return sb.toString()
        } else return sb.toString()
        val p4 = take(2)
        if (p4.isNotEmpty()) {
            sb.append('-').append(p4)
        }
        return sb.toString()
    }
}



