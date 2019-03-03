package urlshortener.page

import kotlinx.html.*

/**
 * Create a plain website with input to create small url
 * @param error Used to display error message on main page. Leave null for none (default)
 */
object MainPage {
    fun create(html: HTML) {
        MainPage.create(html, null)
    }

    fun create(html: HTML, message: String?) {
        with(html) {
            head {
                link(rel = "stylesheet", href = "/style.css", type = "text/css")
            }
            body {
                div {
                    form("/create_url", encType = FormEncType.multipartFormData, method = FormMethod.get) {
                        acceptCharset = "utf-8"
                        // Error message (if exists)
                        if (!message.isNullOrEmpty()) {
                            p {
                                id = "custom"
                                +message
                            }
                        }

                        // URL field
                        p {
                            textInput {
                                name = "url"
                                id = "url"
                                placeholder = "Input URL"
                            }
                            submitInput {
                                id = "submit"
                                value = "Submit"
                            }
                        }

                        // Custom link field
                        p {
                            label { +"Custom URL" }
                            textInput {
                                name = "custom"
                                id = "custom"
                                placeholder = "(Optional)"
                            }
                        }
                    }
                }

                h1 {
                    id = "title"
                    +"KUrlShortener"
                }
            }
        }
    }
}