package no.nav.aap.behandlingsflyt.kontrakt.behandling

enum class TypeBehandling(private var identifikator: String) {

    Førstegangsbehandling("ae0034"),
    Revurdering("ae0028"),
    Tilbakekreving(""),
    Klage("");

    fun identifikator(): String = identifikator

    companion object {
        fun from(identifikator: String): TypeBehandling {
            return entries.first { it.identifikator == identifikator }
        }
    }
}

