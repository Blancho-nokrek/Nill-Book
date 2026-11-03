package com.example.data

object PhoneUtils {
    val COUNTRIES = listOf(
        Country("BD", "Bangladesh (+880)", "+880", "🇧🇩"),
        Country("IN", "India (+91)", "+91", "🇮🇳"),
        Country("PK", "Pakistan (+92)", "+92", "🇵🇰"),
        Country("SA", "Saudi Arabia (+966)", "+966", "🇸🇦"),
        Country("AE", "UAE (+971)", "+971", "🇦🇪"),
        Country("QA", "Qatar (+974)", "+974", "🇶🇦"),
        Country("KW", "Kuwait (+965)", "+965", "🇰🇼"),
        Country("OM", "Oman (+968)", "+968", "🇴🇲"),
        Country("MY", "Malaysia (+60)", "+60", "🇲🇾"),
        Country("SG", "Singapore (+65)", "+65", "🇸🇬"),
        Country("NP", "Nepal (+977)", "+977", "🇳🇵"),
        Country("LK", "Sri Lanka (+94)", "+94", "🇱🇰"),
        Country("GB", "United Kingdom (+44)", "+44", "🇬🇧"),
        Country("US", "United States (+1)", "+1", "🇺🇸"),
        Country("CA", "Canada (+1)", "+1", "🇨🇦"),
        Country("AU", "Australia (+61)", "+61", "🇦🇺"),
        Country("IT", "Italy (+39)", "+39", "🇮🇹")
    )

    val MONTHS = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun toE164(input: String, countryCode: String = "BD"): String? {
        val raw = input.trim()
        if (raw.isEmpty()) return null

        val country = COUNTRIES.find { it.code.equals(countryCode, ignoreCase = true) } ?: COUNTRIES.first()

        // Clean digits
        val digitsOnly = raw.replace(Regex("[^0-9+]"), "")
        if (digitsOnly.startsWith("+")) {
            return if (digitsOnly.length >= 7) digitsOnly else null
        }

        // If local format, strip leading 0 and attach dial code
        val national = digitsOnly.trimStart('0')
        if (national.length < 5) return null
        return "${country.dialCode}$national"
    }

    // E164 masking: "+8801712345678" -> "+880 ••••••••78"
    fun maskNumber(e164: String): String {
        if (!e164.startsWith("+")) return "••••••••"
        // Find matching dial code
        val matchingCountry = COUNTRIES.find { e164.startsWith(it.dialCode) }
        val prefix = matchingCountry?.dialCode ?: e164.take(4)
        val rest = e164.removePrefix(prefix)
        if (rest.length <= 2) return "$prefix ••$rest"
        val lastTwo = rest.takeLast(2)
        val hiddenCount = (rest.length - 2).coerceAtLeast(0)
        val dots = "•".repeat(hiddenCount)
        return "$prefix $dots$lastTwo"
    }

    fun formatBirthday(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "Not provided"
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val monthIdx = parts[1].toIntOrNull()?.minus(1) ?: return dateStr
            val day = parts[2].toIntOrNull() ?: return dateStr
            if (monthIdx in MONTHS.indices) {
                return "$day ${MONTHS[monthIdx]} $year"
            }
        }
        return dateStr
    }

    data class ParsedVCard(
        val name: String,
        val phone: String,
        val email: String = "",
        val address: String = "",
        val birthday: String = ""
    )

    // Ported from parseVcf in ContactsPage
    fun parseVcf(text: String, defaultCountry: String = "BD"): List<ParsedVCard> {
        val unfolded = text.replace(Regex("\r?\n[ \t]"), "")
        val cards = unfolded.split(Regex("BEGIN:VCARD", RegexOption.IGNORE_CASE)).drop(1)
        val out = mutableListOf<ParsedVCard>()

        for (card in cards) {
            var name = ""
            var email = ""
            var address = ""
            var birthday = ""
            val phones = mutableListOf<String>()

            for (line in card.lines()) {
                val idx = line.indexOf(":")
                if (idx < 0) continue
                val key = line.substring(0, idx).split(";")[0].uppercase()
                val value = line.substring(idx + 1).trim()
                if (value.isEmpty()) continue

                when {
                    key == "FN" && name.isEmpty() -> name = value
                    key == "N" && name.isEmpty() -> {
                        val p = value.split(";")
                        val first = p.getOrNull(1) ?: ""
                        val last = p.getOrNull(0) ?: ""
                        name = listOf(first, last).filter { it.isNotBlank() }.joinToString(" ").trim()
                    }
                    key == "TEL" -> phones.add(value)
                    key == "EMAIL" && email.isEmpty() -> email = value
                    key == "ADR" && address.isEmpty() -> {
                        address = value.split(";").filter { it.isNotBlank() }.joinToString(", ")
                    }
                    key == "BDAY" && birthday.isEmpty() -> {
                        val match = Regex("(\\d{4})-?(\\d{2})-?(\\d{2})").find(value)
                        if (match != null) {
                            val (y, m, d) = match.destructured
                            birthday = "$y-$m-$d"
                        }
                    }
                }
            }

            if (name.isNotEmpty()) {
                for (phone in phones) {
                    val normalized = toE164(phone, defaultCountry) ?: phone
                    out.add(ParsedVCard(name, normalized, email, address, birthday))
                }
            }
        }
        return out
    }
}
