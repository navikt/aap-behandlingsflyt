package no.nav.aap.behandlingsflyt.drift

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Segment
import java.time.Instant
import java.time.LocalDate

data class VedtakslengdeVurderingDriftsinfoDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val sluttdato: LocalDate,
    val utvidetMed: ÅrMedHverdager,
    val årsaker: List<VedtakslengdeÅrsak>,
    val vurdertAv: String,
    val opprettet: Instant,
    val manuellVurdering: Boolean,
    val erNy: Boolean
)

internal fun Segment<VedtakslengdeVurdering>.tilDriftsinfoDto(
    behandlingId: BehandlingId
) = VedtakslengdeVurderingDriftsinfoDto(
    fom = periode.fom,
    tom = periode.tom,
    sluttdato = verdi.sluttdato,
    utvidetMed = verdi.utvidetMed,
    årsaker = verdi.årsaker,
    vurdertAv = verdi.vurdertAv.ident,
    opprettet = verdi.opprettet,
    manuellVurdering = verdi.vurdertManuelt,
    erNy = verdi.vurdertIBehandling == behandlingId
)
