package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate

data class VedtakslengdeGrunnlag(
    val vurderinger: List<VedtakslengdeVurdering>
) {
    constructor(vurdering: VedtakslengdeVurdering) : this(listOf(vurdering))

    /** Siste vurdering (convenience for eksisterende kallsteder) */
    val vurdering: VedtakslengdeVurdering? get() = vurderinger.lastOrNull()
}

data class VedtakslengdeVurdering(
    val sluttdato: LocalDate,
    val utvidetMed: ÅrMedHverdager,
    val vurdertAv: Bruker,
    val vurdertIBehandling: BehandlingId,
    val opprettet: Instant
)