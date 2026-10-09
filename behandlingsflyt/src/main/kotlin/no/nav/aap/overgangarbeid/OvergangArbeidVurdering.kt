package no.nav.aap.overgangarbeid

import no.nav.aap.behandlingsflyt.dokumentasjon.Blokker
import no.nav.aap.behandlingsflyt.dokumentasjon.Dict
import no.nav.aap.behandlingsflyt.dokumentasjon.Div
import no.nav.aap.behandlingsflyt.dokumentasjon.Fritekstfelt
import no.nav.aap.behandlingsflyt.dokumentasjon.JaNeiValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate

data class OvergangArbeidVurdering(
    val begrunnelse: String,
    val brukerRettPåAAP: Boolean,
    override val vurdertAv: Bruker,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val opprettet: Instant,
    override val vurdertIBehandling: BehandlingId,
) : PeriodisertVurdering {
    override fun genererDokumentasjon(): Blokker {
        return Div(
            Fritekstfelt("Begrunnelse", begrunnelse),
            Dict(
                "Rett på AAP" to JaNeiValg(brukerRettPåAAP),
            )
        )
    }
}

fun List<OvergangArbeidVurdering>.erFunksjoneltLik(other: List<OvergangArbeidVurdering>): Boolean {
    if (this.size != other.size) return false

    return this.zip(other).all { (a, b) ->
        a.begrunnelse == b.begrunnelse &&
                a.brukerRettPåAAP == b.brukerRettPåAAP &&
                a.fom == b.fom &&
                a.tom == b.tom
    }
}