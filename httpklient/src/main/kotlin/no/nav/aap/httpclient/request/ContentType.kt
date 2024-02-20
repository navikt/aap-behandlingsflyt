package no.nav.aap.httpclient.request

enum class ContentType(val string: String) {
    APPLICATION_JSON("application/json"),
    APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded");

    override fun toString(): String {
        return string
    }
}