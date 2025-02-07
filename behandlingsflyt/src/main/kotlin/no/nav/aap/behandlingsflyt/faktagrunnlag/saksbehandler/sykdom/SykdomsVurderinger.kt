package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.YrkesskadevurderingDto
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate
import java.time.LocalDateTime

class Sykdomsvurdering(
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
    val opprettet: LocalDateTime? = null,
) {
    fun toDto(): SykdomsvurderingDto {
        return SykdomsvurderingDto(
            begrunnelse = begrunnelse,
            vurderingenGjelderFra = vurderingenGjelderFra,
            dokumenterBruktIVurdering = dokumenterBruktIVurdering,
            erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
            harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
            erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
            erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
            erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
            yrkesskadeBegrunnelse = yrkesskadeBegrunnelse,
            kodeverk = kodeverk,
            hoveddiagnose = hoveddiagnose,
            bidiagnoser = bidiagnoser,
        )
    }

    private fun erAndelNedsattNok(): Boolean {
        return erNedsettelseIArbeidsevneMerEnnHalvparten == true || erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true
    }

    fun erOppfylt(): Boolean {
        return erOppfyltSettBortIfraVissVarighet() && erNedsettelseIArbeidsevneAvEnVissVarighet == true
    }

    fun erOppfyltSettBortIfraVissVarighet(): Boolean {
        return harSkadeSykdomEllerLyte && erArbeidsevnenNedsatt == true && erSkadeSykdomEllerLyteVesentligdel == true && erAndelNedsattNok()
    }

    fun erKonsistentForSykdom(harYrkesskadeRegistrert: Boolean): Boolean {
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
        if (erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense != null && yrkesskadeBegrunnelse.isNullOrBlank()) {
            return false
        }
        return true
    }
}

class Yrkesskadevurdering(
    val id: Long? = null,
    val begrunnelse: String,
    val relevanteSaker: List<String>,
    val erÅrsakssammenheng: Boolean,
    val andelAvNedsettelsen: Prosent?
) {
    fun toDto(): YrkesskadevurderingDto {
        return YrkesskadevurderingDto(
            begrunnelse = begrunnelse,
            relevanteSaker = relevanteSaker,
            andelAvNedsettelsen = andelAvNedsettelsen?.prosentverdi(),
            erÅrsakssammenheng = erÅrsakssammenheng
        )
    }
}
