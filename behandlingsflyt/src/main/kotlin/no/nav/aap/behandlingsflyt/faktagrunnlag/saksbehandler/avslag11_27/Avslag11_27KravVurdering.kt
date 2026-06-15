package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.avslag11_27

enum class Avslag11_27KravVurdering {
    AVSLÅTT,
    GODKJENT;

    fun prioritertVerdi(other: Avslag11_27KravVurdering): Avslag11_27KravVurdering {
        if (this == GODKJENT || other == GODKJENT) {
            return GODKJENT
        }
        return AVSLÅTT;
    }
}