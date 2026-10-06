package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

enum class SykdomsvurderingValideringsfeil {
    MANGLER_NEDSATT_ARBEIDSEVNE,
    NEDSATT_ARBEIDSEVNE_STEMMER_IKKE_MED_50_PROSENT,
    NEDSATT_ARBEIDSEVNE_STEMMER_IKKE_MED_VESENTLIGHET,
    MANGLER_VURDERING_AV_YRKESSKADEGRENSE,
}
