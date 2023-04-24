package no.nav.aap.domene.behandling.avklaringsbehov

import no.nav.aap.flyt.StegType
import java.time.Period
import java.util.Arrays
import java.util.stream.Collectors
import kotlin.reflect.KFunction1

enum class Definisjon(private val kode: String,
                      private val type: BehovType = BehovType.MANUELT,
                      private val defaultFrist: Period = Period.ZERO,
                      private val løsesISteg: StegType = StegType.UDEFINERT,
                      private val vurderingspunkt: Vurderingspunkt,
                      private val rekjørSteg: Boolean = false) {

    MANUELT_SATT_PÅ_VENT(
        kode = "9001",
        type = BehovType.AUTOMATISK,
        defaultFrist = Period.ofWeeks(3),
        vurderingspunkt = Vurderingspunkt.UT,
        rekjørSteg = true
    ),
    AVKLAR_YRKESSKADE(
        kode = "5001",
        løsesISteg = StegType.AVKLAR_YRKESSKADE,
        vurderingspunkt = Vurderingspunkt.INN
    );

    companion object {
        init {
            val unikeKoder = Arrays.stream(values())
                .map { it.kode }
                .collect(Collectors.toSet())

            if (unikeKoder.size != values().size) {
                throw IllegalStateException("Gjenbrukt koder for Avklaringsbehov")
            }

            for (value in values()) {
                value.type.valideringsFunksjon.invoke(value)
            }
        }
    }

    enum class BehovType(val valideringsFunksjon: KFunction1<Definisjon, Unit>) {
        MANUELT(Definisjon::validerManuelt),
        AUTOMATISK(Definisjon::validerAutomatisk);
    }

    fun løsesISteg(steg: StegType): Boolean {
        return løsesISteg == steg
    }

    private fun validerManuelt() {
        if (this.løsesISteg.tekniskSteg) {
            throw IllegalArgumentException("Avklaringsbehov må være knyttet til et funksjonelt steg")
        }
    }

    private fun validerAutomatisk() {
        if (this == MANUELT_SATT_PÅ_VENT) {
            if (this.løsesISteg != StegType.UDEFINERT) {
                throw IllegalArgumentException("Manueltsatt på vent er lagt til feil steg")
            }
        }
        if (this != MANUELT_SATT_PÅ_VENT) {
            if (this.løsesISteg == StegType.UDEFINERT) {
                throw IllegalArgumentException("Ventepunkt er lagt til feil steg")
            }
            if (defaultFrist == Period.ZERO) {
                throw IllegalArgumentException("Vent trenger å sette en default frist")
            }
        }
    }
}
