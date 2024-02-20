package no.nav.aap.json

import java.io.IOException

class SerializationException(exception: IOException) : RuntimeException(exception)