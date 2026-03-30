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
    val begrunnelse: String,
    val vurderingenGjelderFra: LocalDate,
    val vurderingenGjelderTil: LocalDate?,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val harSkadeSykdomEllerLyte: Boolean,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
    val erNedsettelseMinstHalvparten: ErNedsettelseMinstHalvpartenValg?,
    val erNedsettelseMerEnnYrkesskadegrense: ErNedsettelseMerEnnYrkesskadegrenseValg?,
    val yrkesskadeBegrunnelse: String?,
    val erArbeidsevnenNedsatt: Boolean?,
    val diagnose: Diagnose?,
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

    @Deprecated("Erstattes av erKonsistentMedSykepengeerstatning")
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

    fun erOppfyltOrdinær(kravdato: LocalDate, periodenVurderingenGjelderFor: Periode): Boolean {
        return erOppfyltOrdinærSettBortIfraVissVarighet() &&
                if (erFørsteVurdering(
                        kravdato,
                        periodenVurderingenGjelderFor
                    )
                ) erNedsettelseIArbeidsevneAvEnVissVarighet == true
                else true
    }

    fun erOppfyltForYrkesskadeSettBortIfraÅrsakssammenheng(
        kravdato: LocalDate,
        periodenVurderingenGjelderFor: Periode
    ): Boolean {
        val erTilstrekkeligNedsattArbeidsevne = erNedsettelseIArbeidsevneMerEnnHalvparten == true
                || erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true

        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && erTilstrekkeligNedsattArbeidsevne
                && erVissVarighetOmRelevant(kravdato, periodenVurderingenGjelderFor)
    }

    fun erOppfyltOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng(
        kravDato: LocalDate,
        periodenVurderingenGjelderFor: Periode
    ): Boolean {
        return erOppfyltForYrkesskadeSettBortIfraÅrsakssammenheng(
            kravDato,
            periodenVurderingenGjelderFor
        ) || erOppfyltOrdinær(
            kravDato,
            periodenVurderingenGjelderFor
        )
    }


    fun erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengOgVissVarighet(): Boolean {
        val erTilstrekkeligNedsattArbeidsevne = erNedsettelseIArbeidsevneMerEnnHalvparten == true
                || erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true

        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && erTilstrekkeligNedsattArbeidsevne
    }

    private fun erVissVarighetOmRelevant(kravdato: LocalDate, periodenVurderingenGjelderFor: Periode): Boolean {
        return if (erFørsteVurdering(kravdato, periodenVurderingenGjelderFor))
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
        fun erFørsteVurdering(kravdato: LocalDate, periodenVurderingenGjelderFor: Periode): Boolean {
            return periodenVurderingenGjelderFor.inneholder(kravdato)
        }
    }
    
    fun utledErNedsettelseMinstHalvparten(): ErNedsettelseMinstHalvpartenValg? {
        if (erNedsettelseMinstHalvparten != null) {
            return erNedsettelseMinstHalvparten
        }

        return when {
            erNedsettelseIArbeidsevneMerEnnHalvparten == true && erNedsettelseIArbeidsevneAvEnVissVarighet == true ->
                ErNedsettelseMinstHalvpartenValg.JA

            erNedsettelseIArbeidsevneMerEnnHalvparten == true && erNedsettelseIArbeidsevneAvEnVissVarighet == false ->
                ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER

            erNedsettelseIArbeidsevneMerEnnHalvparten == true && erNedsettelseIArbeidsevneAvEnVissVarighet == null ->
                ErNedsettelseMinstHalvpartenValg.JA

            erNedsettelseIArbeidsevneMerEnnHalvparten == false ->
                ErNedsettelseMinstHalvpartenValg.NEI

            else -> null
        }
    }

    /**
     * Returnerer enum-verdi for nedsettelse mer enn yrkesskadegrense.
     * Hvis feltet er satt, bruk det direkte.
     * Ellers utled fra gamle boolean-felter.
     */
    fun utledErNedsettelseMerEnnYrkesskadegrense(): ErNedsettelseMerEnnYrkesskadegrenseValg? {
        if (erNedsettelseMerEnnYrkesskadegrense != null) {
            return erNedsettelseMerEnnYrkesskadegrense
        }

        return when {
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true && erNedsettelseIArbeidsevneAvEnVissVarighet == true ->
                ErNedsettelseMerEnnYrkesskadegrenseValg.JA

            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true && erNedsettelseIArbeidsevneAvEnVissVarighet == false ->
                ErNedsettelseMerEnnYrkesskadegrenseValg.JA_FORBIGÅENDE_PROBLEMER

            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true && erNedsettelseIArbeidsevneAvEnVissVarighet == null ->
                ErNedsettelseMerEnnYrkesskadegrenseValg.JA

            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == false ->
                ErNedsettelseMerEnnYrkesskadegrenseValg.NEI

            else -> null
        }
    }

    // ---- NYE METODER SOM BRUKER ENUM-FELTER (uten kravdato-logikk) ----

    /**
     * Ny versjon av erOppfyltOrdinær som bruker enum-felter.
     * Ignorerer kravdato - samme logikk uavhengig av når vurderingen gjelder fra.
     */
    fun erOppfyltOrdinærMedNyeFelter(): Boolean {
        val nedsettelse = utledErNedsettelseMinstHalvparten()
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && nedsettelse == ErNedsettelseMinstHalvpartenValg.JA
    }

    fun skalVurderesForSykepengeerstatningMedNyeFelter(): Boolean {
        val nedsettelseHalvparten = utledErNedsettelseMinstHalvparten()
        val nedsetteYrkesskade = utledErNedsettelseMerEnnYrkesskadegrense()
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (nedsettelseHalvparten == ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER || nedsetteYrkesskade == ErNedsettelseMerEnnYrkesskadegrenseValg.JA_FORBIGÅENDE_PROBLEMER)
    }

    fun erKonsistentMedSykepengeerstatning(yrkesskadevurdering: Yrkesskadevurdering?): Boolean {
        val nedsettelseHalvparten = utledErNedsettelseMinstHalvparten()
        val nedsetteYrkesskade = utledErNedsettelseMerEnnYrkesskadegrense()
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (nedsettelseHalvparten == ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER || (nedsetteYrkesskade == ErNedsettelseMerEnnYrkesskadegrenseValg.JA_FORBIGÅENDE_PROBLEMER && yrkesskadevurdering?.erÅrsakssammenheng == true))
    }

    /**
     * Ny versjon av erOppfyltForYrkesskadeSettBortIfraÅrsakssammenheng som bruker enum-felter.
     * Ignorerer kravdato - samme logikk uavhengig av når vurderingen gjelder fra.
     */
    fun erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengMedNyeFelter(): Boolean {
        val nedsettelseHalvparten = utledErNedsettelseMinstHalvparten()
        val nedsettelseYrkesskade = utledErNedsettelseMerEnnYrkesskadegrense()

        val erTilstrekkeligNedsattArbeidsevne =
            nedsettelseHalvparten == ErNedsettelseMinstHalvpartenValg.JA ||
                    nedsettelseYrkesskade == ErNedsettelseMerEnnYrkesskadegrenseValg.JA

        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && erTilstrekkeligNedsattArbeidsevne
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

enum class ErNedsettelseMinstHalvpartenValg {
    JA,
    JA_FORBIGÅENDE_PROBLEMER,
    NEI,
}

enum class ErNedsettelseMerEnnYrkesskadegrenseValg {
    JA,
    JA_FORBIGÅENDE_PROBLEMER,
    NEI
}
