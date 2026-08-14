package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate
import java.time.LocalDateTime

data class PeriodisertSykepengerVurderingDto(
    override val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val harRettPå: Boolean,
    val grunn: SykepengerGrunn? = null,
    override val fom: LocalDate,
    override val tom: LocalDate? = null,
): LøsningForPeriode {
    fun tilVurdering(
        bruker: Bruker,
        behandlingId: BehandlingId,
    ): SykepengerVurdering = SykepengerVurdering(
        begrunnelse = begrunnelse,
        harRettPå = harRettPå,
        grunn = grunn,
        vurdertIBehandling = behandlingId,
        vurdertAv = bruker,
        vurdertTidspunkt = LocalDateTime.now(),
        fom = fom,
        tom = tom,
    )
}