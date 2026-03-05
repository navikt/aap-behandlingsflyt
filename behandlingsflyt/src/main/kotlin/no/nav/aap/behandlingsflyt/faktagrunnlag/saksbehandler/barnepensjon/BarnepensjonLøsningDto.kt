package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnepensjon

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.årmåned.ÅrMånedPeriodeLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.YearMonth

data class BarnepensjonLøsningDto(
    val begrunnelse: String,
    val perioder: List<BarnepensjonLøsningPeriodeDto>
) {
    fun tilVurdering(bruker: Bruker, vurdertIBehandling: BehandlingId): BarnepensjonVurdering {
        return BarnepensjonVurdering(
            begrunnelse = begrunnelse,
            perioder = perioder.map { it.tilBarnepensjonPeriode() }.toSet(),
            vurdertIBehandling = vurdertIBehandling,
            vurdertAv = bruker,
            opprettet = java.time.Instant.now()
        )
    }
}

data class BarnepensjonLøsningPeriodeDto(
    override val fom: YearMonth,
    override val tom: YearMonth?,
    val månedsbeløp: Beløp
) : ÅrMånedPeriodeLøsning {
    fun tilBarnepensjonPeriode(): BarnepensjonPeriode {
        return BarnepensjonPeriode(
            fom = fom,
            tom = tom,
            månedsats = månedsbeløp
        )
    }
}