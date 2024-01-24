package no.nav.aap.verdityper.sakogbehandling

enum class TypeBehandling(private var identifikator: String) {

    Førstegangsbehandling("ae0034"),
    Revurdering("ae0028"),
    Tilbakekreving(""),
    Klage(""),
    Anke("");

    fun identifikator(): String = identifikator

    companion object {
        fun from(identifikator: String): TypeBehandling {
            return entries.first { it.identifikator.equals(identifikator) }
        }
    }
}

