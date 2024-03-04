package no.nav.aap.behandlingsflyt.avklaringsbehov.flate

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.annotations.Response
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.arbeidsevne.FastsettArbeidsevneLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.bistand.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.meldeplikt.FritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.student.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.ForeslåVedtakLøsning
import java.util.*

@Response(statusCode = 202)
@JsonIgnoreProperties(ignoreUnknown = true)
data class LøsAvklaringsbehovPåBehandling(
    @JsonProperty(value = "referanse", required = true) val referanse: UUID,
    @JsonProperty(value = "behandlingVersjon", required = true, defaultValue = "0") val behandlingVersjon: Long,
    @JsonProperty(value = "behov", required = true) val behov: AvklaringsbehovLøsning,
    @JsonProperty(value = "avklarStudentLøsning") val avklarStudentLøsning: AvklarStudentLøsning?,
    @JsonProperty(value = "avklarSykepengerErstatningLøsning") val avklarSykepengerErstatningLøsning: AvklarSykepengerErstatningLøsning?,
    @JsonProperty(value = "avklarBistandsbehovLøsning") val avklarBistandsbehovLøsning: AvklarBistandsbehovLøsning?,
    @JsonProperty(value = "fritakMeldepliktLøsning") val fritakMeldepliktLøsning: FritakMeldepliktLøsning?,
    @JsonProperty(value = "fastsettArbeidsevneLøsning") val fastsettArbeidsevneLøsning: FastsettArbeidsevneLøsning?,
    @JsonProperty(value = "foreslåVedtakLøsning") val foreslåVedtakLøsning: ForeslåVedtakLøsning?,
    @JsonProperty(value = "fatteVedtakLøsning") val fatteVedtakLøsning: FatteVedtakLøsning?,
    @JsonProperty(value = "ingenEndringIGruppe") val ingenEndringIGruppe: Boolean,
) {
    init {
        //kun en av løsningene kan og MÅ være satt
        require(
            listOfNotNull(
                avklarStudentLøsning,
                avklarSykepengerErstatningLøsning,
                avklarBistandsbehovLøsning,
                fritakMeldepliktLøsning,
                fastsettArbeidsevneLøsning,
                foreslåVedtakLøsning,
                fatteVedtakLøsning
            ).size == 1
        ) { "Kun en av løsningene kan være satt" }
    }
    //hent den aktivt satte løsningen
    fun behov(): AvklaringsbehovLøsning {
        return listOf(
            avklarStudentLøsning,
            avklarSykepengerErstatningLøsning,
            avklarBistandsbehovLøsning,
            fritakMeldepliktLøsning,
            fastsettArbeidsevneLøsning,
            foreslåVedtakLøsning,
            fatteVedtakLøsning
        ).filterNotNull().first()
    }

}