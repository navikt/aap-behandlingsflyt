package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.steg.StegStatus
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.Tilstand
import no.nav.aap.behandlingsflyt.sak.SakId
import java.time.LocalDateTime
import java.util.*

class Behandling(
    val id: BehandlingId,
    val referanse: UUID = UUID.randomUUID(),
    val sakId: SakId,
    val type: BehandlingType,
    private var status: Status = Status.OPPRETTET,
    private var årsaker: List<Årsak> = mutableListOf(),
    private val avklaringsbehovene: Avklaringsbehovene = Avklaringsbehovene(mutableListOf()),
    private var stegHistorikk: List<StegTilstand> = mutableListOf(),
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    val versjon: Long
) : Comparable<Behandling> {

    private val flyt: BehandlingFlyt = type.flyt()

    fun flyt(): BehandlingFlyt = flyt

    fun forberedtFlyt(): BehandlingFlyt {
        flyt.forberedFlyt(aktivtSteg())
        return flyt
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

    private fun validerStegTilstand() {
        if (stegHistorikk.isNotEmpty() && stegHistorikk.stream().noneMatch { tilstand -> tilstand.aktiv }) {
            throw IllegalStateException("Utvikler feil, mangler aktivt steg når steghistorikk ikke er tom.")
        }
    }

    private fun oppdaterStatus(stegTilstand: StegTilstand) {
        val stegStatus = stegTilstand.tilstand.steg().status
        if (status != stegStatus) {
            status = stegStatus
        }
    }

    fun status(): Status = status

    fun settPåVent() {
        status = Status.PÅ_VENT
    }

    fun stegHistorikk(): List<StegTilstand> = stegHistorikk.toList()

    private fun aktivtStegTilstand(): StegTilstand {
        return stegHistorikk.stream()
            .filter { tilstand -> tilstand.aktiv }
            .findAny()
            .orElse(
                StegTilstand(
                    tilstand = Tilstand(StegType.START_BEHANDLING, StegStatus.START)
                )
            )
    }

    fun aktivtSteg(): StegType {
        return stegHistorikk.stream()
            .filter { tilstand -> tilstand.aktiv }
            .findAny()
            .orElse(
                StegTilstand(
                    tilstand = Tilstand(StegType.START_BEHANDLING, StegStatus.START)
                )
            ).tilstand.steg()
    }

    fun årsaker(): List<Årsak> = årsaker.toList()

    fun leggTil(funnetAvklaringsbehov: List<Definisjon>) {
        avklaringsbehovene.leggTil(funnetAvklaringsbehov, aktivtSteg())
    }

    override fun compareTo(other: Behandling): Int {
        return this.opprettetTidspunkt.compareTo(other.opprettetTidspunkt)
    }
}