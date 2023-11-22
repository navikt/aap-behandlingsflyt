package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class Avklaringsbehovene(avklaringsbehovene: List<Avklaringsbehov> = mutableListOf()) {

    private val avklaringsbehovene: MutableList<Avklaringsbehov>

    init {
        this.avklaringsbehovene = avklaringsbehovene.toMutableList()
    }

    fun leggTil(funnetAvklaringsbehov: List<Definisjon>, steg: StegType) {
        funnetAvklaringsbehov.stream()
            .map { definisjon ->
                Avklaringsbehov(
                    id = Long.MAX_VALUE,
                    definisjon = definisjon,
                    funnetISteg = steg,
                    kreverToTrinn = null
                )
            }
            .forEach { this.leggTil(it) }
    }

    fun leggTil(avklaringsbehov: Avklaringsbehov) {
        val relevantBehov = avklaringsbehovene.firstOrNull { it.definisjon == avklaringsbehov.definisjon }

        if (relevantBehov != null) {
            relevantBehov.reåpne()
        } else {
            avklaringsbehovene.add(avklaringsbehov)
        }
    }

    fun løsAvklaringsbehov(definisjon: Definisjon, begrunnelse: String, endretAv: String) {
        avklaringsbehovene.single { it.definisjon == definisjon }.løs(begrunnelse, endretAv = endretAv)
    }

    fun løsAvklaringsbehov(definisjon: Definisjon, begrunnelse: String, endretAv: String, kreverToTrinn: Boolean) {
        avklaringsbehovene.single { it.definisjon == definisjon }
            .løs(begrunnelse = begrunnelse, endretAv = endretAv, kreverToTrinn = kreverToTrinn)
    }

    fun alle(): List<Avklaringsbehov> {
        return avklaringsbehovene.toList()
    }

    fun åpne(): List<Avklaringsbehov> {
        return avklaringsbehovene.filter { it.erÅpent() }.toList()
    }

    fun tilbakeførtFraBeslutter(): List<Avklaringsbehov> {
        return avklaringsbehovene.filter { it.status() == Status.SENDT_TILBAKE_FRA_BESLUTTER }.toList()
    }

    fun hentBehovForDefinisjon(definisjon: Definisjon): Avklaringsbehov? {
        return avklaringsbehovene.filter { it.definisjon == definisjon }.singleOrNull()
    }

    fun hentBehovForDefinisjon(definisjoner: List<Definisjon>): List<Avklaringsbehov> {
        return avklaringsbehovene.filter { it.definisjon in definisjoner }.toList()
    }

    fun vurderTotrinn(definisjon: Definisjon, godkjent: Boolean, begrunnelse: String) {
        avklaringsbehovene.single { it.definisjon == definisjon }.vurderTotrinn(begrunnelse, godkjent)
    }

    fun avbryt(definisjon: Definisjon) {
        avklaringsbehovene.single { it.definisjon == definisjon }.avbryt()
    }

    fun harHattAvklaringsbehov(): Boolean {
        return avklaringsbehovene.any { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
    }

    fun harHattAvklaringsbehovSomHarKrevdToTrinn() =
        avklaringsbehovene
            .filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
            .any { avklaringsbehov -> avklaringsbehov.erTotrinn() && !avklaringsbehov.erTotrinnsVurdert() }

    fun harIkkeForeslåttVedtak(): Boolean {
        return avklaringsbehovene
            .filter { avklaringsbehov -> avklaringsbehov.erForeslåttVedtak() }
            .none { it.status() == Status.AVSLUTTET }
    }

    fun skalTilbakeføresEtterTotrinnsVurdering(): Boolean {
        return tilbakeførtFraBeslutter().isNotEmpty()
    }
}