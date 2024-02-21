package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.NedreGrense
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.verdityper.Periode
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

data class RegistrertYrkesskade(val ref: String, val periode: Periode, val kilde: String)

data class SykdomsvurderingDto(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean,
    val erNedsettelseIArbeidsevneHøyereEnnNedreGrense: Boolean?,
    val nedreGrense: NedreGrense?,
    val yrkesskadevurdering: YrkesskadevurderingDto?
) {
    fun toYrkesskadevurdering(): Yrkesskadevurdering? {
        if (yrkesskadevurdering == null) {
            return null
        }
        return Yrkesskadevurdering(
            begrunnelse = begrunnelse,
            erÅrsakssammenheng = yrkesskadevurdering.erÅrsakssammenheng,
            skadetidspunkt = yrkesskadevurdering.skadetidspunkt,
            andelAvNedsettelse = yrkesskadevurdering.andelAvNedsettelse?.let(::Prosent),
        )
    }

    fun toSykdomsvurdering(): Sykdomsvurdering {
        return Sykdomsvurdering(
            begrunnelse = begrunnelse,
            dokumenterBruktIVurdering = dokumenterBruktIVurdering,
            erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = erNedsettelseIArbeidsevneHøyereEnnNedreGrense,
            nedreGrense = nedreGrense
        )
    }
}

data class YrkesskadevurderingDto(
    val erÅrsakssammenheng: Boolean,
    val skadetidspunkt: LocalDate?,
    val andelAvNedsettelse: Int?
)
