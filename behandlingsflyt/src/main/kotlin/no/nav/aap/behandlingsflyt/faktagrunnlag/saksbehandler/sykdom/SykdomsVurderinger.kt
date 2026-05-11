package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

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
    @Deprecated("Bakes inn i harNedsattArbeidsevne")
    val erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    @Deprecated("Bruk erNedsettelseIArbeidsevneMerEnnHalvparten")
    val erNedsettelseMinstHalvparten: ErNedsettelseMinstHalvpartenValg?,
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
    @Deprecated("Bruk erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense")
    val erNedsettelseMerEnnYrkesskadegrense: ErNedsettelseMerEnnYrkesskadegrenseValg?,
    val yrkesskadeBegrunnelse: String?,
    @Deprecated("Erstattes av harNedsattArbeidsevne")
    val erArbeidsevnenNedsatt: Boolean?,
    val harNedsattArbeidsevne: ArbeidsevneNedsattValg?,
    val diagnose: Diagnose?,
    val vurdertIBehandling: BehandlingId,
    val opprettet: Instant,
    val vurdertAv: Bruker,
) {

    @Deprecated("Erstattet av erOppfyltOrdinærMedUtlededeFelter")
    fun erOppfyltOrdinær(kravdato: LocalDate, periodenVurderingenGjelderFor: Periode): Boolean {
        return erOppfyltOrdinærSettBortIfraVissVarighet() &&
                if (erFørsteVurdering(
                        kravdato,
                        periodenVurderingenGjelderFor
                    )
                ) erNedsettelseIArbeidsevneAvEnVissVarighet == true
                else true
    }

    fun erOppfyltOrdinærSettBortIfraVissVarighet(): Boolean {
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && erNedsettelseIArbeidsevneMerEnnHalvparten == true
    }

    // TODO: Erstatt validering i løser
    fun erKonsistentForSykdom(harYrkesskadeRegistrert: Boolean): Boolean {
        if (!harSkadeSykdomEllerLyte && erSkadeSykdomEllerLyteVesentligdel == true) {
            return false
        }
        if (erArbeidsevnenNedsatt == false && (erNedsettelseMinstHalvparten == ErNedsettelseMinstHalvpartenValg.JA || erNedsettelseMinstHalvparten == ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER)) {
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
        if (erNedsettelseMinstHalvparten == ErNedsettelseMinstHalvpartenValg.NEI &&
            harYrkesskadeRegistrert &&
            erNedsettelseMerEnnYrkesskadegrense == null
        ) {
            return false
        }
        return true
    }

    @Deprecated("Bruk erOppfyltOrdinærMedUtlededeFelter")
    fun erOppfyltOrdinærMedUtlededeFelterGammel(): Boolean {
        val nedsettelse = utledErNedsettelseMinstHalvparten()
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && nedsettelse == ErNedsettelseMinstHalvpartenValg.JA
    }

    fun erOppfyltOrdinærMedUtlededeFelter(): Boolean {
        return harSkadeSykdomEllerLyte
                && utledHarNedsattArbeidsevne() == ArbeidsevneNedsattValg.JA
                && erSkadeSykdomEllerLyteVesentligdel == true
                && erNedsettelseIArbeidsevneMerEnnHalvparten == true
    }

    @Deprecated("Bruk skalVurderesForSykepengeerstatning")
    fun skalVurderesForSykepengeerstatningMedUtlededeFelter(): Boolean {
        val nedsettelseHalvparten = utledErNedsettelseMinstHalvparten()
        val nedsetteYrkesskade = utledErNedsettelseMerEnnYrkesskadegrense()
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (nedsettelseHalvparten == ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER || nedsetteYrkesskade == ErNedsettelseMerEnnYrkesskadegrenseValg.JA_FORBIGÅENDE_PROBLEMER)
    }

    fun skalVurderesForSykepengeerstatning(): Boolean {
        return harSkadeSykdomEllerLyte
                && utledHarNedsattArbeidsevne() == ArbeidsevneNedsattValg.JA_FORBIGÅENDE_PROBLEMER
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (erNedsettelseIArbeidsevneMerEnnHalvparten == true || erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true)
    }

    @Deprecated("Bruk erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng")
    fun erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenhengMedUtlededeFelter(): Boolean {
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

    fun erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng(): Boolean {
        val erTilstrekkeligNedsattArbeidsevne =
            erNedsettelseIArbeidsevneMerEnnHalvparten == true ||
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true

        return harSkadeSykdomEllerLyte
                && utledHarNedsattArbeidsevne() == ArbeidsevneNedsattValg.JA
                && erSkadeSykdomEllerLyteVesentligdel == true
                && erTilstrekkeligNedsattArbeidsevne
    }

    @Deprecated("Bruk erKonsistentMedSykepengeerstatning")
    fun erKonsistentMedSykepengeerstatningGammel(yrkesskadevurdering: Yrkesskadevurdering?): Boolean {
        val nedsettelseHalvparten = utledErNedsettelseMinstHalvparten()
        val nedsetteYrkesskade = utledErNedsettelseMerEnnYrkesskadegrense()
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (nedsettelseHalvparten == ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER || (nedsetteYrkesskade == ErNedsettelseMerEnnYrkesskadegrenseValg.JA_FORBIGÅENDE_PROBLEMER && yrkesskadevurdering?.erÅrsakssammenheng == true))
    }

    fun erKonsistentMedSykepengeerstatning(yrkesskadevurdering: Yrkesskadevurdering?): Boolean {
        return harSkadeSykdomEllerLyte
                && utledHarNedsattArbeidsevne() == ArbeidsevneNedsattValg.JA_FORBIGÅENDE_PROBLEMER
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (erNedsettelseIArbeidsevneMerEnnHalvparten == true || (erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true && yrkesskadevurdering?.erÅrsakssammenheng == true))
    }

    @Deprecated("Bruk skalVurderesForSykepengeerstatning")
    fun erKonsistentMedSykepengeerstatningSettBortIfraÅrsakssammenheng(): Boolean {
        val nedsettelseHalvparten = utledErNedsettelseMinstHalvparten()
        val nedsetteYrkesskade = utledErNedsettelseMerEnnYrkesskadegrense()
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (nedsettelseHalvparten == ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER || (nedsetteYrkesskade == ErNedsettelseMerEnnYrkesskadegrenseValg.JA_FORBIGÅENDE_PROBLEMER))
    }

    fun utledHarNedsattArbeidsevne(): ArbeidsevneNedsattValg? {
        if (harNedsattArbeidsevne != null) {
            harNedsattArbeidsevne
        }

        if (erArbeidsevnenNedsatt == null) return null
        if (!erArbeidsevnenNedsatt) return ArbeidsevneNedsattValg.NEI

        val erSykdomMedVissVarighet =
            utledErNedsettelseMinstHalvparten() == ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER || utledErNedsettelseMerEnnYrkesskadegrense() == ErNedsettelseMerEnnYrkesskadegrenseValg.JA_FORBIGÅENDE_PROBLEMER
        if (erSykdomMedVissVarighet) {
            return ArbeidsevneNedsattValg.JA_FORBIGÅENDE_PROBLEMER
        }
        return ArbeidsevneNedsattValg.JA
    }

    @Deprecated("Bruk erNedsettelseIArbeidsevneMerEnnHalvparten")
    fun utledErNedsettelseMinstHalvparten(): ErNedsettelseMinstHalvpartenValg? {
        if (erNedsettelseMinstHalvparten != null) {
            return erNedsettelseMinstHalvparten
        }

        return when (erNedsettelseIArbeidsevneMerEnnHalvparten) {
            true if erNedsettelseIArbeidsevneAvEnVissVarighet == true ->
                ErNedsettelseMinstHalvpartenValg.JA

            true if erNedsettelseIArbeidsevneAvEnVissVarighet == false ->
                ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER

            true if erSkadeSykdomEllerLyteVesentligdel == true ->
                ErNedsettelseMinstHalvpartenValg.JA

            false ->
                ErNedsettelseMinstHalvpartenValg.NEI

            else -> null
        }
    }

    fun utledErNedsettelseMerEnnYrkesskadegrense(): ErNedsettelseMerEnnYrkesskadegrenseValg? {
        if (erNedsettelseMerEnnYrkesskadegrense != null) {
            return erNedsettelseMerEnnYrkesskadegrense
        }

        return when (erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense) {
            true if erNedsettelseIArbeidsevneAvEnVissVarighet == true ->
                ErNedsettelseMerEnnYrkesskadegrenseValg.JA

            true if erNedsettelseIArbeidsevneAvEnVissVarighet == false ->
                ErNedsettelseMerEnnYrkesskadegrenseValg.JA_FORBIGÅENDE_PROBLEMER

            true if erSkadeSykdomEllerLyteVesentligdel == true ->
                ErNedsettelseMerEnnYrkesskadegrenseValg.JA

            false ->
                ErNedsettelseMerEnnYrkesskadegrenseValg.NEI

            else -> null
        }
    }

    companion object {
        fun erFørsteVurdering(kravdato: LocalDate, periodenVurderingenGjelderFor: Periode): Boolean {
            return periodenVurderingenGjelderFor.inneholder(kravdato)
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

enum class ArbeidsevneNedsattValg {
    JA,
    JA_FORBIGÅENDE_PROBLEMER,
    NEI,
}

enum class ErNedsettelseMinstHalvpartenValg {
    JA,
    JA_FORBIGÅENDE_PROBLEMER,
    NEI,
}

enum class ErNedsettelseMerEnnYrkesskadegrenseValg {
    JA,
    JA_FORBIGÅENDE_PROBLEMER,
    NEI,
}



