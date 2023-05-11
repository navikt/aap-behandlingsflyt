package no.nav.aap.flyt

import no.nav.aap.domene.behandling.BehandlingType
import no.nav.aap.domene.behandling.EndringType
import java.util.Collections

/**
 * Holder styr på den definerte behandlingsflyten og regner ut hvilket steg det skal flyttes
 */
class BehandlingFlyt(private var behandlingType: BehandlingType,
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

        return StegType.AVSLUTT_BEHANDLING // Dette steget må da spesialhåndteres i Flytkontroller for å ikke gi evig løkke
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

    fun stegene(): List<StegType> {
        return this.flyt
    }

    fun erStegFør(stegA: StegType, stegB: StegType): Boolean {
        val aIndex = flyt.indexOf(stegA)
        val bIndex = flyt.indexOf(stegB)

        return aIndex < bIndex
    }

    fun forrige(nåværendeSteg: StegType): StegType {
        val nåværendeIndex = flyt.indexOf(nåværendeSteg)

        if (nåværendeIndex == -1) {
            throw IllegalStateException("[Utvikler feil] '" + nåværendeSteg + "' er ikke en del av den definerte prosessen")
        }

        val iterator = this.flyt.listIterator(nåværendeIndex)
        if (iterator.hasPrevious()) {
            return iterator.previous()
        }

        return StegType.START_BEHANDLING // Dette steget må da spesialhåndteres i Flytkontroller for å ikke gi evig løkke
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
        if (StegType.UDEFINERT == steg) {
            throw IllegalStateException("[Utvikler feil] Builder er allerede bygget")
        }
        this.flyt.add(steg)
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

        return BehandlingFlyt(
            this.behandlingType,
            Collections.unmodifiableList(flyt),
            Collections.unmodifiableMap(endringTilSteg)
        )
    }
}
