package no.nav.aap.json

import java.io.IOException

class DeserializationException(exception: IOException) : RuntimeException(exception)