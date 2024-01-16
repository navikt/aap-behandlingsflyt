package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AVKLAR_YRKESSKADE_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.Yrkesskadevurdering
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.Prosent
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_YRKESSKADE_KODE)
class AvklarYrkesskadeLøsning(
    @JsonProperty("yrkesskadevurdering", required = true) val yrkesskadevurdering: YrkesskadevurderingDto
) :
    AvklaringsbehovLøsning

data class YrkesskadevurderingDto(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erÅrsakssammenheng: Boolean,
    val skadetidspunkt: LocalDate?,
    val andelAvNedsettelse: Int?,
    val antattÅrligInntekt: Beløp?
){
    internal fun toYrkesskadevurdering(): Yrkesskadevurdering {
        return Yrkesskadevurdering(
            begrunnelse = begrunnelse,
                    dokumenterBruktIVurdering = dokumenterBruktIVurdering,
                    erÅrsakssammenheng = erÅrsakssammenheng,
                    skadetidspunkt = skadetidspunkt,
                    andelAvNedsettelse = andelAvNedsettelse?.let(::Prosent),
                    antattÅrligInntekt = antattÅrligInntekt
        )
    }
}
