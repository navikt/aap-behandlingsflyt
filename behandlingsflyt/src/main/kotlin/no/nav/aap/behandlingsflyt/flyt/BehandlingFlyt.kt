package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import java.util.*


/**
 * Holder styr på den definerte behandlingsflyten og regner ut hvilket steg det skal flyttes
 */
class BehandlingFlyt private constructor(
    private val flyt: List<Behandlingsflytsteg>,
    private val vurderingsbehov: Map<Vurderingsbehov, List<StegType>>,
    private val parent: BehandlingFlyt?
) {
    private var aktivtSteg: Behandlingsflytsteg? = flyt.firstOrNull()

    /**
     * @param oppdaterFaktagrunnlag Om faktagrunnlaget skal oppdateres for dette steget.
     */
    class Behandlingsflytsteg(
        val steg: FlytSteg,
        val kravliste: List<Informasjonskravkonstruktør>,
        val oppdaterFaktagrunnlag: Boolean
    ) {
        override fun toString(): String {
            return "Behandlingsflytsteg(kravliste=${kravliste.map { it.navn }}, steg=${steg.type()}, oppdaterFaktagrunnlag=$oppdaterFaktagrunnlag)"
        }
    }

    constructor(flyt: List<Behandlingsflytsteg>, vurderingsbehov: Map<Vurderingsbehov, List<StegType>>) : this(
        flyt = flyt,
        vurderingsbehov = vurderingsbehov,
        parent = null
    )

    constructor(flyt: List<Behandlingsflytsteg>) : this(
        flyt = flyt,
        vurderingsbehov = emptyMap(),
        parent = null
    )

    fun faktagrunnlagForGjeldendeSteg(): List<Pair<StegType, Informasjonskravkonstruktør>> {
        return aktivtSteg
            ?.let { steg -> steg.kravliste.map { steg.steg.type() to it } }
            .orEmpty()
    }

    /**
     * Henter alle faktagrunnlag strengt før (altså, ikke inklusivt) gjeldende steg.
     *
     * @return Alle faktagrunnlag, i form av en liste av [Informasjonskravkonstruktør].
     */
    fun alleFaktagrunnlagFørGjeldendeSteg(): List<Pair<StegType, Informasjonskravkonstruktør>> {
        if (aktivtSteg?.oppdaterFaktagrunnlag != true) {
            return emptyList()
        }

        return flyt
            .takeWhile { it != aktivtSteg }
            .flatMap { steg -> steg.kravliste.map { steg.steg.type() to it } }
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
        { "Aktivt steg $aktivtStegType er ikke lik det forventede steget $skulleVærtIStegType" }
    }

    private fun steg(nåværendeSteg: StegType): Behandlingsflytsteg {
        return try {
            flyt[flyt.indexOfFirst { it.steg.type() == nåværendeSteg }]
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalArgumentException("Steg $nåværendeSteg finnes ikke i flyten", e)
        }
    }

    fun erStegFør(stegA: StegType, stegB: StegType): Boolean {
        val aIndex = flyt.indexOfFirst { it.steg.type() == stegA }
        val bIndex = flyt.indexOfFirst { it.steg.type() == stegB }

        return aIndex < bIndex
    }

    /** Sorter avklaringsbehov i samme rekkefølgen som stegene behovet løses i.
     * Steg som løses i ukjente steg, plasseres bakerst. */
    val stegComparator: Comparator<StegType> by lazy {
        val rekkefølge = flyt.mapIndexed { i, steg -> steg.steg.type() to i }.toMap()
        compareBy { rekkefølge[it] ?: rekkefølge.size }
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
            return tilbakeflyt(emptyList())
        }
        return tilbakeflyt(listOf(avklaringsbehov))
    }

    internal fun tilbakeflyt(avklaringsbehov: List<Avklaringsbehov>): BehandlingFlyt {
        val skalTilSteg = skalTilStegForBehov(avklaringsbehov)

        return utledTilbakeflytTilSteg(skalTilSteg)
    }

    fun skalTilStegForBehov(avklaringsbehov: List<Avklaringsbehov>): StegType? {
        return avklaringsbehov.map { it.løsesISteg() }.minWithOrNull(stegComparator)
    }

    internal fun skalTilStegForBehov(avklaringsbehov: Avklaringsbehov?): StegType? {
        if (avklaringsbehov == null) {
            return null
        }
        return skalTilStegForBehov(listOf(avklaringsbehov))
    }

    fun tilbakeflytEtterEndringer(
        oppdaterteGrunnlagstype: List<Informasjonskravkonstruktør>,
        nyeVurderingsbehov: List<Vurderingsbehov>? = null
    ): BehandlingFlyt {
        val tidligsteStegForVurderingsbehov =
            nyeVurderingsbehov?.flatMap { vurderingsbehov[it].orEmpty() }
                // Skal ikke kunne flyttes tilbake til steg med status OPPRETTET
                ?.minus(StegType.entries.filter { it.status == Status.OPPRETTET })
                ?.minWithOrNull(stegComparator)

        val skalTilSteg =
            flyt.filter { it.kravliste.any { at -> oppdaterteGrunnlagstype.contains(at) } }
                .map { it.steg.type() }
                .minus(StegType.entries.filter { it.status == Status.OPPRETTET })
                .minWithOrNull(stegComparator)

        val tidligsteSteg = listOfNotNull(tidligsteStegForVurderingsbehov, skalTilSteg).minWithOrNull(stegComparator)

        return utledTilbakeflytTilSteg(tidligsteSteg)
    }

    private fun utledTilbakeflytTilSteg(skalTilSteg: StegType?): BehandlingFlyt {
        if (skalTilSteg == null) {
            return BehandlingFlyt(emptyList())
        }

        val returflyt = flyt.slice(flyt.indexOfFirst { it.steg.type() == skalTilSteg }..flyt.indexOf(this.aktivtSteg))

        if (returflyt.size <= 1) {
            return BehandlingFlyt(emptyList())
        }

        return BehandlingFlyt(
            flyt = returflyt.reversed(),
            vurderingsbehov = emptyMap(),
            parent = this
        )
    }

    internal fun erTom(): Boolean {
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

    fun skalOppdatereFaktagrunnlag(): Boolean {
        return requireNotNull(aktivtSteg).oppdaterFaktagrunnlag
    }

    /**
     * Lager en kopi av flyten uten vurderingsbehov knyttet til steg.
     */
    fun utenVurderingsbehov(): BehandlingFlyt {
        return BehandlingFlyt(flyt = flyt)
    }

    fun vurderingsbehovRelevantForSteg(stegType: StegType): Set<Vurderingsbehov> {
        return if (steg(stegType).oppdaterFaktagrunnlag) {
            vurderingsbehov.filter { entry -> entry.value.contains(stegType) }.keys
        } else {
            Vurderingsbehov.entries.toSet()
        }
    }

    fun skalOppdatereFaktagrunnlagForSteg(nåværendeSteg: StegType): Boolean {
        return steg(nåværendeSteg).oppdaterFaktagrunnlag
    }

    override fun toString(): String {
        return "BehandlingFlyt(aktivtSteg=$aktivtSteg, flyt=$flyt, vurderingsbehov=$vurderingsbehov, parent=$parent)"
    }

    fun alleInformasjonskravForÅpneSteg(): List<Informasjonskravkonstruktør> {
        return flyt.flatMap {
            if (it.steg.type().status.erÅpen())
                it.kravliste
            else
                emptyList()
        }
    }
}


class BehandlingFlytBuilder {
    private val flyt: MutableList<BehandlingFlyt.Behandlingsflytsteg> = mutableListOf()
    private val endringTilSteg: MutableMap<Vurderingsbehov, MutableList<StegType>> = mutableMapOf()
    private var oppdaterFaktagrunnlag = true
    private var buildt = false

    fun medSteg(
        steg: FlytSteg,
        vurderingsbehovRelevanteForSteg: List<Vurderingsbehov> = Vurderingsbehov.alle(),
        informasjonskrav: List<Informasjonskravkonstruktør> = emptyList()
    ): BehandlingFlytBuilder {
        if (buildt) {
            throw IllegalStateException("[Utviklerfeil] Builder er allerede bygget")
        }
        if (StegType.UDEFINERT == steg.type()) {
            throw IllegalStateException("[Utviklerfeil] StegType UDEFINERT er ugyldig å legge til i flyten")
        }
        this.flyt.add(BehandlingFlyt.Behandlingsflytsteg(steg, informasjonskrav, oppdaterFaktagrunnlag))
        vurderingsbehovRelevanteForSteg.forEach { endring ->
            val stegene = this.endringTilSteg[endring] ?: mutableListOf()
            stegene.add(steg.type())
            this.endringTilSteg[endring] = stegene
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
            flyt = Collections.unmodifiableList(flyt),
            vurderingsbehov = Collections.unmodifiableMap(endringTilSteg),
        )
    }
}
