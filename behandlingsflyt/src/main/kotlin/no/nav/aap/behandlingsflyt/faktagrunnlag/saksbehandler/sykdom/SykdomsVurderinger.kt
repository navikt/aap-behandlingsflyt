package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

data class Sykdomsvurdering(
    val id: Long? = null,
    val begrunnelse: String,
    val vurderingenGjelderFra: LocalDate,
    val vurderingenGjelderTil: LocalDate?,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val harSkadeSykdomEllerLyte: Boolean,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
    val yrkesskadeBegrunnelse: String?,
    val erArbeidsevnenNedsatt: Boolean?,
    val kodeverk: String? = null,
    val hoveddiagnose: String? = null,
    val bidiagnoser: List<String>? = emptyList(),
    val vurdertIBehandling: BehandlingId,
    val opprettet: Instant,
    val vurdertAv: Bruker,
) {
    fun erOppfyltOrdinærtEllerMedYrkesskadeSettBortFraVissVarighet(yrkesskadevurdering: Yrkesskadevurdering?): Boolean {
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (
                erNedsettelseIArbeidsevneMerEnnHalvparten == true ||
                        ((yrkesskadevurdering?.erÅrsakssammenheng ?: false) &&
                                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true)
                )
    }

    fun erOppfyltOrdinærtEllerMedYrkesskadeMenIkkeVissVarighet(yrkesskadevurdering: Yrkesskadevurdering?): Boolean {
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && erNedsettelseIArbeidsevneAvEnVissVarighet == false
                && (
                erNedsettelseIArbeidsevneMerEnnHalvparten == true ||
                        ((yrkesskadevurdering?.erÅrsakssammenheng ?: false) &&
                               erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true)
                )
    }
    
    fun erOppfyltOrdinær(kravdato: LocalDate, segmentPeriode: Periode): Boolean {
        return erOppfyltOrdinærSettBortIfraVissVarighet() &&
                if (erFørsteVurdering(kravdato, segmentPeriode)) erNedsettelseIArbeidsevneAvEnVissVarighet == true
                else true
    }

    fun erOppfyltForYrkesskadeSettBortIfraÅrsakssammenheng(kravdato: LocalDate, segmentPeriode: Periode): Boolean {
        val erTilstrekkeligNedsattArbeidsevne = erNedsettelseIArbeidsevneMerEnnHalvparten == true
                || erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true
        
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && erTilstrekkeligNedsattArbeidsevne
                && erVissVarighetOmRelevant(kravdato, segmentPeriode)
    }

    fun erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengOgVissVarighet(): Boolean {
        val erTilstrekkeligNedsattArbeidsevne = erNedsettelseIArbeidsevneMerEnnHalvparten == true
                || erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true

        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && erTilstrekkeligNedsattArbeidsevne
    }

    private fun erVissVarighetOmRelevant(kravdato: LocalDate, segmentPeriode: Periode): Boolean {
        return if (erFørsteVurdering(kravdato, segmentPeriode))
            erNedsettelseIArbeidsevneAvEnVissVarighet == true
        else true
    }

    fun erOppfyltOrdinærSettBortIfraVissVarighet(): Boolean {
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && erNedsettelseIArbeidsevneMerEnnHalvparten == true
    }

    fun erKonsistentForSykdom(harYrkesskadeRegistrert: Boolean, typeBehandling: TypeBehandling): Boolean {
        if (!harSkadeSykdomEllerLyte && erSkadeSykdomEllerLyteVesentligdel == true) {
            return false
        }
        if (erArbeidsevnenNedsatt == false && erNedsettelseIArbeidsevneMerEnnHalvparten == true) {
            return false
        }
        if (erNedsettelseIArbeidsevneMerEnnHalvparten != null &&
            !erNedsettelseIArbeidsevneMerEnnHalvparten &&
            harYrkesskadeRegistrert &&
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == null
        ) {
            return false
        }
        if (erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense != null && yrkesskadeBegrunnelse.isNullOrBlank() && typeBehandling == TypeBehandling.Førstegangsbehandling) {
            return false
        }
        return true
    }
    
    companion object {
        fun erFørsteVurdering(kravdato: LocalDate, segmentPeriode: Periode): Boolean {
            return segmentPeriode.inneholder(kravdato)
        }

    }
}

/**
 * @param relevanteSaker Liste over saksnumre til yrkesskadesaker fra register.
 */
data class Yrkesskadevurdering(
    val id: Long? = null,
    val begrunnelse: String,
    val relevanteSaker: List<YrkesskadeSak>,
    val erÅrsakssammenheng: Boolean,
    val andelAvNedsettelsen: Prosent?,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null,
)

data class YrkesskadeSak(
    val referanse: String,
    val manuellYrkesskadeDato: LocalDate?,
)




