package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate
import java.time.LocalDateTime

data class FastsettArbeidsevneDto(
    val begrunnelse: String,
    val arbeidsevne: Int,
    val fraDato: LocalDate
) {
    fun toArbeidsevnevurdering(vurdertAv: String) =
        ArbeidsevneVurdering(
            begrunnelse = begrunnelse,
            arbeidsevne = Prosent(arbeidsevne),
            fraDato = fraDato,
            opprettetTid = LocalDateTime.now(),
            vurdertAv = vurdertAv
        )
}

data class PeriodisertFastsettArbeidsevneDto(
    override val begrunnelse: String,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    val arbeidsevne: Int,
) : LøsningForPeriode {
    fun toArbeidsevnevurdering(kontekst: AvklaringsbehovKontekst) =
        ArbeidsevneVurdering(
            begrunnelse = begrunnelse,
            arbeidsevne = Prosent(arbeidsevne),
            fraDato = fom,
            tilDato = tom,
            vurdertIBehandling = kontekst.behandlingId(),
            opprettetTid = LocalDateTime.now(),
            vurdertAv = kontekst.bruker.ident
        )
}