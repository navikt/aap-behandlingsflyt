package no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import java.time.Instant

data class SvarFraAndreinstansGrunnlag(
    val vurdering: SvarFraAndreinstansVurdering
)

data class SvarFraAndreinstansVurdering(
    val begrunnelse: String,
    val konsekvens: SvarFraAndreinstansKonsekvens,
    val vilkårSomOmgjøres: List<Hjemmel>,
    val vurdertAv: String,
    val opprettet: Instant? = null
)

enum class SvarFraAndreinstansKonsekvens {
    OMGJØRING,
    BEHANDLE_PÅ_NYTT,
    INGENTING
}