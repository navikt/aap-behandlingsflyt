package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
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

    fun erOppfyltOrdinærtEllerMedYrkesskadeMenIkkeVissVarighet(kravdato: LocalDate, yrkesskadevurdering: Yrkesskadevurdering?): Boolean {
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (if (erFørsteVurdering(kravdato)) erNedsettelseIArbeidsevneAvEnVissVarighet == false else true)
                && (
                erNedsettelseIArbeidsevneMerEnnHalvparten == true ||
                        ((yrkesskadevurdering?.erÅrsakssammenheng ?: false) &&
                               erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true)
                )
    }

    /* Denne metoden må sannsynligvis generaliseres når vi skal implementere gjeninntreden etter opphør. */
    fun erFørsteVurdering(kravdato: LocalDate): Boolean {
        return vurderingenGjelderTil?.let {
            vurderingenGjelderFra <= kravdato && kravdato <= vurderingenGjelderTil
        } ?: (vurderingenGjelderFra <= kravdato)
    }

    fun erOppfylt(kravdato: LocalDate): Boolean {
        return erOppfyltSettBortIfraVissVarighet() &&
                if (erFørsteVurdering(kravdato)) erNedsettelseIArbeidsevneAvEnVissVarighet == true
                else true
    }

    fun erOppfyltForYrkesskade(): Boolean {
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (erNedsettelseIArbeidsevneMerEnnHalvparten == true
                || erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true) // trengs viss varighet for yrkesskade?
    }

    fun erOppfyltSettBortIfraVissVarighet(): Boolean {
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




