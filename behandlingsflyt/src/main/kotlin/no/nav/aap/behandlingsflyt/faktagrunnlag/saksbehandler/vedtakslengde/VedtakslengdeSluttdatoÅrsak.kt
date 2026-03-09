package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde

enum class VedtakslengdeSluttdatoÅrsak {
    ETT_ÅR_VARIGHET,
    FORLENGELSE_ETT_ÅR,
    FORLENGELSE_TO_ÅR,

    // Unntaksvilkår utvidelse / innskrenkning
    ARBEIDSSØKER,
    SØKNAD_UFØRETRYGD,
    AVBRUTT_STUDIE,
    SYKEPENGEERSTATNING,

    // Avslagsårsaker
    ALDER,
    MELDEMSKAP,
    OPPHOLDSKRAV,
    INSTITUSJON,
    SAMORDNING_FOLKETRYGDEN,
    ORDINÆRKVOTE_BRUKT_OPP,
}
