import DBHandler.DBConn
import Handler.BudgetTypeHandler
import Handler.StatementHandler
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.toMap
import kotlinx.serialization.json.Json

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
            println("Received request to upload a budget divider.")
            val params = call.receiveParameters().toMap()// consume once

            val name = params["name"]?.firstOrNull()
            val description = params["description"]?.firstOrNull()
            val maxBudget = params["max_budget"]?.firstOrNull()?.toIntOrNull()

            if (name == null || description == null) {
                println("Invalid request body.")
                call.respond(HttpStatusCode.BadRequest, "Invalid request body.")
                return@post
            }

            println(params)

            val response = BudgetTypeHandler().execute(name, description, maxBudget)

            if (true) {
                call.respondText("Budget divider uploaded successfully.")
            } else {
                call.respond(HttpStatusCode.BadRequest, "Failed to upload budget divider.")
            }
        }

//        get("/test") {
//            call.respond(HttpStatusCode.OK, getAllTransactions())
//        }
    }
}