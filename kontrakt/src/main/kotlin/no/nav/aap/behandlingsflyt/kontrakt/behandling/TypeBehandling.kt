package no.nav.aap.behandlingsflyt.kontrakt.behandling

public enum class TypeBehandling(
    /** Kodene følger kodeverket Behandlingstyper.
     *
     * Se https://kodeverk.ansatt.nav.no/kodeverk/Behandlingstyper
     **/
    private var identifikator: String,
) {

    Førstegangsbehandling("ae0034"),
    Revurdering("ae0028"),
    Tilbakekreving(""),
    Klage("ae0058"),
    SvarFraAndreinstans("svar-fra-andreinstans"),  // TODO: Undersøk om vi bør opprette kode i kodeverk
    OppfølgingsBehandling("oppfølgingsbehandling");

    public fun identifikator(): String = identifikator

    public fun toLogString(): String = "${this.name}($identifikator)"

    public companion object {
        public fun from(identifikator: String): TypeBehandling {
            return entries.first { it.identifikator == identifikator }
        }
    }

    public fun erYtelsesbehandling(): Boolean {
        return this == Førstegangsbehandling || this == Revurdering
    }
}

