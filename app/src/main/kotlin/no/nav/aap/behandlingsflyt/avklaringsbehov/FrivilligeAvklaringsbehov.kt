package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.verdityper.flyt.StegType
import java.time.LocalDateTime

class FrivilligeAvklaringsbehov(
    private val avklaringsbehovene: Avklaringsbehovene,
    private val flyt: BehandlingFlyt,
    private val aktivtSteg: StegType
) : AvklaringsbehoveneDecorator by avklaringsbehovene {

    override fun alle(): List<Avklaringsbehov> {
        val eksisterendeBehov = avklaringsbehovene.alle()
        val list = flyt.frivilligeAvklaringsbehovRelevantForFlyten(aktivtSteg)
            .filter { definisjon -> eksisterendeBehov.none { behov -> behov.definisjon == definisjon } }
            .map { definisjon ->
                Avklaringsbehov(
                    id = Long.MAX_VALUE,
                    definisjon = definisjon,
                    historikk = mutableListOf(
                        Endring(
                            status = Status.OPPRETTET,
                            tidsstempel = LocalDateTime.now(),
                            begrunnelse = "",
                            endretAv = "system"
                        )
                    ),
                    funnetISteg = definisjon.løsesISteg,
                    kreverToTrinn = null
                )
            }.toMutableList()
        list.addAll(eksisterendeBehov)

        return list.toList()
    }

    fun harVærtSendtTilbakeFraBeslutterTidligere(): Boolean {
        return avklaringsbehovene.harVærtSendtTilbakeFraBeslutterTidligere()
    }
}