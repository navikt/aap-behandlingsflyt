package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.NedreGrense
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
    val erNedsettelseIArbeidsevneHøyereEnnNedreGrense: Boolean?,
    val nedreGrense: NedreGrense?,
    val nedsattArbeidsevneDato: LocalDate?,
    val yrkesskadevurdering: YrkesskadevurderingDto?
) {

    fun toYrkesskadevurdering() = yrkesskadevurdering?.let {
        Yrkesskadevurdering(begrunnelse, it.erÅrsakssammenheng, nedsattArbeidsevneDato, Prosent.`50_PROSENT`)
    }

    fun toSykdomsvurdering(): Sykdomsvurdering {
        return Sykdomsvurdering(
            begrunnelse = begrunnelse,
            dokumenterBruktIVurdering = dokumenterBruktIVurdering,
            erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
            harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
            erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = erNedsettelseIArbeidsevneHøyereEnnNedreGrense,
            nedreGrense = nedreGrense,
            nedsattArbeidsevneDato = nedsattArbeidsevneDato
        )
    }
}

data class YrkesskadevurderingDto(
    val erÅrsakssammenheng: Boolean
)
