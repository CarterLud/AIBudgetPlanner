package Handler

import Storage.Transaction
import Storage.Transactions.TransactionRepository.addTransaction
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException

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
    fun splitByTransactions(cardTransactions: Map<String, List<String>>): Int {
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

                    val transaction = Transaction(
                        cardNumber,
                        startMonth,
                        startDay,
                        endMonth,
                        endDay,
                        amount,
                        vendor
                    )

                    addTransaction(transaction)
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
        var pdfText = emptyMap<String, List<String>>()

        statement.forEachPart { part ->
            if (part is PartData.FileItem && part.name == "file") {
                val name = part.originalFileName ?: throw FileNotFoundException("File name not found")
                val bytes = part.streamProvider().readBytes()
                pdfText = splitByCardNumbers(extractTextFromPdf(bytes))
                uploadedFileName = name
            }
            part.dispose()
        }

        val transactionData = splitByTransactions(pdfText)

        if (uploadedFileName.isNotEmpty()) {
            return true
        }
        return false
    }
}