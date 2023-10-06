package no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.domene.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class Avklaringsbehovene {

    private val avklaringsbehovene: MutableList<Avklaringsbehov> = mutableListOf()

    fun leggTil(funnetAvklaringsbehov: List<Definisjon>, steg: StegType) {
        funnetAvklaringsbehov.stream()
            .map { definisjon ->
                Avklaringsbehov(
                    definisjon,
                    funnetISteg = steg
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

    fun alle(): List<Avklaringsbehov> {
        return avklaringsbehovene.toList()
    }

    fun åpne(): List<Avklaringsbehov> {
        return avklaringsbehovene.filter { it.erÅpent() }.toList()
    }

    fun tilbakeførtFraBeslutter(): List<Avklaringsbehov> {
        return avklaringsbehovene.filter { it.status() == Status.SENDT_TILBAKE_FRA_BESLUTTER }.toList()
    }

    fun hentBehovForLøsninger(definisjoner: List<Definisjon>): List<Avklaringsbehov> {
        return avklaringsbehovene.filter {it.definisjon in definisjoner }.toList()
    }

    fun vurderTotrinn(definisjon: Definisjon, godkjent: Boolean, begrunnelse: String) {
        avklaringsbehovene.single { it.definisjon == definisjon }.vurderTotrinn(begrunnelse, godkjent)
    }
}
