package no.nav.aap.flyt

import no.nav.aap.domene.behandling.EndringType
import no.nav.aap.flyt.steg.AvsluttBehandlingSteg
import no.nav.aap.flyt.steg.BehandlingSteg
import no.nav.aap.flyt.steg.StartBehandlingSteg
import java.util.Collections

/**
 * Holder styr på den definerte behandlingsflyten og regner ut hvilket steg det skal flyttes
 */
class BehandlingFlyt(private var flyt: List<BehandlingSteg>,
                     private var endringTilSteg: Map<EndringType, StegType>) {

    /**
     * Finner neste steget som skal prosesseres etter at nåværende er ferdig
     */
    fun neste(nåværendeSteg: StegType): BehandlingSteg {
        val nåværendeIndex = this.flyt.indexOfFirst { it.type() == nåværendeSteg }
        if (nåværendeIndex == -1) {
            throw IllegalStateException("[Utvikler feil] Nåværende steg '$nåværendeSteg' er ikke en del av den definerte prosessen")
        }

        val iterator = this.flyt.listIterator(nåværendeIndex)
        iterator.next() // Er alltid nåværende steg

        if (iterator.hasNext()) {
            return iterator.next()
        }

        return AvsluttBehandlingSteg() // Dette steget må da spesialhåndteres i Flytkontroller for å ikke gi evig løkke
    }

    /**
     * Avgjør hvilket steg prosessen skal fortsette fra. Hvis ingen endring så står pekeren stille
     */
    fun nesteEtterEndringer(nåværendeSteg: StegType, vararg endringer: EndringType): BehandlingSteg {
        if (endringer.isNotEmpty()) {
            val nåværendeIndex = flyt.indexOfFirst { it.type() == nåværendeSteg }
            if (nåværendeIndex == -1) {
                throw IllegalStateException("[Utvikler feil] Nåværende steg '" + nåværendeSteg + "' er ikke en del av den definerte prosessen")
            }

            val endringsIndex =
                endringer.map { endring -> flyt.indexOfFirst { it.type() == endringTilSteg[endring] } }.min()

            if (endringsIndex < nåværendeIndex) {
                return flyt[endringsIndex]
            }
        }
        return flyt[flyt.indexOfFirst { it.type() == nåværendeSteg }]
    }

    fun steg(nåværendeSteg: StegType): BehandlingSteg {
        return flyt[flyt.indexOfFirst { it.type() == nåværendeSteg }]
    }

    fun erStegFør(stegA: StegType, stegB: StegType): Boolean {
        val aIndex = flyt.indexOfFirst { it.type() == stegA }
        val bIndex = flyt.indexOfFirst { it.type() == stegB }

        return aIndex < bIndex
    }

    fun compareable(): StegComparator {
        return StegComparator(flyt)
    }

    fun erStegFørEllerLik(stegA: StegType, stegB: StegType): Boolean {
        val aIndex = flyt.indexOfFirst { it.type() == stegA }
        val bIndex = flyt.indexOfFirst { it.type() == stegB }

        return aIndex <= bIndex
    }

    fun forrige(nåværendeSteg: StegType): BehandlingSteg {
        val nåværendeIndex = flyt.indexOfFirst { it.type() == nåværendeSteg }

        if (nåværendeIndex == -1) {
            throw IllegalStateException("[Utvikler feil] '$nåværendeSteg' er ikke en del av den definerte prosessen")
        }

        val iterator = this.flyt.listIterator(nåværendeIndex)
        if (iterator.hasPrevious()) {
            return iterator.previous()
        }

        return StartBehandlingSteg() // Dette steget må da spesialhåndteres i Flytkontroller for å ikke gi evig løkke
    }
}

class StegComparator(private var flyt: List<BehandlingSteg>) : Comparator<StegType> {
    override fun compare(stegA: StegType?, stegB: StegType?): Int {
        val aIndex = flyt.indexOfFirst { it.type() == stegA }
        val bIndex = flyt.indexOfFirst { it.type() == stegB }

        return aIndex.compareTo(bIndex)
    }

}

class BehandlingFlytBuilder {
    private var flyt: MutableList<BehandlingSteg> = mutableListOf()
    private var endringTilSteg: MutableMap<EndringType, StegType> = mutableMapOf()
    private var buildt = false

    fun medSteg(steg: BehandlingSteg, vararg endringer: EndringType): BehandlingFlytBuilder {
        if (buildt) {
            throw IllegalStateException("[Utvikler feil] Builder er allerede bygget")
        }
        if (StegType.UDEFINERT == steg.type()) {
            throw IllegalStateException("[Utvikler feil] StegType UDEFINERT er ugyldig å legge til i flyten")
        }
        this.flyt.add(steg)
        endringer.forEach { endring ->
            this.endringTilSteg[endring] = steg.type()
        }
        return this
    }

    fun build(): BehandlingFlyt {
        if (buildt) {
            throw IllegalStateException("[Utvikler feil] Builder er allerede bygget")
        }
        buildt = true

        return BehandlingFlyt(
            Collections.unmodifiableList(flyt),
            Collections.unmodifiableMap(endringTilSteg)
        )
    }
}
