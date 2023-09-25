package no.nav.aap.domene.behandling.avklaringsbehov

import no.nav.aap.domene.behandling.StegTilstand
import no.nav.aap.flyt.BehandlingFlyt
import no.nav.aap.flyt.StegType

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

    fun alle(): List<Avklaringsbehov> = avklaringsbehovene.toList()
    fun åpne(): List<Avklaringsbehov> = avklaringsbehovene.filter { it.erÅpent() }.toList()

    fun skalHoppesTilbake(
        behandlingFlyt: BehandlingFlyt,
        aktivtSteg: StegTilstand,
        definisjoner: List<Definisjon>
    ): Boolean {
        val relevanteBehov = avklaringsbehovene.filter { it.definisjon in definisjoner }

        return relevanteBehov.any { behov ->
            behandlingFlyt.erStegFør(
                stegA = behov.løsesISteg(),
                stegB = aktivtSteg.tilstand.steg()
            )
        }
    }
}
