package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering

enum class SamordningAndreStatligeYtelserType {
    DAGPENGER_ARBEIDSSOKER_ORDINAER,
    DAGPENGER_PERMITTERING_ORDINAER,
    DAGPENGER_PERMITTERING_FISKEINDUSTRI,
    TILTAKSPENGER,
    TILTAKSPENGER_OG_BARNETILLEGG,
    INGENTING,
}

enum class SamordningAndreStatligeYtelserKilde {
    ARENA,
    DP_SAK,
    TPSAK,
}