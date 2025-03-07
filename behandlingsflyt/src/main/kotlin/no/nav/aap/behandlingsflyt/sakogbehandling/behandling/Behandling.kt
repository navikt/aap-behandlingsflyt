package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

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
    private var stegHistorikk: List<StegTilstand> = mutableListOf(),
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    val versjon: Long
) : Comparable<Behandling> {

    fun typeBehandling(): TypeBehandling = typeBehandling

    fun flytKontekst(): FlytKontekst {
        return FlytKontekst(sakId, id, typeBehandling)
    }

    fun visit(stegTilstand: StegTilstand) {
        if (!stegTilstand.aktiv) {
            throw IllegalStateException("Utvikler feil, prøver legge til steg med aktivtflagg false.")
        }
        if (stegHistorikk.isEmpty() || aktivtStegTilstand() != stegTilstand) {
            stegHistorikk.stream().filter { tilstand -> tilstand.aktiv }.forEach { tilstand -> tilstand.deaktiver() }
            stegHistorikk += stegTilstand
            stegHistorikk = stegHistorikk.sorted()
        }
        validerStegTilstand()

        oppdaterStatus(stegTilstand)
    }

    fun årsaker(): List<Årsak> {
        return årsaker.toList()
    }

    private fun validerStegTilstand() {
        if (stegHistorikk.isNotEmpty() && stegHistorikk.stream().noneMatch { tilstand -> tilstand.aktiv }) {
            throw IllegalStateException("Utvikler feil, mangler aktivt steg når steghistorikk ikke er tom.")
        }
    }

    private fun oppdaterStatus(stegTilstand: StegTilstand) {
        val stegStatus = stegTilstand.steg().status
        if (status != stegStatus) {
            status = stegStatus
        }
    }

    fun status(): Status = status

    fun stegHistorikk(): List<StegTilstand> = stegHistorikk.toList()

    fun harIkkeVærtAktivitetIDetSiste(): Boolean {
        return aktivtStegTilstand().tidspunkt().isBefore(LocalDateTime.now().minusMinutes(15))
    }

    fun aktivtSteg(): StegType {
        return aktivtStegTilstand().steg()
    }

    private fun aktivtStegTilstand(): StegTilstand {
        return stegHistorikk.stream()
            .filter { tilstand -> tilstand.aktiv }
            .findAny()
            .orElse(
                StegTilstand(
                    stegType = StegType.START_BEHANDLING,
                    stegStatus = StegStatus.START,
                    aktiv = true
                )
            )
    }

    override fun compareTo(other: Behandling): Int {
        return this.opprettetTidspunkt.compareTo(other.opprettetTidspunkt)
    }

    override fun toString(): String {
        return "Behandling(id=$id, referanse=$referanse, sakId=$sakId, typeBehandling=$typeBehandling, status=$status, opprettetTidspunkt=$opprettetTidspunkt, versjon=$versjon)"
    }
}