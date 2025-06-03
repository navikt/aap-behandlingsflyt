package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.flyt
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.time.LocalDateTime

class Behandling(
    val id: BehandlingId,
    val forrigeBehandlingId: BehandlingId?,
    val referanse: BehandlingReferanse = BehandlingReferanse(),
    val sakId: SakId,
    private val typeBehandling: TypeBehandling,
    private var status: Status = Status.OPPRETTET,
    private var årsaker: List<Årsak> = mutableListOf(),
    private var stegTilstand: StegTilstand? = null,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    val versjon: Long
) : Comparable<Behandling> {

    fun flyt(): BehandlingFlyt = typeBehandling.flyt()

    fun typeBehandling(): TypeBehandling = typeBehandling

    fun flytKontekst(): FlytKontekst {
        return FlytKontekst(
            sakId = sakId,
            behandlingId = id,
            forrigeBehandlingId = forrigeBehandlingId,
            behandlingType = typeBehandling
        )
    }

    fun harBehandlingenStartet(): Boolean {
        return stegTilstand != null
    }

    fun oppdaterSteg(nyStegTilstand: StegTilstand) {
        if (!nyStegTilstand.aktiv) {
            throw IllegalStateException("Utvikler feil, prøver legge til steg med aktivtflagg false.")
        }
        stegTilstand = nyStegTilstand

        oppdaterStatus(nyStegTilstand)
    }

    fun årsaker(): List<Årsak> {
        return årsaker.toList()
    }

    private fun oppdaterStatus(stegTilstand: StegTilstand) {
        val stegStatus = stegTilstand.steg().status
        if (status != stegStatus) {
            status = stegStatus
        }
    }

    fun status(): Status = status

    fun harIkkeVærtAktivitetIDetSiste(): Boolean {
        return aktivtStegTilstand().tidspunkt().isBefore(LocalDateTime.now().minusMinutes(15))
    }

    fun aktivtSteg(): StegType {
        return aktivtStegTilstand().steg()
    }

    fun aktivtStegTilstand(): StegTilstand {
        return stegTilstand ?: StegTilstand(
            stegType = StegType.START_BEHANDLING,
            stegStatus = StegStatus.START,
            aktiv = true
        )
    }

    override fun compareTo(other: Behandling): Int {
        return this.opprettetTidspunkt.compareTo(other.opprettetTidspunkt)
    }

    override fun toString(): String {
        return "Behandling(id=$id, referanse=$referanse, sakId=$sakId, typeBehandling=$typeBehandling, status=$status, opprettetTidspunkt=$opprettetTidspunkt, versjon=$versjon)"
    }
}