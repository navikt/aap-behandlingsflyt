package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate

data class VedtakslengdeGrunnlag(
    val vurderinger: List<VedtakslengdeVurdering>
) {
    fun gjeldendeVurdering(): VedtakslengdeVurdering? {
        return vurderinger.maxByOrNull { it.opprettet }
    }
}

data class VedtakslengdeVurdering(
    val sluttdato: LocalDate,
    val utvidetMed: ÅrMedHverdager,
    val vurdertAv: Bruker,
    val vurdertIBehandling: BehandlingId,
    val opprettet: Instant,
    val begrunnelse: String,
) {
    val vurdertManuelt: Boolean get() = !vurdertAutomatisk
    val vurdertAutomatisk: Boolean get() = vurdertAv == SYSTEMBRUKER
}
