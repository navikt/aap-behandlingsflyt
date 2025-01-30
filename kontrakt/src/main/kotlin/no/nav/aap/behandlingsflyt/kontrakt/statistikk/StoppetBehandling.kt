package no.nav.aap.behandlingsflyt.kontrakt.statistikk

import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDateTime
import java.util.*
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status as BehandlingsFlytBehandlingStatus
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status as SakStatus

/**
 * @param saksnummer Saksnummer.
 * @param behandlingReferanse Behandlingsreferanse
 * @param relatertBehandling Hvis behandlingen har oppsått med bakgrunn i en annen, skal den foregående behandlingen refereres til her. Dette er tolket som forrige behandling på samme sak.
 * @param mottattTid Dato for første søknad mottatt for behandlingen.
 * @param behandlingStatus Behandlingstatus. Ikke det samme som sakstatus.
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
    val soknadsFormat: Kanal = Kanal.DIGITAL,
    val ident: String,
    val versjon: String,
    val årsakTilBehandling: List<ÅrsakTilBehandling>,
    val avklaringsbehov: List<AvklaringsbehovHendelseDto>,
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


/**
 * Eksponert versjon av [no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling] til kontrakt.
 */
public enum class ÅrsakTilBehandling {
    SØKNAD,
    AKTIVITETSMELDING,
    MELDEKORT,
    LEGEERKLÆRING,
    AVVIST_LEGEERKLÆRING,
    DIALOGMELDING,
    G_REGULERING,
    REVURDER_MEDLEMSKAP,
    REVURDER_YRKESSKADE,
    REVURDER_BEREGNING
}
