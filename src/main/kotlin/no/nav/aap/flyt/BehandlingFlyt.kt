package no.nav.aap.flyt

import no.nav.aap.domene.behandling.BehandlingType
import no.nav.aap.domene.behandling.EndringType
import java.util.Collections

/**
 * Holder styr på den definerte behandlingsflyten og regner ut hvilket steg det skal flyttes
 */
class BehandlingFlyt(var behandlingType: BehandlingType,
                     private var flyt: List<StegType>,
                     private var endringTilSteg: Map<EndringType, StegType>) {

    /**
     * Finner neste steget som skal prosesseres etter at nåværende er ferdig
     */
    fun neste(nåværendeSteg: StegType): StegType {
        val nåværendeIndex = this.flyt.indexOf(nåværendeSteg)
        if (nåværendeIndex == -1) {
            throw IllegalStateException("[Utvikler feil] Nåværende steg '" + nåværendeSteg + "' er ikke en del av den definerte prosessen")
        }

        val iterator = this.flyt.listIterator(nåværendeIndex)
        iterator.next() // Er alltid nåværende steg

        if (iterator.hasNext()) {
            return iterator.next()
        }

        return StegType.AVSLUTT_BEHANDLING
    }

    /**
     * Avgjør hvilket steg prosessen skal fortsette fra. Hvis ingen endring så står pekeren stille
     */
    fun nesteEtterEndringer(nåværendeSteg: StegType, vararg endringer: EndringType): StegType {
        if (endringer.isNotEmpty()) {
            val nåværendeIndex = this.flyt.indexOf(nåværendeSteg)
            if (nåværendeIndex == -1) {
                throw IllegalStateException("[Utvikler feil] Nåværende steg '" + nåværendeSteg + "' er ikke en del av den definerte prosessen")
            }

            val endringsIndex = endringer.map { endring -> this.flyt.indexOf(endringTilSteg[endring]) }.min()

            if (endringsIndex < nåværendeIndex) {
                return this.flyt[endringsIndex]
            }
        }
        return nåværendeSteg
    }

    fun forType(): BehandlingType {
        return this.behandlingType;
    }
}

class BehandlingFlytBuilder(private var behandlingType: BehandlingType) {
    private var flyt: MutableList<StegType> = mutableListOf()
    private var endringTilSteg: MutableMap<EndringType, StegType> = mutableMapOf()
    private var buildt = false

    fun medSteg(steg: StegType, vararg endringer: EndringType): BehandlingFlytBuilder {
        if (buildt) {
            throw IllegalStateException("[Utvikler feil] Builder er allerede bygget")
        }
        if (!steg.tekniskSteg) {
            this.flyt.add(steg)
        }
        endringer.forEach { endring ->
            this.endringTilSteg[endring] = steg
        }
        return this
    }

    fun build(): BehandlingFlyt {
        if (buildt) {
            throw IllegalStateException("[Utvikler feil] Builder er allerede bygget")
        }
        buildt = true

        return BehandlingFlyt(this.behandlingType, Collections.unmodifiableList(flyt), Collections.unmodifiableMap(endringTilSteg))
    }
}
