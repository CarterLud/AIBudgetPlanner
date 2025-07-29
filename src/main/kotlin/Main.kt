import Handler.StatementHandler
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.File

fun main() {
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
        get("/") {
            call.respondText("✅ Server is running")
        }

        post("/upload") {
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

    }
}
