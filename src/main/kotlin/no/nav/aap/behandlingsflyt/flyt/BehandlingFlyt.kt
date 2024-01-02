package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.EndringType
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import java.util.*


/**
 * Holder styr på den definerte behandlingsflyten og regner ut hvilket steg det skal flyttes
 */
class BehandlingFlyt private constructor(
    private val flyt: List<Behandlingsflytsteg>,
    private val endringTilSteg: Map<EndringType, StegType>,
    private val parent: BehandlingFlyt?
) {
    private var aktivtSteg: Behandlingsflytsteg? = flyt.firstOrNull()

    class Behandlingsflytsteg(
        val steg: FlytSteg,
        val kravliste: List<Grunnlagkonstruktør>,
        val oppdaterFaktagrunnlag: Boolean
    )

    constructor(
        flyt: List<Behandlingsflytsteg>,
        endringTilSteg: Map<EndringType, StegType>,
    ) : this(
        flyt = flyt,
        endringTilSteg = endringTilSteg,
        parent = null
    )

    fun faktagrunnlagForGjeldendeSteg(): List<Grunnlagkonstruktør> {
        return aktivtSteg?.kravliste ?: emptyList()
    }

    fun faktagrunnlagFremTilOgMedGjeldendeSteg(): List<Grunnlagkonstruktør> {
        if (aktivtSteg?.oppdaterFaktagrunnlag != true) {
            return emptyList()
        }

        return flyt
            .takeWhile { it != aktivtSteg }
            .flatMap { it.kravliste }
            .plus(faktagrunnlagForGjeldendeSteg())
    }

    fun forberedFlyt(aktivtSteg: StegType): FlytSteg {
        return forberedFlyt(steg(aktivtSteg)).steg
    }

    private fun forberedFlyt(aktivtSteg: Behandlingsflytsteg): Behandlingsflytsteg {
        this.aktivtSteg = aktivtSteg
        parent?.forberedFlyt(aktivtSteg)
        return aktivtSteg
    }

    /**
     * Finner neste steget som skal prosesseres etter at nåværende er ferdig
     */
    fun neste(): FlytSteg? {
        if (this.flyt.isEmpty()) {
            return null
        }

        val nåværendeIndex = this.flyt.indexOfFirst { it === this.aktivtSteg }
        val iterator = this.flyt.listIterator(nåværendeIndex)
        iterator.next() // Er alltid nåværende steg

        if (iterator.hasNext()) {
            val nesteSteg = iterator.next()
            return forberedFlyt(nesteSteg).steg
        }

        return null
    }

    internal fun validerPlassering(skulleVærtIStegType: StegType) {
        val aktivtStegType = requireNotNull(aktivtSteg).steg.type()
        require(skulleVærtIStegType == aktivtStegType)
    }

    /**
     * Avgjør hvilket steg prosessen skal fortsette fra. Hvis ingen endring så står pekeren stille
     */
    fun nesteEtterEndringer(nåværendeSteg: StegType, vararg endringer: EndringType): FlytSteg {
        if (endringer.isNotEmpty()) {
            val nåværendeIndex = flyt.indexOfFirst { it.steg.type() == nåværendeSteg }
            if (nåværendeIndex == -1) {
                throw IllegalStateException("[Utvikler feil] Nåværende steg '" + nåværendeSteg + "' er ikke en del av den definerte prosessen")
            }

            val endringsIndex =
                endringer.map { endring -> flyt.indexOfFirst { it.steg.type() == endringTilSteg[endring] } }.min()

            if (endringsIndex < nåværendeIndex) {
                return flyt[endringsIndex].steg
            }
        }
        return flyt[flyt.indexOfFirst { it.steg.type() == nåværendeSteg }].steg
    }

    private fun steg(nåværendeSteg: StegType): Behandlingsflytsteg {
        return flyt[flyt.indexOfFirst { it.steg.type() == nåværendeSteg }]
    }

    fun erStegFør(stegA: StegType, stegB: StegType): Boolean {
        val aIndex = flyt.indexOfFirst { it.steg.type() == stegA }
        val bIndex = flyt.indexOfFirst { it.steg.type() == stegB }

        return aIndex < bIndex
    }

    fun compareable(): StegComparator {
        return StegComparator(flyt)
    }

    internal fun erStegFørEllerLik(stegA: StegType, stegB: StegType): Boolean {
        val aIndex = flyt.indexOfFirst { it.steg.type() == stegA }
        val bIndex = flyt.indexOfFirst { it.steg.type() == stegB }

        return aIndex <= bIndex
    }

    /**
     * Brukes av APIet
     */
    fun stegene(): List<StegType> {
        return flyt.map { it.steg.type() }
    }

    fun frivilligeAvklaringsbehovRelevantForFlyten(): List<Definisjon> {
        val stegene = stegene()
        return Definisjon.entries
            .filter { def -> stegene.contains(def.løsesISteg) && def.erFrivillig()
        }
    }

    internal fun tilbakeflyt(avklaringsbehov: Avklaringsbehov?): BehandlingFlyt {
        if (avklaringsbehov == null) {
            return tilbakeflyt(listOf())
        }
        return tilbakeflyt(listOf(avklaringsbehov))
    }

    internal fun tilbakeflyt(avklaringsbehov: List<Avklaringsbehov>): BehandlingFlyt {
        val skalTilSteg = skalTilStegForBehov(avklaringsbehov)

        if (skalTilSteg == null) {
            return BehandlingFlyt(emptyList(), emptyMap())
        }

        val returflyt = flyt.slice(flyt.indexOfFirst { it.steg.type() == skalTilSteg }..flyt.indexOf(this.aktivtSteg))

        if (returflyt.size <= 1) {
            return BehandlingFlyt(emptyList(), emptyMap())
        }

        return BehandlingFlyt(
            flyt = returflyt.reversed(),
            endringTilSteg = emptyMap(),
            parent = this
        )
    }

    internal fun skalTilStegForBehov(avklaringsbehov: List<Avklaringsbehov>): StegType? {
        return avklaringsbehov.map { it.løsesISteg() }.minWithOrNull(compareable())
    }

    internal fun skalTilStegForBehov(avklaringsbehov: Avklaringsbehov?): StegType? {
        if (avklaringsbehov == null) {
            return null
        }
        return skalTilStegForBehov(listOf(avklaringsbehov))
    }

    fun tilbakeflytEtterEndringer(oppdaterteGrunnlagstype: List<Grunnlagkonstruktør>): BehandlingFlyt {
        val skalTilSteg =
            flyt.filter { it.kravliste.any { at -> oppdaterteGrunnlagstype.contains(at) } }.map { it.steg.type() }
                .minWithOrNull(compareable())

        if (skalTilSteg == null) {
            return BehandlingFlyt(emptyList(), emptyMap())
        }

        val returflyt = flyt.slice(flyt.indexOfFirst { it.steg.type() == skalTilSteg }..flyt.indexOf(this.aktivtSteg))

        if (returflyt.size <= 1) {
            return BehandlingFlyt(emptyList(), emptyMap())
        }

        return BehandlingFlyt(
            flyt = returflyt.reversed(),
            endringTilSteg = emptyMap(),
            parent = this
        )
    }

    fun erTom(): Boolean {
        return flyt.isEmpty()
    }

    fun gjenståendeStegIAktivGruppe(): List<StegType> {
        val aktivtStegType = requireNotNull(aktivtSteg).steg.type()
        return stegene().filter { it.gruppe == aktivtStegType.gruppe && !erStegFørEllerLik(it, aktivtStegType) }
    }

    internal fun aktivtStegType(): StegType {
        return requireNotNull(aktivtSteg).steg.type()
    }
}

class StegComparator(private var flyt: List<BehandlingFlyt.Behandlingsflytsteg>) : Comparator<StegType> {
    override fun compare(stegA: StegType?, stegB: StegType?): Int {
        val aIndex = flyt.indexOfFirst { it.steg.type() == stegA }
        val bIndex = flyt.indexOfFirst { it.steg.type() == stegB }

        return aIndex.compareTo(bIndex)
    }

}

class BehandlingFlytBuilder {
    private val flyt: MutableList<BehandlingFlyt.Behandlingsflytsteg> = mutableListOf()
    private val endringTilSteg: MutableMap<EndringType, StegType> = mutableMapOf()
    private var oppdaterFaktagrunnlag = true
    private var buildt = false

    fun medSteg(
        steg: FlytSteg,
        vararg endringer: EndringType,
        informasjonskrav: List<Grunnlagkonstruktør> = emptyList()
    ): BehandlingFlytBuilder {
        if (buildt) {
            throw IllegalStateException("[Utvikler feil] Builder er allerede bygget")
        }
        if (StegType.UDEFINERT == steg.type()) {
            throw IllegalStateException("[Utvikler feil] StegType UDEFINERT er ugyldig å legge til i flyten")
        }
        this.flyt.add(BehandlingFlyt.Behandlingsflytsteg(steg, informasjonskrav.toList(), oppdaterFaktagrunnlag))
        endringer.forEach { endring ->
            this.endringTilSteg[endring] = steg.type()
        }
        return this
    }

    fun sluttÅOppdatereFaktagrunnlag(): BehandlingFlytBuilder {
        oppdaterFaktagrunnlag = false
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
