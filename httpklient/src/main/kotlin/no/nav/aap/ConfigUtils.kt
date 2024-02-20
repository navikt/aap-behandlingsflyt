package no.nav.aap

fun requiredConfigForKey(key: String): String {
    var property = System.getProperty(key)
    if (property != null) {
        return property
    }
    property = System.getProperty(key.lowercase().replace("_", "."))
    if (property != null) {
        return property
    }
    property = System.getenv(key)
    if (property != null) {
        return property
    }
    throw IllegalStateException("Mangler påkrevd config verdi $key")
}

fun configForKey(key: String): String? {
    var property = System.getProperty(key)
    if (property != null) {
        return property
    }
    property = System.getProperty(key.lowercase().replace("_", "."))
    if (property != null) {
        return property
    }
    return System.getenv(key)
}