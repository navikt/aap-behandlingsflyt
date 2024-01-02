package no.nav.aap.behandlingsflyt.avklaringsbehov

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import java.time.Period
import java.util.*
import java.util.stream.Collectors

const val MANUELT_SATT_PÅ_VENT_KODE = "9001"
const val AVKLAR_STUDENT_KODE = "5001"
const val AVKLAR_YRKESSKADE_KODE = "5002"
const val AVKLAR_SYKDOM_KODE = "5003"
const val FASTSETT_ARBEIDSEVNE_KODE = "5004"
const val FRITAK_MELDEPLIKT_KODE = "5005"
const val AVKLAR_BISTANDSBEHOV_KODE = "5006"
const val VURDER_SYKEPENGEERSTATNING_KODE = "5007"
const val FORESLÅ_VEDTAK_KODE = "5098"
const val FATTE_VEDTAK_KODE = "5099"

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class Definisjon(
    @JsonProperty("kode") val kode: String,
    private val type: BehovType = BehovType.MANUELT_PÅKREVD,
    @JsonIgnore private val defaultFrist: Period = Period.ZERO,
    @JsonProperty("løsesISteg") val løsesISteg: StegType = StegType.UDEFINERT,
    val kreverToTrinn: Boolean = false
) {
    MANUELT_SATT_PÅ_VENT(
        kode = MANUELT_SATT_PÅ_VENT_KODE,
        type = BehovType.AUTOMATISK,
        defaultFrist = Period.ofWeeks(3),
    ),
    AVKLAR_STUDENT(
        kode = AVKLAR_STUDENT_KODE,
        løsesISteg = StegType.AVKLAR_STUDENT,
    ),
    AVKLAR_YRKESSKADE(
        kode = AVKLAR_YRKESSKADE_KODE,
        løsesISteg = StegType.AVKLAR_YRKESSKADE,
        kreverToTrinn = true
    ),
    AVKLAR_SYKDOM(
        kode = AVKLAR_SYKDOM_KODE,
        løsesISteg = StegType.AVKLAR_SYKDOM,
        kreverToTrinn = true
    ),
    FASTSETT_ARBEIDSEVNE(
        kode = FASTSETT_ARBEIDSEVNE_KODE,
        type = BehovType.MANUELT_FRIVILLIG,
        løsesISteg = StegType.FASTSETT_ARBEIDSEVNE,
        kreverToTrinn = true
    ),
    FRITAK_MELDEPLIKT(
        kode = FRITAK_MELDEPLIKT_KODE,
        type = BehovType.MANUELT_FRIVILLIG,
        løsesISteg = StegType.FRITAK_MELDEPLIKT,
        kreverToTrinn = true
    ),
    AVKLAR_BISTANDSBEHOV(
        kode = AVKLAR_BISTANDSBEHOV_KODE,
        løsesISteg = StegType.VURDER_BISTANDSBEHOV,
        kreverToTrinn = true
    ),
    AVKLAR_SYKEPENGEERSTATNING(
        kode = VURDER_SYKEPENGEERSTATNING_KODE,
        løsesISteg = StegType.VURDER_SYKEPENGEERSTATNING,
        kreverToTrinn = true
    ),
    FORESLÅ_VEDTAK(
        kode = FORESLÅ_VEDTAK_KODE,
        løsesISteg = StegType.FORESLÅ_VEDTAK,
    ),
    FATTE_VEDTAK(
        kode = FATTE_VEDTAK_KODE,
        løsesISteg = StegType.FATTE_VEDTAK,
    );

    companion object {
        fun forKode(definisjon: String): Definisjon {
            return entries.single { it.kode == definisjon }
        }

        init {
            val unikeKoder =
                Arrays.stream(entries.toTypedArray())
                    .map { it.kode }
                    .collect(Collectors.toSet())

            if (unikeKoder.size != entries.size) {
                throw IllegalStateException("Gjenbrukt koder for Avklaringsbehov")
            }

            for (value in entries) {
                value.type.valideringsFunksjon(value)
            }
        }
    }

    enum class BehovType(val valideringsFunksjon: Definisjon.() -> Unit) {
        MANUELT_PÅKREVD(Definisjon::validerManuelt),
        MANUELT_FRIVILLIG(Definisjon::validerManuelt),
        AUTOMATISK(Definisjon::validerAutomatisk)
    }

    fun skalLøsesISteg(steg: StegType, funnetISteg: StegType): Boolean {
        if (løsesISteg == StegType.UDEFINERT) {
            return steg == funnetISteg
        }
        return løsesISteg == steg
    }

    private fun validerManuelt() {
        if (this.løsesISteg.tekniskSteg) {
            throw IllegalArgumentException(
                "Avklaringsbehov må være knyttet til et funksjonelt steg"
            )
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

    override fun toString(): String {
        return "$name(kode='$kode')"
    }

    fun erFrivillig(): Boolean {
        return type == BehovType.MANUELT_FRIVILLIG
    }
}
