package no.nav.aap.behandlingsflyt.pip

class IdentPåSak(
    val ident: String,
    private val opprinnelse: Opprinnelse
) {
    enum class Opprinnelse {
        PERSON, BARN
    }

    companion object {
        fun Iterable<IdentPåSak>.filterDistinctIdent(opprinnelse: Opprinnelse): List<String> {
            return this.filter { it.opprinnelse == opprinnelse }.map(IdentPåSak::ident).distinct()
        }
    }
}