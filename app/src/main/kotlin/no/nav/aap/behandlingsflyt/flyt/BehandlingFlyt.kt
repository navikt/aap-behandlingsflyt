package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.verdityper.flyt.StegType
import java.util.*


/**
 * Holder styr på den definerte behandlingsflyten og regner ut hvilket steg det skal flyttes
 */
class BehandlingFlyt private constructor(
    private val flyt: List<Behandlingsflytsteg>,
    private val parent: BehandlingFlyt?
) {
    private var aktivtSteg: Behandlingsflytsteg? = flyt.firstOrNull()

    class Behandlingsflytsteg(
        val steg: FlytSteg,
        val kravliste: List<Informasjonskravkonstruktør>,
        val oppdaterFaktagrunnlag: Boolean
    )

    constructor(flyt: List<Behandlingsflytsteg>) : this(
        flyt = flyt,
        parent = null
    )

    fun faktagrunnlagForGjeldendeSteg(): List<Informasjonskravkonstruktør> {
        return aktivtSteg?.kravliste ?: emptyList()
    }

    fun faktagrunnlagFremTilOgMedGjeldendeSteg(): List<Informasjonskravkonstruktør> {
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

    fun frivilligeAvklaringsbehovRelevantForFlyten(aktivtSteg: StegType): List<Definisjon> {
        val stegene = stegene()
        return Definisjon.entries
            .filter { def ->
                stegene.contains(def.løsesISteg) && def.erFrivillig() && stegene.indexOf(aktivtSteg) >= stegene.indexOf(
                    def.løsesISteg
                )
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

        return utletTilbakeflytTilSteg(skalTilSteg)
    }

    fun skalTilStegForBehov(avklaringsbehov: List<Avklaringsbehov>): StegType? {
        return avklaringsbehov.map { it.løsesISteg() }.minWithOrNull(compareable())
    }

    internal fun skalTilStegForBehov(avklaringsbehov: Avklaringsbehov?): StegType? {
        if (avklaringsbehov == null) {
            return null
        }
        return skalTilStegForBehov(listOf(avklaringsbehov))
    }

    fun tilbakeflytEtterEndringer(oppdaterteGrunnlagstype: List<Informasjonskravkonstruktør>): BehandlingFlyt {
        val skalTilSteg =
            flyt.filter { it.kravliste.any { at -> oppdaterteGrunnlagstype.contains(at) } }.map { it.steg.type() }
                .minWithOrNull(compareable())

        return utletTilbakeflytTilSteg(skalTilSteg)
    }

    private fun utletTilbakeflytTilSteg(skalTilSteg: StegType?): BehandlingFlyt {
        if (skalTilSteg == null) {
            return BehandlingFlyt(emptyList())
        }

        val returflyt = flyt.slice(flyt.indexOfFirst { it.steg.type() == skalTilSteg }..flyt.indexOf(this.aktivtSteg))

        if (returflyt.size <= 1) {
            return BehandlingFlyt(emptyList())
        }

        return BehandlingFlyt(
            flyt = returflyt.reversed(),
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

    internal fun aktivtSteg(): FlytSteg {
        return requireNotNull(aktivtSteg).steg
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
        informasjonskrav: List<Informasjonskravkonstruktør> = emptyList()
    ): BehandlingFlytBuilder {
        if (buildt) {
            throw IllegalStateException("[Utvikler feil] Builder er allerede bygget")
        }
        if (StegType.UDEFINERT == steg.type()) {
            throw IllegalStateException("[Utvikler feil] StegType UDEFINERT er ugyldig å legge til i flyten")
        }
        this.flyt.add(BehandlingFlyt.Behandlingsflytsteg(steg, informasjonskrav, oppdaterFaktagrunnlag))
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
            Collections.unmodifiableList(flyt)
        )
    }
}
