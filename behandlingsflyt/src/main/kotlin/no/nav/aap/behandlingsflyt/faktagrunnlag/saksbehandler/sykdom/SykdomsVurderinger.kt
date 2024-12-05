package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.YrkesskadevurderingDto
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId

class Sykdomsvurdering(
    internal val id: Long? = null,
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val harSkadeSykdomEllerLyte: Boolean,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
    val yrkesskadeBegrunnelse: String?,
    val erArbeidsevnenNedsatt: Boolean?,
    val kodeverk: String? = null,
    val diagnose: String? = null,
    val bidiagnoser: List<String>? = emptyList()
) {
    fun toDto(): SykdomsvurderingDto {
        return SykdomsvurderingDto(
            begrunnelse = begrunnelse,
            dokumenterBruktIVurdering = dokumenterBruktIVurdering,
            erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
            harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
            erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
            erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
            erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
            yrkesskadeBegrunnelse = yrkesskadeBegrunnelse,
            kodeverk = kodeverk,
            diagnose = diagnose,
            bidiagnoser = bidiagnoser
        )
    }
}

class Yrkesskadevurdering(
    internal val id: Long? = null,
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
