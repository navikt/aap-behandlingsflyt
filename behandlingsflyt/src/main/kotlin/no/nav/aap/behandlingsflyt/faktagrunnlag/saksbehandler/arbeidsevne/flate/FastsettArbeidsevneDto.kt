package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate
import java.time.LocalDateTime

data class PeriodisertFastsettArbeidsevneDto(
    override val begrunnelse: String,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val arbeidsevne: Int,
) : LøsningForPeriode {
    fun toArbeidsevnevurdering(kontekst: AvklaringsbehovKontekst) =
        toArbeidsevnevurdering(
            vurdertIBehandling = kontekst.behandlingId(),
            bruker = kontekst.bruker,
        )

    fun toArbeidsevnevurdering(bruker: Bruker, vurdertIBehandling: BehandlingId) =
        ArbeidsevneVurdering(
            begrunnelse = begrunnelse,
            arbeidsevne = Prosent(arbeidsevne),
            vurdertIBehandling = vurdertIBehandling,
            opprettetTid = LocalDateTime.now(),
            vurdertAv = bruker,
            fom = fom,
            tom = tom
        )
}