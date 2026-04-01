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
    @Deprecated("Bakes inn i erNedsettelseMinstHalvparten og erNedsettelseMerEnnYrkesskadegrense")
    val erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?,
    @Deprecated("Erstattes av erNedsettelseMinstHalvparten etter migrering")
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    val erNedsettelseMinstHalvparten: ErNedsettelseMinstHalvpartenValg?,
    @Deprecated("Erstattes av erNedsettelseMerEnnYrkesskadegrense")
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
    val erNedsettelseMerEnnYrkesskadegrense: ErNedsettelseMerEnnYrkesskadegrenseValg?,
    val yrkesskadeBegrunnelse: String?,
    val erArbeidsevnenNedsatt: Boolean?,
    val diagnose: Diagnose?,
    val vurdertIBehandling: BehandlingId,
    val opprettet: Instant,
    val vurdertAv: Bruker,
) {
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

    fun erOppfyltOrdinærMedUtlededeFelter(): Boolean {
        val nedsettelse = utledErNedsettelseMinstHalvparten()
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && nedsettelse == ErNedsettelseMinstHalvpartenValg.JA
    }

    fun skalVurderesForSykepengeerstatningMedUtlededeFelter(): Boolean {
        val nedsettelseHalvparten = utledErNedsettelseMinstHalvparten()
        val nedsetteYrkesskade = utledErNedsettelseMerEnnYrkesskadegrense()
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (nedsettelseHalvparten == ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER || nedsetteYrkesskade == ErNedsettelseMerEnnYrkesskadegrenseValg.JA_FORBIGÅENDE_PROBLEMER)
    }

    fun erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengMedUtlededeFelter(): Boolean {
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


    fun erKonsistentMedSykepengeerstatning(yrkesskadevurdering: Yrkesskadevurdering?): Boolean {
        val nedsettelseHalvparten = utledErNedsettelseMinstHalvparten()
        val nedsetteYrkesskade = utledErNedsettelseMerEnnYrkesskadegrense()
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (nedsettelseHalvparten == ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER || (nedsetteYrkesskade == ErNedsettelseMerEnnYrkesskadegrenseValg.JA_FORBIGÅENDE_PROBLEMER && yrkesskadevurdering?.erÅrsakssammenheng == true))
    }

    fun erKonsistentMedSykepengeerstatningSettBortIfraÅrsakssammenheng(): Boolean {
        val nedsettelseHalvparten = utledErNedsettelseMinstHalvparten()
        val nedsetteYrkesskade = utledErNedsettelseMerEnnYrkesskadegrense()
        return harSkadeSykdomEllerLyte
                && erArbeidsevnenNedsatt == true
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (nedsettelseHalvparten == ErNedsettelseMinstHalvpartenValg.JA_FORBIGÅENDE_PROBLEMER || (nedsetteYrkesskade == ErNedsettelseMerEnnYrkesskadegrenseValg.JA_FORBIGÅENDE_PROBLEMER))
    }


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



