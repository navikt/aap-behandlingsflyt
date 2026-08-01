package no.nav.aap.behandlingsflyt.steg.samordning.barnepensjon

import java.time.Instant
import java.time.YearMonth
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.årmåned.ÅrMånedPeriodeLøsning
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.samordning.barnepensjon.BarnepensjonPeriode
import no.nav.aap.samordning.barnepensjon.BarnepensjonVurdering

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
            opprettet = Instant.now()
        )
    }
}

data class BarnepensjonLøsningPeriodeDto(
    override val fom: String,
    override val tom: String?,
    val månedsbeløp: Beløp
) : ÅrMånedPeriodeLøsning {
    fun tilBarnepensjonPeriode(): BarnepensjonPeriode {
        return BarnepensjonPeriode(
            fom = YearMonth.parse(fom),
            tom = tom?.let { YearMonth.parse(it) },
            månedsats = månedsbeløp
        )
    }
}