package no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant

data class SvarFraAndreinstansGrunnlag(
    val vurdering: SvarFraAndreinstansVurdering
)

data class SvarFraAndreinstansVurdering(
    val begrunnelse: String,
    val konsekvens: SvarFraAndreinstansKonsekvens,
    val vilkårSomOmgjøres: List<Hjemmel>,
    val vurdertAv: Bruker,
    val opprettet: Instant
)

enum class SvarFraAndreinstansKonsekvens {
    OMGJØRING,
    BEHANDLE_PÅ_NYTT,
    INGENTING
}