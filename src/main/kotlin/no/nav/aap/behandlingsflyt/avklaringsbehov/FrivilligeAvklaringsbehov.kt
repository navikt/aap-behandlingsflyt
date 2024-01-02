package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import java.time.LocalDateTime

class FrivilligeAvklaringsbehov(
    private val avklaringsbehovene: AvklaringsbehoveneDecorator,
    private val flyt: BehandlingFlyt
) : AvklaringsbehoveneDecorator by avklaringsbehovene {

    override fun alle(): List<Avklaringsbehov> {
        val eksisterendeBehov = avklaringsbehovene.alle()
        val list = flyt.frivilligeAvklaringsbehovRelevantForFlyten()
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
}