import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RegexInference {

    // List to store MRP options

    // Extracts MRP values from OCR text
    fun extractMRPFromText(ocrText: String): List<Double> {
        val mrpPattern = Regex(
            """(?:MRP\s*[:\-]?\s*(?:Rs\.?|₹|\u20ac)?\s*[\$\₹\-]?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{2})?)(?:/[\d\.]+)?|Rs\.?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{2})?)|₹\s*(\d{1,3}(?:,\d{3})*(?:\.\d{2})?)|(\d{1,3}(?:,\d{3})*(?:\.\d{2})?)\s*MRP|(\d{1,3}(?:,\d{3})(?:\.\d{2})?)\s*/-|\$(\d{1,3}(?:,\d{3})*(?:\.\d{2})?)|\u20ac\s*(\d{1,3}(?:,\d{3})*(?:\.\d{2})?)|\s(\d{1,3}(?:,\d{3})(?:\.\d{2})?)|(\d{2,4}\.00))""",
            RegexOption.IGNORE_CASE
        )

        val matches = mrpPattern.findAll(ocrText)
        val mrpValues = mutableListOf<Double>()

        for (match in matches) {
            for (i in 1 until match.groupValues.size) { // Adjusted loop to avoid out-of-bounds
                val value = match.groupValues[i]
                if (value.isNotEmpty()) {
                    val cleanedValue = value.replace(Regex("""[^\d.]"""), "").replace(",", "")
                    if (cleanedValue.isNotEmpty()) {
                        try {
                            val parsedValue = cleanedValue.toDouble()
                            if (parsedValue > 0 && parsedValue <= 10000) {
                                // Filter out unusually high or low values
                                mrpValues.add(parsedValue)
                            }
                        } catch (e: Exception) {
                            // Log or handle the error if needed
                        }
                    }
                }
            }
        }
        return mrpValues.toSet().toList().sorted()
    }

    // Extracts manufacturing and expiry dates from OCR text
    fun extractDatesFromText(ocrText: String): List<String> {
        val datePatterns = listOf(
            """\b(?:[0-9]|0[1-9]|[12][0-9]|3[01])\s*/\s*(?:0[1-9]|1[0-2])\s*/\s*(?:2[0-9]|20[2-3][0-9])\b""",
            """\b(?:0[1-9]|1[0-2])\s*/\s*(?:2[0-9]|20[2-3][0-9])\b""",
            """\b(?:[0-9]|0[1-9]|[12][0-9]|3[01])\s*-\s*(?:0[1-9]|1[0-2])\s*-\s*(?:2[0-9]|20[2-3][0-9])\b""",
            """\b(?:0[1-9]|1[0-2])\s*-\s*(?:2[0-9]|20[2-3][0-9])\b""",
            """\b(?:0[1-9]|[12][0-9]|3[01])\s*\.\s*(?:0[1-9]|1[0-2])\s*\.\s*(?:2[0-9]|20[2-3][0-9])\b""",
            """\b(?:0[1-9]|[12][0-9]|3[01])\s*(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s*(?:2[0-9]|20[2-3][0-9])\b""",
            """\b(?:0[1-9]|[12][0-9]|3[01])\s*(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s*\d{2}\b""",
            """\b(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s*(?:[0-9]|[1-2][0-9]|3[01])\s*\d{2}\b""",
            """\b(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\s*[0-9]{2}\b"""
        )

        val dates = mutableListOf<String>()
        for (pattern in datePatterns) {
            val regExp = Regex(pattern, RegexOption.IGNORE_CASE)
            val matches = regExp.findAll(ocrText)
            for (match in matches) {
                dates.add(match.value)
            }
        }
        return dates
    }

    // Parses manufacturing and expiry dates from a list of date strings
    fun getMfgExpiryFromDate(dates: List<String>): List<String> {
        val dateFormats = listOf(
            "dd/MM/yy", "dd/MM/yyyy", "MM/yyyy", "MM/yy", "MMM/yy", "MMM/yyyy",
            "MMMM yyyy", "MMMM yy", "dd MMM yy", "dd MMM yyyy", "MM-yyyy",
            "MM-yy", "dd MMMM yyyy", "dd-MM-yyyy", "dd-MM-yy", "dd.MM.yy",
            "dd.MM.yyyy", "yyyy", "yy", "dd MM yyyy", "dd MM yy", "dd-MMM-yy",
            "dd-MMM-yyyy", "MMM.yy", "MMM.yyyy", "MMM yy", "MMM yyyy", "MMM-yy", "MMM-yyyy"
        )

        val parsedDates = mutableListOf<Date>()
        val originalDates = mutableListOf<String>()

        for (dateStr in dates) {
            for (fmt in dateFormats) {
                try {
                    val parsedDate = SimpleDateFormat(fmt, Locale.ENGLISH).parse(dateStr)
                    parsedDates.add(parsedDate)
                    originalDates.add(dateStr)
                    break
                } catch (e: Exception) {
                    // Ignore parsing exceptions
                }
            }
        }

        if (parsedDates.isEmpty()) return emptyList()

        val minDate = parsedDates.minOrNull()!!
        val maxDate = parsedDates.maxOrNull()!!
        val minDateStr = originalDates[parsedDates.indexOf(minDate)]
        val maxDateStr = originalDates[parsedDates.indexOf(maxDate)]

        return listOf(minDateStr, maxDateStr)
    }

    // Parse MRP options from a string
    private fun _parseMrpOptions(formattedMrpText: String): List<String>? {
        val regex = Regex("""\[(.*?)\]""")
        val match = regex.find(formattedMrpText)
        match?.let {
            val valuesString = it.groups[1]?.value
            return valuesString?.split(",")?.map { s -> s.trim() }
        }
        return null
    }

    // Main processing function
    fun processText(text: String): Map<String, Any?> {
        val mrpResult = extractMRPFromText(text)
        val mfgExpiryResult = extractDatesFromText(text)
        val parsedDates = getMfgExpiryFromDate(mfgExpiryResult)

        // Convert MRP result to string for _parseMrpOptions if needed
        val mrpText = if (mrpResult.isNotEmpty()) mrpResult.joinToString(", ") { "'$it'" } else ""

        // Wrap in brackets if mrpText is not empty
        val formattedMrpText = if (mrpText.isNotEmpty()) "[$mrpText]" else "[]"

        // Pass the formatted text to _parseMrpOptions
        val mrpOptions = _parseMrpOptions(formattedMrpText)

        return mapOf(
            "mrp" to mrpResult,
            "manufacturingDate" to if (parsedDates.isNotEmpty()) parsedDates[0] else null,
            "expiryDate" to if (parsedDates.size > 1) parsedDates[1] else null,
            "mrpOptions" to mrpOptions
        )
    }

}
