package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.domene.behandling.EndringType
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import java.util.*


/**
 * Holder styr på den definerte behandlingsflyten og regner ut hvilket steg det skal flyttes
 */
class BehandlingFlyt(
    private val flyt: List<BehandlingSteg>,
    private val endringTilSteg: Map<EndringType, StegType>,
    private val onCompletedFlyt: (StegType) -> Any = { _ -> }
) {
    private var aktivtSteg: BehandlingSteg? = flyt.firstOrNull()

    fun forberedFlyt(aktivtSteg: StegType): BehandlingSteg {
        this.aktivtSteg = steg(aktivtSteg)
        return this.aktivtSteg!!
    }


    /**
     * Finner neste steget som skal prosesseres etter at nåværende er ferdig
     */
    fun neste(): BehandlingSteg? {
        if (this.flyt.isEmpty()) {
            return null
        }

        val nåværendeIndex = this.flyt.indexOfFirst { it === this.aktivtSteg }
        val iterator = this.flyt.listIterator(nåværendeIndex)
        iterator.next() // Er alltid nåværende steg

        if (iterator.hasNext()) {
            aktivtSteg = iterator.next()
            return aktivtSteg
        }
        onCompletedFlyt(flyt.last().type())

        return null
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

    private fun steg(nåværendeSteg: StegType): BehandlingSteg {
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

    /**
     * Brukes av APIet
     */
    fun stegene(): List<StegType> {
        return flyt.map { it.type() }
    }

    fun aktivtSteg(): BehandlingSteg? {
        return aktivtSteg
    }

    fun tilbakeflyt(avklaringsbehov: List<Avklaringsbehov>): BehandlingFlyt {
        val skalTilSteg = avklaringsbehov.map { it.løsesISteg() }.minWithOrNull(compareable())

        if (skalTilSteg == null) {
            return BehandlingFlyt(emptyList(), emptyMap())
        }

        val returflyt = flyt.slice(flyt.indexOfFirst { it.type() == skalTilSteg }..flyt.indexOf(this.aktivtSteg))

        if (returflyt.size <= 1) {
            return BehandlingFlyt(emptyList(), emptyMap())
        }

        return BehandlingFlyt(returflyt.reversed(), emptyMap(), this::forberedFlyt)
    }

    fun erTom(): Boolean {
        return flyt.isEmpty()
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
