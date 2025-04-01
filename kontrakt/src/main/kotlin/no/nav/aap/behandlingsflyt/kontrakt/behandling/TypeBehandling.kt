package no.nav.aap.behandlingsflyt.kontrakt.behandling

public enum class TypeBehandling(private var identifikator: String) {

    Førstegangsbehandling("ae0034"),
    Revurdering("ae0028"),
    Tilbakekreving(""),
    Klage("ae0058");

    public fun identifikator(): String = identifikator

    public fun toLogString(): String = "${this.name}($identifikator)"

    public companion object {
        public fun from(identifikator: String): TypeBehandling {
            return entries.first { it.identifikator == identifikator }
        }
    }
}

