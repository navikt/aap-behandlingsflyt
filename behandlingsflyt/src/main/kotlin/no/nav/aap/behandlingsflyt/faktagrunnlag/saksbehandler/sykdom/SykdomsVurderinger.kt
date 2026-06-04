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
    val harSkadeSykdomEllerLyte: Boolean,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
    val yrkesskadeBegrunnelse: String?,
    val harNedsattArbeidsevne: ArbeidsevneNedsattValg?,
    val diagnose: Diagnose?,
    val vurdertIBehandling: BehandlingId,
    val opprettet: Instant,
    val vurdertAv: Bruker,
) {
    fun erKonsistentForSykdom(harYrkesskadeRegistrert: Boolean): Boolean {
        if (!harSkadeSykdomEllerLyte && erSkadeSykdomEllerLyteVesentligdel == true) {
            return false
        }
        
        if (harSkadeSykdomEllerLyte && harNedsattArbeidsevne == null) {
            return false
        }
        
        if (harNedsattArbeidsevne == ArbeidsevneNedsattValg.NEI && (erNedsettelseIArbeidsevneMerEnnHalvparten == true)) {
            return false
        }
        
        if (erNedsettelseIArbeidsevneMerEnnHalvparten != null &&
            !erNedsettelseIArbeidsevneMerEnnHalvparten &&
            harYrkesskadeRegistrert &&
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == null
        ) {
            return false
        }
        return true
    }

    fun erOppfyltOrdinærMedUtlededeFelter(): Boolean {
        return harSkadeSykdomEllerLyte
                && harNedsattArbeidsevne == ArbeidsevneNedsattValg.JA
                && erSkadeSykdomEllerLyteVesentligdel == true
                && erNedsettelseIArbeidsevneMerEnnHalvparten == true
    }

    fun skalVurderesForSykepengeerstatning(): Boolean {
        return harSkadeSykdomEllerLyte
                && harNedsattArbeidsevne == ArbeidsevneNedsattValg.JA_FORBIGÅENDE_PROBLEMER
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (erNedsettelseIArbeidsevneMerEnnHalvparten == true || erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true)
    }

    fun erOppfyltForOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng(): Boolean {
        val erTilstrekkeligNedsattArbeidsevne =
            erNedsettelseIArbeidsevneMerEnnHalvparten == true ||
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true

        return harSkadeSykdomEllerLyte
                && harNedsattArbeidsevne == ArbeidsevneNedsattValg.JA
                && erSkadeSykdomEllerLyteVesentligdel == true
                && erTilstrekkeligNedsattArbeidsevne
    }

    fun erKonsistentMedSykepengeerstatning(yrkesskadevurdering: Yrkesskadevurdering?): Boolean {
        return harSkadeSykdomEllerLyte
                && harNedsattArbeidsevne == ArbeidsevneNedsattValg.JA_FORBIGÅENDE_PROBLEMER
                && erSkadeSykdomEllerLyteVesentligdel == true
                && (erNedsettelseIArbeidsevneMerEnnHalvparten == true || (erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true && yrkesskadevurdering?.erÅrsakssammenheng == true))
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
    NEI_MEN_STUDENT
}

