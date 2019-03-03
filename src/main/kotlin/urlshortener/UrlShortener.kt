package urlshortener

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.document
import io.ktor.request.host
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.html.*
import urlshortener.page.MainPage
import java.io.BufferedReader
import java.io.InputStreamReader

fun main() {
    UrlDatabase.init()
    UrlDatabase.createUrl()

    // Start server using etty as backend
    val server = embeddedServer(Netty) {
        routing {
            // On / just direct to main page
            get("/") {
                println(call.request.origin.remoteHost)
                call.respondHtml(MainPage::create)
            }
            get("/style.css") {
                val reader = BufferedReader(InputStreamReader(ClassLoader.getSystemClassLoader().getResourceAsStream("style.css")))
                val css = reader.readText()
                reader.close()
                call.respondText(css, ContentType.Text.CSS)
            }
            // create_url is called when form is complete to create new small url
            get("/create_url") {
                createUrl(call)
            }
            // Finally if no other page is found we check for a wildcard and see if the page has a url created for it
            get("*") {
                handleUnknownLink(call)
            }
        }
    }
    server.start(wait = true)
}

/**
 * Function to create new url
 * Redirects to main page
 */
suspend fun createUrl(call: ApplicationCall) {
    var url = call.parameters["url"]
    val customUrl = call.parameters["custom"]?.let { if (it.isBlank()) null else it }
    // Check if url is null or blank. If it is redirect to main page with error "Missing URL!"
    if (url.isNullOrBlank()) {
        call.respondHtml {
            MainPage.create(this, "Missing URL!")
        }
        return
    }
    // Ensure the url starts with http:// or https:// so redirects work correctly
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
        url = "http://$url"
    }
    // Check if custom url has already been created. If so redirect with error message
    if (customUrl != null && UrlDatabase.doesUrlExist(customUrl)) {
        call.respondHtml {
            MainPage.create(this, "Custom url '$customUrl' already exists! Try something else!")
        }
        return
    }
    // Create blank entry for url we're about to create
    val id = UrlDatabase.createUrl()
    // Create key for customUrl or if it's null the database id
    val key = customUrl ?: UrlEncoderUtil.encode(id)
    // Since we need the id to create the key we now have to update the database
    UrlDatabase.updateUrl(id, key, url)
    call.respondHtml {
        MainPage.create(this, "Created url: ${call.request.host()}/$key")
    }
}

/**
 * Function to handle "unknown" or wildcard links
 */
suspend fun handleUnknownLink(call: ApplicationCall) {
    val uri = call.request.document()
    // Check if url exists. If not simply redirect to main page
    if (!UrlDatabase.doesUrlExist(uri)) {
        call.respondHtml {
            MainPage.create(this)
        }
        return
    }
    val url = UrlDatabase.getUrl(uri)
    if (url == null) {
        call.respondHtml {
            MainPage.create(this, "Internal error fetching url!")
        }
        error("Null url found in database for key $uri")
    }
    call.respondRedirect(url.url)
}

/**
 * Simple extension function to make it easier to send reference to function block without including [HttpStatusCode]
 */
suspend fun ApplicationCall.respondHtml(block: HTML.() -> Unit) {
    this.respondHtml(HttpStatusCode.OK, block)
}