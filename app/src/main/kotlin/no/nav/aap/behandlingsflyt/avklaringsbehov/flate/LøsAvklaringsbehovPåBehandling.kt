package no.nav.aap.behandlingsflyt.avklaringsbehov.flate

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.annotations.Response
import io.ktor.util.reflect.*
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.arbeidsevne.FastsettArbeidsevneLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.beregning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.bistand.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.meldeplikt.FritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.student.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.Arbeidsevne
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingDto
import java.util.*

@Response(statusCode = 202)
@JsonIgnoreProperties(ignoreUnknown = true)
data class LøsAvklaringsbehovPåBehandling(
    @JsonProperty(value = "referanse", required = true) val referanse: UUID,
    @JsonProperty(value = "behandlingVersjon", required = true, defaultValue = "0") val behandlingVersjon: Long,
    @JsonProperty(value = "studentvurdering") val studentvurdering: StudentVurdering?,
    @JsonProperty(value = "sykdomsvurderingDto") val sykdomsvurderingDto: SykdomsvurderingDto?,
    @JsonProperty(value = "sykepengerVurdering") val sykepengerVurdering: SykepengerVurdering?,
    @JsonProperty(value = "beregningVurdering") val beregningVurdering: BeregningVurdering?,
    @JsonProperty(value = "bistandVurdering") val bistandVurdering: BistandVurdering?,
    @JsonProperty(value = "fritaksvurdering") val fritaksvurdering: Fritaksvurdering?,
    @JsonProperty(value = "arbeidsevne") val arbeidsevne: Arbeidsevne?,
    @JsonProperty(value = "foreslåVedtakLøsning") val foreslåVedtakLøsning: ForeslåVedtakLøsning?,
    @JsonProperty(value = "fatteVedtakLøsning") val fatteVedtakLøsning: FatteVedtakLøsning?,
    @JsonProperty(value = "ingenEndringIGruppe") val ingenEndringIGruppe: Boolean?,
) {
    init {
        //kun en av løsningene kan og MÅ være satt
        require(
            listOfNotNull(
                studentvurdering,
                sykdomsvurderingDto,
                sykepengerVurdering,
                beregningVurdering,
                bistandVurdering,
                fritaksvurdering,
                arbeidsevne,
                foreslåVedtakLøsning,
                fatteVedtakLøsning
            ).size == 1
        ) { "Kun en av løsningene kan være satt" }
    }
    //hent den aktivt satte løsningen
    fun behov(): AvklaringsbehovLøsning {
        val behov = listOf(
            studentvurdering,
            sykepengerVurdering,
            bistandVurdering,
            sykdomsvurderingDto,
            fritaksvurdering,
            arbeidsevne,
            foreslåVedtakLøsning,
            beregningVurdering,
            fatteVedtakLøsning
        ).filterNotNull().first()

        return when(behov){
            is StudentVurdering -> return AvklarStudentLøsning(behov)
            is SykepengerVurdering -> return AvklarSykepengerErstatningLøsning(behov)
            is BistandVurdering -> return AvklarBistandsbehovLøsning(behov)
            is Fritaksvurdering -> return FritakMeldepliktLøsning(behov)
            is Arbeidsevne -> return FastsettArbeidsevneLøsning(behov)
            is SykdomsvurderingDto -> return AvklarSykdomLøsning(behov)
            is BeregningVurdering -> return FastsettBeregningstidspunktLøsning(behov)
            is AvklaringsbehovLøsning -> behov
            else -> throw IllegalArgumentException("Ukjent løsning")
        }
    }
}
