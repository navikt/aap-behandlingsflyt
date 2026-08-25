package no.nav.aap.behandlingsflyt.behandling.oppholdskrav

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate
import java.time.LocalDateTime

data class AvklarOppholdkravLøsningForPeriodeDto(
    override val begrunnelse: String,
    override val fom: LocalDate,
    override val tom: LocalDate? = null,
    val oppfylt: Boolean,
    val land: String?,
): LøsningForPeriode {
    fun tilOppholdskravPeriode() = OppholdskravPeriode(
        fom = fom,
        tom = tom,
        begrunnelse = begrunnelse,
        land = land,
        oppfylt = oppfylt,
    )

    fun tilOppholdskravPeriodisertVurdering(
        bruker: Bruker,
        behandlingId: BehandlingId,
    ) = OppholdskravPeriodisertVurdering(
        fom = fom,
        tom = tom,
        land = land,
        oppfylt = oppfylt,
        begrunnelse = begrunnelse,
        vurdertAv = bruker,
        vurdertIBehandling = behandlingId,
        opprettetTid = LocalDateTime.now(),
    )
}


