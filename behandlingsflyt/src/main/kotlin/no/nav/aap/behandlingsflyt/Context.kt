package no.nav.aap.behandlingsflyt

object Context : ThreadLocal<CallContext>()

class CallContext(
    val brukerIdent: String,
)