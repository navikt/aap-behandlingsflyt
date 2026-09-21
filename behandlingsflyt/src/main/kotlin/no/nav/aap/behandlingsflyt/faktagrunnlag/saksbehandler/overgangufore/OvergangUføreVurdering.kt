package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore

import no.nav.aap.behandlingsflyt.dokumentasjon.Blokker
import no.nav.aap.behandlingsflyt.dokumentasjon.Dict
import no.nav.aap.behandlingsflyt.dokumentasjon.Div
import no.nav.aap.behandlingsflyt.dokumentasjon.Fritekstfelt
import no.nav.aap.behandlingsflyt.dokumentasjon.JaNeiValg
import no.nav.aap.behandlingsflyt.dokumentasjon.PrettyEnum
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant
import java.time.LocalDate

data class OvergangUføreVurdering(
    val begrunnelse: String,
    val brukerHarSøktOmUføretrygd: Boolean,
    val brukerHarFåttVedtakOmUføretrygd: UføreSøknadVedtakResultat?,
    val brukerRettPåAAP: Boolean?,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertAv: Bruker,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : PeriodisertVurdering {
    fun harRettPåAAPMedOvergangUføre(): Boolean {
        return brukerHarSøktOmUføretrygd
                && brukerHarFåttVedtakOmUføretrygd == UføreSøknadVedtakResultat.NEI
                && brukerRettPåAAP == true
    }

    override fun genererDokumentasjon(): Blokker {
        return Div(
            Fritekstfelt("Begrunnelse", begrunnelse),
            Dict(
                "Har søkt om uføretrygd" to JaNeiValg(brukerHarSøktOmUføretrygd),
                "Fått vedtak om uføretrygd" to PrettyEnum(brukerHarFåttVedtakOmUføretrygd),
                "Har rett på AAP" to JaNeiValg(brukerRettPåAAP),
            )
        )
    }
}

enum class UføreSøknadVedtakResultat(val verdi: String) {
    JA_AVSLAG("JA_AVSLAG"),
    JA_INNVILGET_GRADERT("JA_INNVILGET_GRADERT"),
    JA_INNVILGET_FULL("JA_INNVILGET_FULL"),
    NEI("NEI")
}

fun List<OvergangUføreVurdering>.erFunksjoneltLik(other: List<OvergangUføreVurdering>): Boolean {
    if (this.size != other.size) return false
    return this.zip(other).all { (a, b) ->
        a.begrunnelse == b.begrunnelse &&
                a.brukerHarSøktOmUføretrygd == b.brukerHarSøktOmUføretrygd &&
                a.brukerHarFåttVedtakOmUføretrygd == b.brukerHarFåttVedtakOmUføretrygd &&
                a.brukerRettPåAAP == b.brukerRettPåAAP &&
                a.fom == b.fom &&
                a.tom == b.tom
    }
}
