import DBHandler.DBConn
import Handler.StatementHandler
import Storage.Transactions
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import Storage.Transactions.TransactionRepository.getAllTransactions
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.File

fun main() {
    DBConn().connect(false)
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }


    routing {
        post("/upload_statement") {
            try {
                val multipart = call.receiveMultipart()
                val response = StatementHandler().execute(multipart)

                if (response) {
                    call.respondText("File uploaded successfully.")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "No file was included in the upload.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, "Failed to upload file: ${e.message}")
            }
        }

        post("/upload_divider") {
            val name = call.receiveParameters()["name"]
            val description = call.receiveParameters()["description"]
        }

        get("/test") {
            call.respond(HttpStatusCode.OK, getAllTransactions())
        }
    }
}
