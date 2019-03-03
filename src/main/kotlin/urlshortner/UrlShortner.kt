package urlshortner

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.html.respondHtml
import io.ktor.request.document
import io.ktor.request.host
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.html.*

fun main() {
    UrlDatabase.init()
    UrlDatabase.createUrl()

    // Start server using etty as backend
    val server = embeddedServer(Netty, host = "192.168.254.33") {
        routing {
            // On / just direct to main page
            get("/") {
                println(call.request.origin.remoteHost)
                call.respondHtml {
                    createLinkSite()
                }
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
        call.respondHtml { createLinkSite("Missing URL!") }
        return
    }
    // Ensure the url starts with http:// or https:// so redirects work correctly
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
        url = "http://$url"
    }
    // Check if custom url has already been created. If so redirect with error message
    if (customUrl != null && UrlDatabase.doesUrlExist(customUrl)) {
        call.respondHtml { createLinkSite("Custom url has already been chosen. Try again!") }
        return
    }
    // Create blank entry for url we're about to create
    val id = UrlDatabase.createUrl()
    // Create key for customUrl or if it's null the database id
    val key = customUrl ?: UrlEncoderUtil.encode(id)
    // Since we need the id to create the key we now have to update the database
    UrlDatabase.updateUrl(id, key, url)
    call.respondHtml { createLinkSite("Created url: ${call.request.host()}/$key") }
}

/**
 * Function to handle "unknown" or wildcard links
 */
suspend fun handleUnknownLink(call: ApplicationCall) {
    val uri = call.request.document()
    // Check if url exists. If not simply redirect to main page
    if (!UrlDatabase.doesUrlExist(uri)) {
        call.respondHtml { createLinkSite() }
        return
    }
    val url = UrlDatabase.getUrl(uri)
    if (url == null) {
        call.respondHtml { createLinkSite("Internal error fetching url!") }
        error("Null url found in database for key $uri")
    }
    call.respondRedirect(url.url)
}

/**
 * Create a plain website with input to create small url
 * @param error Used to display error message on main page. Leave null for none (default)
 */
fun HTML.createLinkSite(error: String? = null) {
    body {
        if (error != null) {
            p {
                +error
            }
        }

        form("/create_url", encType = FormEncType.multipartFormData, method = FormMethod.get) {
            acceptCharset = "utf-8"

            p {
                label { +"Enter URL: " }
                textInput { name = "url" }
                submitInput { value = "send" }
            }
            p {
                label { +"(Optional) Custom URL: " }
                textInput { name = "custom" }
            }
        }
    }
}