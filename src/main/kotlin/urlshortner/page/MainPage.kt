package urlshortner.page

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
            body {
                if (message != null) {
                    p {
                        +message
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
    }
}