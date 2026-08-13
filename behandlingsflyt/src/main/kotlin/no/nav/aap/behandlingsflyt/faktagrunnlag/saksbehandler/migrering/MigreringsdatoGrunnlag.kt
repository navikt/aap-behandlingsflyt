package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.migrering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate
import java.time.LocalDateTime

data class MigreringsdatoGrunnlag(
    val vurderinger: List<MigreringsdatoVurdering>,
) {
    fun gjeldendeVurdering(): MigreringsdatoVurdering? = vurderinger.maxByOrNull { it.opprettet }
}

data class MigreringsdatoVurdering(
    val migreringsdato: LocalDate,
    val vurdertAv: Bruker,
    val vurdertIBehandling: BehandlingId,
    val opprettet: LocalDateTime,
)
