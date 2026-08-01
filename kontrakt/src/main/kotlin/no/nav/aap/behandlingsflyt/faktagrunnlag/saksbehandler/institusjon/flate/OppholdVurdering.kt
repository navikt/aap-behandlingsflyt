package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

enum class OppholdVurdering {
    AVSLÅTT,
    GODKJENT,
    UAVKLART;

    fun prioritertVerdi(other: OppholdVurdering): OppholdVurdering {
        if (this == AVSLÅTT || other == AVSLÅTT) {
            return AVSLÅTT
        }
        if (this == GODKJENT || other == GODKJENT) {
            return GODKJENT
        }
        return UAVKLART
    }
}