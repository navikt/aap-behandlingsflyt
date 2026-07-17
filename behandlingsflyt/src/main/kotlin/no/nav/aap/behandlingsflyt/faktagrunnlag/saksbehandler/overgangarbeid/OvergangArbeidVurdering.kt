package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Instant
import java.time.LocalDate

data class OvergangArbeidVurdering(
    val begrunnelse: String,
    val brukerRettPåAAP: Boolean,
    val vurdertAv: String,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val opprettet: Instant,
    override val vurdertIBehandling: BehandlingId,
) : PeriodisertVurdering

fun List<OvergangArbeidVurdering>.erFunksjoneltLik(other: List<OvergangArbeidVurdering>): Boolean {
    if (this.size != other.size) return false

    return this.zip(other).all { (a, b) ->
        a.begrunnelse == b.begrunnelse &&
                a.brukerRettPåAAP == b.brukerRettPåAAP &&
                a.fom == b.fom &&
                a.tom == b.tom
    }
}