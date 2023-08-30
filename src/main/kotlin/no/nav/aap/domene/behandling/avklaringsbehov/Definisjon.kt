package no.nav.aap.domene.behandling.avklaringsbehov

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.aap.flyt.StegStatus
import no.nav.aap.flyt.StegType
import java.time.Period
import java.util.*
import java.util.stream.Collectors
import kotlin.reflect.KFunction1

const val MANUELT_SATT_PÅ_VENT_KODE = "9001"
const val AVKLAR_YRKESSKADE_KODE = "5001"
const val FORESLÅ_VEDTAK_KODE = "5098"
const val FATTE_VEDTAK_KODE = "5099"

enum class Definisjon(@JsonValue val kode: String,
                      private val type: BehovType = BehovType.MANUELT,
                      private val defaultFrist: Period = Period.ZERO,
                      val løsesISteg: StegType = StegType.UDEFINERT,
                      val vurderingspunkt: Vurderingspunkt,
                      val rekjørSteg: Boolean = false,
                      val kreverToTrinn: Boolean = false) {

    MANUELT_SATT_PÅ_VENT(
        kode = MANUELT_SATT_PÅ_VENT_KODE,
        type = BehovType.AUTOMATISK,
        defaultFrist = Period.ofWeeks(3),
        vurderingspunkt = Vurderingspunkt.UT,
        rekjørSteg = true
    ),
    AVKLAR_YRKESSKADE(
        kode = AVKLAR_YRKESSKADE_KODE,
        løsesISteg = StegType.AVKLAR_YRKESSKADE,
        vurderingspunkt = Vurderingspunkt.UT,
        kreverToTrinn = true
    ),
    FORESLÅ_VEDTAK(
        kode = FORESLÅ_VEDTAK_KODE,
        løsesISteg = StegType.FORESLÅ_VEDTAK,
        vurderingspunkt = Vurderingspunkt.UT
    ),
    FATTE_VEDTAK(
        kode = FATTE_VEDTAK_KODE,
        løsesISteg = StegType.FATTE_VEDTAK,
        vurderingspunkt = Vurderingspunkt.UT
    );

    companion object {
        init {
            val unikeKoder = Arrays.stream(entries.toTypedArray())
                .map { it.kode }
                .collect(Collectors.toSet())

            if (unikeKoder.size != entries.size) {
                throw IllegalStateException("Gjenbrukt koder for Avklaringsbehov")
            }

            for (value in entries) {
                value.type.valideringsFunksjon.invoke(value)
            }
        }
    }

    enum class BehovType(val valideringsFunksjon: KFunction1<Definisjon, Unit>) {
        MANUELT(Definisjon::validerManuelt),
        AUTOMATISK(Definisjon::validerAutomatisk);
    }

    fun skalLøsesISteg(steg: StegType): Boolean {
        return løsesISteg == steg
    }

    fun påStegStatus(status: StegStatus): Boolean {
        return vurderingspunkt.stegStatus == status
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
