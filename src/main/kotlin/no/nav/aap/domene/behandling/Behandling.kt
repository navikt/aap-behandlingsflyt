package no.nav.aap.domene.behandling

import no.nav.aap.domene.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.StegStatus
import no.nav.aap.flyt.StegType
import no.nav.aap.flyt.Tilstand

class Behandling(val id: Long,
                 val type: BehandlingType,
                 private var status: Status = Status.OPPRETTET,
                 private var avklaringsbehov: List<Avklaringsbehov> = mutableListOf(),
                 private var stegHistorikk: List<StegTilstand> = mutableListOf()) {

    fun visit(stegTilstand: StegTilstand) {
        if (!stegTilstand.aktiv) {
            throw IllegalStateException("Utvikler feil, prøver legge til steg med aktivtflagg false.")
        }
        stegHistorikk.stream().filter { tilstand -> tilstand.aktiv }.forEach { tilstand -> tilstand.deaktiver() }
        stegHistorikk += stegTilstand
        stegHistorikk = stegHistorikk.sorted()
        validerStegTilstand()

        oppdaterStatus(stegTilstand)
    }

    fun avklaringsbehov(): List<Avklaringsbehov> {
        return avklaringsbehov
    }

    private fun oppdaterStatus(stegTilstand: StegTilstand) {
        val stegStatus = stegTilstand.tilstand.steg().status
        if (status != stegStatus) {
            status = stegStatus
        }
    }

    private fun validerStegTilstand() {
        if (stegHistorikk.isNotEmpty() && stegHistorikk.stream().noneMatch { tilstand -> tilstand.aktiv }) {
            throw IllegalStateException("Utvikler feil, mangler aktivt steg når steghistorikk ikke er tom.")
        }
    }

    fun aktivtSteg(): StegTilstand {
        return stegHistorikk.stream()
            .filter { tilstand -> tilstand.aktiv }
            .findAny()
            .orElse(
                StegTilstand(
                    tilstand = Tilstand(StegType.START_BEHANDLING, StegStatus.START)
                )
            )
    }

    fun leggTil(funnetAvklaringsbehov: List<Definisjon>) {
        funnetAvklaringsbehov.stream()
            .map { definisjon ->
                Avklaringsbehov(
                    definisjon,
                    no.nav.aap.domene.behandling.avklaringsbehov.Status.OPPRETTET
                )
            }
            .forEach { leggTil(behov = it) }
    }

    private fun leggTil(behov: Avklaringsbehov) {
        val relevantBehov = avklaringsbehov.stream().filter { it.definisjon == behov.definisjon }.findFirst()

        if (relevantBehov.isEmpty) {
            avklaringsbehov += behov
        } else {
            relevantBehov.get().reåpne()
        }
    }
}
