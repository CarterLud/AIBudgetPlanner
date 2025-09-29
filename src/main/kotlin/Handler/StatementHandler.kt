package Handler

import sql.Transaction
import sql.Transactions.TransactionRepository.persist
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.time.LocalDate

class StatementHandler {

    /**
     * Regex for finding card numbers in a statement
     *
     * Can get it through ["CardNumber"]
     */
    val cardRegex = Regex("""(?<CardNumber>\d{4} \d{2}XX XXXX \d{4})""")

    /**
     * Regex for finding transactions in a statement
     *
     * Can Get
     * ["startMonth"], ["startDay"]
     * ["endMonth"], ["endDay"]
     * ["amount"]
     * ["vendor"]
     */
    val transactionRegex = Regex(
        """^(?<startMonth>[A-Z]{3}) (?<startDay>\d{1,2}) (?<endMonth>[A-Z]{3}) (?<endDay>\d{1,2}) (?<amount>-?\$\d+\.\d{2})(?<vendor>.+)$"""
    )

    /**
     * A regular expression used to extract the start and end dates of a statement period from a bank statement.
     *
     * The pattern matches strings in the format:
     * "STATEMENT PERIOD: <Start Month> <Start Day>, <Start Year> to <End Month> <End Day>, <End Year>".
     *
     * Capturing groups:
     * - `startmonth: The month the statement period starts (e.g., January).
     * - `startday`: The day of the month the statement period starts (1-31).
     * - `startyear`: The year the statement period starts (4-digit format).
     * - `endmonth`: The month the statement period ends.
     * - `endday`: The day of the month the statement period ends.
     * - `endyear`: The year the statement period ends (4-digit format).
     */
    val statementRangeRegex = Regex("""STATEMENT PERIOD:\s+(?<startmonth>[A-Za-z]+)\s+(?<startday>\d{1,2}),\s+(?<startyear>\d{4})\s+to\s+(?<endmonth>[A-Za-z]+)\s+(?<endday>\d{1,2}),\s+(?<endyear>\d{4})""")
    
    /**
     * Extracts plain text content from a PDF document.
     *
     * This function converts a PDF file (provided as a byte array) into a string containing
     * all the text content from the document. It uses an Apache PDFBox library to parse the
     * PDF and extract the text.
     *
     * @param bytes The raw bytes of the PDF document to process
     * @return A string containing all the extracted text content from the PDF
     *
     * Note: The function automatically handles resource cleanup by using Kotlin's 'use' extension
     * functions on both the input stream and the PDF document.
     */
    fun extractTextFromPdf(bytes: ByteArray): String {
        ByteArrayInputStream(bytes).use { input ->
            PDDocument.load(input).use { document ->
                return PDFTextStripper().getText(document)
            }
        }
    }

    /**
     * Extracts the start and end dates from a bank statement.
     */
    fun extractStatementRange(statement: String): Map<String, String> {
        val match = statementRangeRegex.find(statement)
        return match?.groups?.mapNotNull { it?.let { it.value to it.value } }?.toMap() ?: emptyMap()
    }

    /**
     * Parses a bank statement text and organizes it by credit card numbers.
     *
     * This function takes the full text content of a bank statement and divides it into
     * sections based on credit card numbers identified using the [cardRegex] pattern.
     * All lines following a detected card number are associated with that card until
     * another card number is encountered.
     *
     * @param statement The complete text content of a bank statement as a single string
     * @return A map where keys are credit card numbers (in format "XXXX XXXX XXXX XXXX")
     *         and values are lists of strings representing all lines of text associated
     *         with that card in the statement
     *
     * Note: Lines that appear before the first card number in the statement are not
     * included in the returned map.
     */
    fun splitByCardNumbers(statement: String): Map<String, List<String>> {
        val lines = statement.lines()
        val cardData = mutableMapOf<String, MutableList<String>>()

        var currentCard: String? = null

        for (line in lines) {
            if (cardRegex.matches(line.trim())) {
                // Found a new card number
                currentCard = line.trim()
                cardData.putIfAbsent(currentCard, mutableListOf())
            } else if (currentCard != null) {
                // Belongs to the current card
                cardData[currentCard]!!.add(line)
            }
        }
        return cardData
    }

    /**
     * Processes card transaction data by parsing individual transactions from the text lines.
     *
     * This function takes a map where each key is a credit card number and the value is a list of
     * text lines containing transaction information. It parses these lines using regex pattern matching
     * to extract transaction details such as date, amount, and vendor information.
     *
     * @param cardTransactions A map where keys are credit card numbers and values are lists of 
     *                         text lines containing transaction information
     * @return A map where keys are credit card numbers and values are lists of transaction maps. 
     *         Each transaction map contains keys like "startMonth", "amount", and "vendor" 
     *         with their corresponding values extracted from the transaction text.
     *
     * Example transaction format in input text: "JAN 15 JAN 15-$24.99AMAZON PRIME"
     */
    fun splitByTransactions(cardTransactions: Map<String, List<String>>, start: LocalDate, end: LocalDate): Int {
        cardTransactions.forEach { (cardNumber, lines) ->
            lines.forEach { line ->
                transactionRegex.findAll(line).forEach { matchResult ->
                    // Get the named capture groups directly
                    val cardNumber = cardNumber.replace(" ", "").takeLast(4)
                    val startMonth = matchResult.groups["startMonth"]?.value ?: ""
                    val startDay = matchResult.groups["startDay"]?.value ?: ""
                    val endMonth = matchResult.groups["endMonth"]?.value ?: ""
                    val endDay = matchResult.groups["endDay"]?.value ?: ""
                    val amount = matchResult.groups["amount"]?.value ?: ""
                    val vendor = matchResult.groups["vendor"]?.value ?: ""

                    val startYear = findYear(startDay.toInt(), monthAbbreviationToNumber(startMonth)!!, start, end)
                    val endYear = findYear(endDay.toInt(), monthAbbreviationToNumber(endMonth)!!, start, end)
                    val start = LocalDate.of(startYear, monthAbbreviationToNumber(startMonth)!!, startDay.toInt())
                    val end = LocalDate.of(endYear, monthAbbreviationToNumber(endMonth)!!, endDay.toInt())

                    val transaction = Transaction(
                        0,
                        cardNumber,
                        start,
                        end,
                        amount,
                        vendor
                    )

                    persist(transaction)
                }
            }
        }
        return 0
    }

    /**
     * Processes a multipart form submission containing a PDF bank statement.
     *
     * This suspends function handles the upload of a PDF bank statement, extracts its content,
     * and processes the data to identify card numbers and their associated transactions.
     * It expects a MultiPartData object containing a file part named "file".
     *
     * The function:
     * 1. Extracts the uploaded PDF file
     * 2. Converts the PDF to text
     * 3. Organizes the text by card numbers
     * 4. Parses individual transactions
     *
     * @param statement The MultiPartData object containing the uploaded PDF file
     * @return Boolean indicating whether the processing was successful (true if a file was uploaded)
     *
     * Note: This function properly disposes of all multipart data parts after processing
     * to prevent resource leaks.
     */
    suspend fun execute(statement: MultiPartData): Boolean {
        var uploadedFileName = ""
        var statementRange = emptyMap<String, String>()
        var pdfText = emptyMap<String, List<String>>()

        statement.forEachPart { part ->
            if (part is PartData.FileItem && part.name == "file") {
                val name = part.originalFileName ?: throw FileNotFoundException("File name not found")
                val bytes = part.streamProvider().readBytes()
                statementRange = extractStatementRange(extractTextFromPdf(bytes))
                pdfText = splitByCardNumbers(extractTextFromPdf(bytes))
                uploadedFileName = name
            }
            part.dispose()
        }

        val start = LocalDate.parse("${statementRange["startyear"]}-${statementRange["startmonth"]}-${statementRange["startday"]}")
        val end = LocalDate.parse("${statementRange["endyear"]}-${statementRange["endmonth"]}-${statementRange["endday"]}")
        val transactionData = splitByTransactions(pdfText, start, end)

        if (uploadedFileName.isNotEmpty()) {
            return true
        }
        return false
    }

    /**
     * Converts a month abbreviation to its corresponding month number.
     *
     * This function takes a month abbreviation (e.g., "Jan") and converts it to its corresponding
     * month number (e.g., 1). It is used to parse the start and end dates of a bank statement
     * period.
     *
     * @param month The month abbreviation as a string
     * @return The corresponding month number as an integer, or null if the input is invalid
     */
    fun monthAbbreviationToNumber(month: String): Int? {
        val months = mapOf(
            "Jan" to 1,
            "Feb" to 2,
            "Mar" to 3,
            "Apr" to 4,
            "May" to 5,
            "Jun" to 6,
            "Jul" to 7,
            "Aug" to 8,
            "Sep" to 9,
            "Oct" to 10,
            "Nov" to 11,
            "Dec" to 12
        )

        return months[month.trim().replaceFirstChar { it.uppercase() }]
    }

    /**
     * Finds the year for a given date in a statement period.
     *
     * This function takes a date (represented as day, month, and year) and the start and end dates
     * of a bank statement period and determines the year for the given date. It uses the logic
     * described in the [findYear] function to determine the year.
     *
     * @param day The day of the month for the given date
     * @param month The month number for the given date
     * @param statementStart The start date of the bank statement period
     **/
    fun findYear(day: Int, month: Int, statementStart: LocalDate, statementEnd: LocalDate): Int {
        if (statementEnd < statementStart) { throw IllegalArgumentException("End date cannot be before start date") }

        if (statementStart.year == statementEnd.year) {
            return statementStart.year
        }

        val yearChange = LocalDate.of(statementEnd.year, 1, 1)

        return if (day < yearChange.dayOfMonth && month < yearChange.monthValue) {
            statementStart.year
        } else {
            statementEnd.year
        }
    }

}