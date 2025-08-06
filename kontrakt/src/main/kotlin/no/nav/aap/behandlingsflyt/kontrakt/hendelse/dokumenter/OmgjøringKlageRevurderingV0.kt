package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov

public sealed interface OmgjøringKlageRevurdering : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class OmgjøringKlageRevurderingV0(
    public val vurderingsbehov: List<Vurderingsbehov>,
    public val beskrivelse: String,
    public val kilde: Omgjøringskilde
) : OmgjøringKlageRevurdering

public enum class Omgjøringskilde {
    KLAGEINSTANS,
    KELVIN
}