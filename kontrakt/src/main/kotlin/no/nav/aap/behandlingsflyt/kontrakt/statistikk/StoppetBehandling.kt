package no.nav.aap.behandlingsflyt.kontrakt.statistikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * @param saksnummer Saksnummer.
 * @param behandlingReferanse Behandlingsreferanse
 * @param relatertBehandling Hvis behandlingen har oppsått med bakgrunn i en annen, skal den foregående behandlingen refereres til her. Dette er tolket som forrige behandling på samme sak.
 * @param mottattTid Dato for første søknad mottatt for behandlingen.
 * @param status Behandlingstatus. Ikke det samme som sakstatus.
 * @param identerForSak Identer på sak. Brukes for å filtrere kode 6-personer.
 */
public data class StoppetBehandling(
    val saksnummer: String,
    val sakStatus: SakStatus,
    val behandlingReferanse: UUID,
    val relatertBehandling: UUID? = null,
    val behandlingOpprettetTidspunkt: LocalDateTime,
    val mottattTid: LocalDateTime,
    val status: BehandlingStatus,
    val behandlingType: TypeBehandling,
    val ident: String,
    val versjon: String,
    val avklaringsbehov: List<AvklaringsbehovHendelse>,
    val hendelsesTidspunkt: LocalDateTime,
    val avsluttetBehandling: AvsluttetBehandlingDTO? = null,
    val identerForSak: List<String> = listOf(),
) {
    init {
        require(ident.isNotEmpty())
        require(status == BehandlingStatus.AVSLUTTET || avsluttetBehandling == null)
        { "Om behandling er avsluttet, legg ved data om avsluttet behandling. Status er $status" }
        require(status != BehandlingStatus.AVSLUTTET || avsluttetBehandling != null)
        { "Om behandling ikke er avsluttet, ikke legg ved data om avsluttet behandling. Status er $status" }
    }
}

public enum class SakStatus {
    OPPRETTET,
    UTREDES,
    LØPENDE,
    AVSLUTTET
}

public enum class BehandlingStatus {
    OPPRETTET,
    UTREDES,
    IVERKSETTES,
    AVSLUTTET;
}


public enum class TypeBehandling {
    Førstegangsbehandling,
    Revurdering,
    Tilbakekreving,
    Klage;
}


public data class AvklaringsbehovHendelse(
    val definisjon: Definisjon,
    val status: EndringStatus,
    val endringer: List<Endring>
)

/**
 * @param type Referer til type avklaringsbehov. Disse er definert i Definisjon.kt i aap-behandlingsflyt.
 */
public data class Definisjon(
    val type: String,
    val behovType: BehovType,
    val løsesISteg: StegType
)


public enum class EndringStatus {
    OPPRETTET,
    AVSLUTTET,
    TOTRINNS_VURDERT,
    SENDT_TILBAKE_FRA_BESLUTTER,
    KVALITETSSIKRET,
    SENDT_TILBAKE_FRA_KVALITETSSIKRER,
    AVBRUTT
}

public data class Endring(
    val status: EndringStatus,
    val tidsstempel: LocalDateTime,
    val frist: LocalDate? = null,
    val endretAv: String
)

public enum class BehovType {
    MANUELT_PÅKREVD, MANUELT_FRIVILLIG, VENTEPUNKT,
}

public enum class StegType(
    public val status: BehandlingStatus
) {
    START_BEHANDLING(
        status = BehandlingStatus.OPPRETTET
    ),
    VURDER_ALDER(
        status = BehandlingStatus.UTREDES,
    ),
    VURDER_LOVVALG(
        status = BehandlingStatus.UTREDES
    ),
    VURDER_MEDLEMSKAP(
        status = BehandlingStatus.UTREDES
    ),
    AVKLAR_STUDENT(
        status = BehandlingStatus.UTREDES
    ),
    VURDER_BISTANDSBEHOV(
        status = BehandlingStatus.UTREDES
    ),
    VURDER_SYKEPENGEERSTATNING(
        status = BehandlingStatus.UTREDES
    ),
    FRITAK_MELDEPLIKT(
        status = BehandlingStatus.UTREDES
    ),
    KVALITETSSIKRING(
        status = BehandlingStatus.UTREDES,
    ),
    BARNETILLEGG(
        status = BehandlingStatus.UTREDES
    ),
    AVKLAR_SYKDOM(
        status = BehandlingStatus.UTREDES
    ),
    FASTSETT_ARBEIDSEVNE(
        status = BehandlingStatus.UTREDES
    ),
    FASTSETT_BEREGNINGSTIDSPUNKT(
        status = BehandlingStatus.UTREDES
    ),
    FASTSETT_GRUNNLAG(
        status = BehandlingStatus.UTREDES
    ),
    VIS_GRUNNLAG(
        status = BehandlingStatus.UTREDES
    ),
    FASTSETT_UTTAK(
        status = BehandlingStatus.UTREDES
    ),
    SAMORDNING_GRADERING(
        status = BehandlingStatus.UTREDES
    ),
    DU_ER_ET_ANNET_STED(
        status = BehandlingStatus.UTREDES
    ),
    BEREGN_TILKJENT_YTELSE(
        status = BehandlingStatus.UTREDES
    ),
    SIMULERING(
        status = BehandlingStatus.UTREDES
    ),
    FORESLÅ_VEDTAK(
        status = BehandlingStatus.UTREDES
    ),
    FATTE_VEDTAK(
        status = BehandlingStatus.UTREDES
    ),
    BREV(
        status = BehandlingStatus.IVERKSETTES,
    ),
    IVERKSETT_VEDTAK(
        status = BehandlingStatus.IVERKSETTES
    )
}