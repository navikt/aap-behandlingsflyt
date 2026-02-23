package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import java.util.UUID

public sealed interface OmgjøringKlageRevurdering : Melding {
    public val beskrivelse: String
    public val vurderingsbehov: List<Vurderingsbehov>
    public val kilde: Omgjøringskilde
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class OmgjøringKlageRevurderingV0(
    public override val vurderingsbehov: List<Vurderingsbehov>,
    public override val beskrivelse: String,
    public override val kilde: Omgjøringskilde
) : OmgjøringKlageRevurdering

@JsonIgnoreProperties(ignoreUnknown = true)
public data class OmgjøringKlageRevurderingV1(
    public override val vurderingsbehov: List<Vurderingsbehov>,
    public override val beskrivelse: String,
    public override val kilde: Omgjøringskilde,
    public val kildeReferanse: UUID
) : OmgjøringKlageRevurdering

public enum class Omgjøringskilde {
    KLAGEINSTANS,
    KELVIN
}