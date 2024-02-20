package no.nav.aap.httpclient.request

import java.time.Duration

interface Request {

    fun aditionalHeaders(): List<Pair<String, String>>

    fun timeout(): Duration
}