package com.example.data

import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object FuzzyMatcher {
    const val MIN_MATCH = 0.9
    const val MIN_WORD_LENGTH = 3

    fun normalize(s: String): String {
        return Normalizer.normalize(s, Normalizer.Form.NFC)
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun levenshtein(a: List<Char>, b: List<Char>): Int {
        if (a.isEmpty()) return b.size
        if (b.isEmpty()) return a.size
        var prev = (0..b.size).toList()
        for (i in 1..a.size) {
            val cur = mutableListOf(i)
            for (j in 1..b.size) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur.add(min(min(prev[j] + 1, cur[j - 1] + 1), prev[j - 1] + cost))
            }
            prev = cur
        }
        return prev[b.size]
    }

    private fun rawSimilarity(a: List<Char>, b: List<Char>): Double {
        val maxLen = max(a.size, b.size)
        if (maxLen == 0) return 0.0
        return 1.0 - (levenshtein(a, b).toDouble() / maxLen.toDouble())
    }

    private fun wordsMatch(a: List<Char>, b: List<Char>): Boolean {
        val maxLen = max(a.size, b.size)
        if (abs(a.size - b.size).toDouble() / maxLen.toDouble() > (1.0 - MIN_MATCH)) return false
        return rawSimilarity(a, b) >= (MIN_MATCH - 1e-9)
    }

    // Returns > 0 if match, 0 if not a match
    fun matchScore(query: String, name: String): Double {
        val qWords = normalize(query)
            .split(" ")
            .filter { it.length >= MIN_WORD_LENGTH }
            .map { it.toList() }
        if (qWords.isEmpty()) return 0.0

        val nName = normalize(name)
        val nWords = nName
            .split(" ")
            .filter { it.isNotEmpty() }
            .map { it.toList() }
        if (nWords.isEmpty()) return 0.0

        var hits = 0
        for (qw in qWords) {
            if (nWords.any { nw -> wordsMatch(qw, nw) }) {
                hits++
            }
        }
        if (hits == 0) return 0.0

        val whole = rawSimilarity(normalize(query).toList(), nName.toList())
        return hits.toDouble() + whole
    }
}
