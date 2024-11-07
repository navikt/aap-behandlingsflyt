package no.nav.aap.behandlingsflyt.kontrakt.statistikk

import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsBehovStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status as BehandlingsFlytBehandlingStatus

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
    val behandlingStatus: BehandlingsFlytBehandlingStatus,
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
        require(behandlingStatus == Status.AVSLUTTET || avsluttetBehandling == null)
        { "Om behandling er avsluttet, legg ved data om avsluttet behandling. Status er $behandlingStatus" }
        require(behandlingStatus != Status.AVSLUTTET || avsluttetBehandling != null)
        { "Om behandling ikke er avsluttet, ikke legg ved data om avsluttet behandling. Status er $behandlingStatus" }
    }
}

public enum class SakStatus {
    OPPRETTET,
    UTREDES,
    LØPENDE,
    AVSLUTTET
}

public data class AvklaringsbehovHendelse(
    val definisjon: Definisjon,
    val status: AvklaringsBehovStatus,
    val endringer: List<Endring>
)

/**
 * @param type Referer til type avklaringsbehov. Disse er definert i Definisjon.kt i aap-behandlingsflyt.
 */
public data class Definisjon(
    val type: String,
    val behovType: no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.BehovType,
    val løsesISteg: no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
)


public data class Endring(
    val status: AvklaringsBehovStatus,
    val tidsstempel: LocalDateTime,
    val frist: LocalDate? = null,
    val endretAv: String
)