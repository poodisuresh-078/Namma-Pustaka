package com.example.nammapustaka.data.local

enum class BookCategory(val storageValue: String, val labelEn: String, val labelKn: String) {
    STORY("Story", "Story", "ಕಥೆ"),
    SCIENCE("Science", "Science", "ವಿಜ್ಞಾನ"),
    HISTORY("History", "History", "ಇತಿಹಾಸ"),
    OTHERS("Others", "Others", "ಇತರೆ");

    companion object {
        fun fromStorage(value: String): BookCategory =
            entries.find { it.storageValue == value } ?: OTHERS

        val filterChips: List<Pair<String?, String>> =
            listOf(null to "All") + entries.map { it.storageValue to it.labelEn }
    }
}
