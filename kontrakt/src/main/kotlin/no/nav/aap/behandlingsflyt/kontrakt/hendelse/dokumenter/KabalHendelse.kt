package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime
import java.util.UUID

public sealed interface KabalHendelse : Melding

// https://github.com/navikt/kabal-api/blob/main/docs/schema/behandling-events.json
@JsonIgnoreProperties(ignoreUnknown = true)
public data class KabalHendelseKafkaMelding(
    val eventId: UUID,
    val kildeReferanse: String,
    val kilde: String,
    val kabalReferanse: String,
    val type: BehandlingEventType,
    val detaljer: BehandlingDetaljer
) {

    public fun tilKabalHendelseV0() =
        KabalHendelseV0(
            eventId = eventId,
            kildeReferanse = kildeReferanse,
            kilde = kilde,
            kabalReferanse = kabalReferanse,
            type = type,
            detaljer = detaljer
        )
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class KabalHendelseV0(
    val eventId: UUID,
    val kildeReferanse: String,
    val kilde: String,
    val kabalReferanse: String,
    val type: BehandlingEventType,
    val detaljer: BehandlingDetaljer
) : KabalHendelse

public enum class BehandlingEventType {
    KLAGEBEHANDLING_AVSLUTTET,
    ANKEBEHANDLING_OPPRETTET,
    ANKEBEHANDLING_AVSLUTTET,
    ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET,
    BEHANDLING_FEILREGISTRERT,
    BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET,
    OMGJOERINGSKRAVBEHANDLING_AVSLUTTET
}

// Container for optional detail types
public data class BehandlingDetaljer(
    val klagebehandlingAvsluttet: KlagebehandlingAvsluttetDetaljer? = null,
    val ankebehandlingOpprettet: AnkebehandlingOpprettetDetaljer? = null,
    val ankebehandlingAvsluttet: AnkebehandlingAvsluttetDetaljer? = null,
    val ankeITrygderettenbehandlingOpprettet: AnkeITrygderettenbehandlingOpprettetDetaljer? = null,
    val behandlingFeilregistrert: BehandlingFeilregistrertDetaljer? = null,
    val behandlingEtterTrygderettenOpphevetAvsluttet: BehandlingEtterTrygderettenOpphevetAvsluttetDetaljer? = null,
    val omgjoeringskravbehandlingAvsluttet: OmgjoeringskravbehandlingAvsluttetDetaljer? = null
)

// Detail types
public data class KlagebehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageUtfall,
    val journalpostReferanser: List<String>
)

public data class AnkebehandlingOpprettetDetaljer(
    val mottattKlageinstans: LocalDateTime
)

public data class AnkebehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: AnkeUtfall,
    val journalpostReferanser: List<String>
)

public data class AnkeITrygderettenbehandlingOpprettetDetaljer(
    val sendtTilTrygderetten: LocalDateTime,
    val utfall: TrygderettUtfall? = null
)

public data class BehandlingFeilregistrertDetaljer(
    val feilregistrert: LocalDateTime,
    val navIdent: String,
    val reason: String,
    val type: FeilregistrertType
)

public data class BehandlingEtterTrygderettenOpphevetAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: KlageUtfall,
    val journalpostReferanser: List<String>
)

public data class OmgjoeringskravbehandlingAvsluttetDetaljer(
    val avsluttet: LocalDateTime,
    val utfall: OmgjoeringsUtfall,
    val journalpostReferanser: List<String>
)

// Enums
public enum class KlageUtfall {
    TRUKKET, RETUR, OPPHEVET, MEDHOLD, DELVIS_MEDHOLD, STADFESTELSE, UGUNST, AVVIST
}

public enum class AnkeUtfall {
    TRUKKET, RETUR, OPPHEVET, MEDHOLD, DELVIS_MEDHOLD, STADFESTELSE, UGUNST, AVVIST, HEVET
}

public enum class TrygderettUtfall {
    TRUKKET, OPPHEVET, MEDHOLD, DELVIS_MEDHOLD, INNSTILLING_STADFESTELSE, INNSTILLING_AVVIST
}

public enum class FeilregistrertType {
    KLAGE, ANKE, ANKE_I_TRYGDERETTEN, BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET
}

public enum class OmgjoeringsUtfall {
    MEDHOLD_ETTER_FVL_35
}
