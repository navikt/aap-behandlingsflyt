package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

data class Sykdomsvurdering(
    val id: Long? = null,
    val begrunnelse: String,
    val vurderingenGjelderFra: LocalDate?,
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
    val opprettet: Instant,
    val vurdertAv: Bruker,
) {
    private fun erAndelNedsattNok(): Boolean {
        return erNedsettelseIArbeidsevneMerEnnHalvparten == true || erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true
    }

    fun erOppfylt(behandlingType: TypeBehandling, kravDato: LocalDate): Boolean {
        /* Det stemmer vel ikke å se på behandlingstypen her? For poenget er vel at vurderingen som gjelder
         * fra kravdatoen (som som oftest stammer fra en førstegangsbehandling) har en viss varighet.
         * Se `erOppfylt(LocalDate)` nedenfor.
         */
        return when (behandlingType) {
            TypeBehandling.Førstegangsbehandling -> erOppfyltSettBortIfraVissVarighet() && erNedsettelseIArbeidsevneAvEnVissVarighet == true
            TypeBehandling.Revurdering -> {
                if (erFørsteVurdering(kravDato)) {
                    erOppfyltSettBortIfraVissVarighet() && erNedsettelseIArbeidsevneAvEnVissVarighet == true
                } else {
                    erOppfyltSettBortIfraVissVarighet()
                }
            }

            else -> error("Ugyldig behandlingsType: $behandlingType for vurdering av sykdom.")
        }
    }

    /* Denne metoden må sannsynligvis generaliseres når vi skal implementere gjeninntreden etter opphør. */
    fun erFørsteVurdering(kravdato: LocalDate): Boolean {
        return vurderingenGjelderFra == null || vurderingenGjelderFra == kravdato
    }

    fun erOppfylt(kravdato: LocalDate): Boolean {
        return erOppfyltSettBortIfraVissVarighet() &&
                if (erFørsteVurdering(kravdato)) erNedsettelseIArbeidsevneAvEnVissVarighet == true
                else true
    }

    fun erOppfyltSettBortIfraVissVarighet(): Boolean {
        return harSkadeSykdomEllerLyte && erArbeidsevnenNedsatt == true && erSkadeSykdomEllerLyteVesentligdel == true && erAndelNedsattNok()
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




