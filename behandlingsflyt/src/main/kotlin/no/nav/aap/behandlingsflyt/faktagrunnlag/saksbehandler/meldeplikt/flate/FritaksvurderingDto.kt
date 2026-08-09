package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate
import java.time.LocalDateTime

data class FritaksvurderingDto(
    val harFritak: Boolean,
    val fraDato: LocalDate,
    val begrunnelse: String,
)

data class PeriodisertFritaksvurderingDto(
    override val begrunnelse: String,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val harFritak: Boolean,
) : LøsningForPeriode {
    fun tilFritaksvurdering(
        kontekst: AvklaringsbehovKontekst
    ): Fritaksvurdering = tilFritaksvurdering(
        bruker = kontekst.bruker,
        vurdertIBehandling = kontekst.behandlingId(),
    )

    fun tilFritaksvurdering(
        bruker: Bruker,
        vurdertIBehandling: BehandlingId,
    ): Fritaksvurdering = Fritaksvurdering(
        harFritak = harFritak,
        fom = fom,
        tom = tom,
        begrunnelse = begrunnelse,
        vurdertAv = bruker,
        vurdertIBehandling = vurdertIBehandling,
        opprettetTid = LocalDateTime.now()
    )
}