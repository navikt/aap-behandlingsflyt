package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class SykdomGrunnlagDto(
    val skalVurdereYrkesskade: Boolean,
    val opplysninger: InnhentetSykdomsOpplysninger,
    val sykdomsvurdering: SykdomsvurderingDto?
)

data class InnhentetSykdomsOpplysninger(
    val oppgittYrkesskadeISøknad: Boolean,
    val innhentedeYrkesskader: List<RegistrertYrkesskade>,
)

data class RegistrertYrkesskade(val ref: String, val skadedato: LocalDate, val kilde: String)

data class SykdomsvurderingDto(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erArbeidsevnenNedsatt: Boolean?,
    val harSkadeSykdomEllerLyte: Boolean,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
    val erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
    val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
    val yrkesskadeBegrunnelse: String?
) {

    fun toSykdomsvurdering(): Sykdomsvurdering {
        return Sykdomsvurdering(
            begrunnelse = begrunnelse,
            dokumenterBruktIVurdering = dokumenterBruktIVurdering,
            erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
            harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
            erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
            erNedsettelseIArbeidsevneMerEnnHalvparten = this@SykdomsvurderingDto.erNedsettelseIArbeidsevneMerEnnHalvparten,
            erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
            yrkesskadeBegrunnelse = yrkesskadeBegrunnelse
        )
    }
}

data class YrkesskadevurderingDto(
    val begrunnelse: String,
    val relevanteSaker: List<String>,
    val andelAvNedsettelsen: Prosent?,
    val erÅrsakssammenheng: Boolean
) {
    fun toYrkesskadevurdering(): Yrkesskadevurdering {
        return Yrkesskadevurdering(
            begrunnelse = begrunnelse,
            relevanteSaker = relevanteSaker,
            erÅrsakssammenheng = erÅrsakssammenheng,
            andelAvNedsettelse = andelAvNedsettelsen
        )
    }
}
