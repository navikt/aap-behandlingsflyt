package no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import java.time.LocalDate
import java.time.Period
import java.util.*
import java.util.stream.Collectors

const val MANUELT_SATT_PÅ_VENT_KODE = "9001"
const val AVVENTER_BREV_BESTILLING_KODE = "9002"
const val AVKLAR_STUDENT_KODE = "5001"
const val AVKLAR_SYKDOM_KODE = "5003"
const val FASTSETT_ARBEIDSEVNE_KODE = "5004"
const val FRITAK_MELDEPLIKT_KODE = "5005"
const val AVKLAR_BISTANDSBEHOV_KODE = "5006"
const val VURDER_SYKEPENGEERSTATNING_KODE = "5007"
const val FASTSETT_BEREGNINGSTIDSPUNKT_KODE = "5008"
const val AVKLAR_BARNETILLEGG_KODE = "5009"
const val AVKLAR_SONINGSFORRHOLD_KODE = "5010"
const val AVKLAR_HELSEINSTITUSJON_KODE = "5011"
const val AVKLAR_SAMORDNING_GRADERING_KODE = "5012"
const val KVALITETSSIKRING_KODE = "5097"
const val FORESLÅ_VEDTAK_KODE = "5098"
const val FATTE_VEDTAK_KODE = "5099"

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class Definisjon(
    @JsonProperty("kode") val kode: String,
    val type: BehovType,
    @JsonIgnore private val defaultFrist: Period = Period.ZERO,
    @JsonProperty("løsesISteg") val løsesISteg: StegType = StegType.UDEFINERT,
    val kreverToTrinn: Boolean = false,
    val kvalitetssikres: Boolean = false
) {
    MANUELT_SATT_PÅ_VENT(
        kode = MANUELT_SATT_PÅ_VENT_KODE,
        type = BehovType.VENTEPUNKT,
        defaultFrist = Period.ofWeeks(3),
    ),
    AVVENTER_BREV_BESTILLING(
        kode = AVVENTER_BREV_BESTILLING_KODE,
        løsesISteg = StegType.BREV,
        type = BehovType.VENTEPUNKT,
        defaultFrist = Period.ofWeeks(3),
    ),
    AVKLAR_STUDENT(
        kode = AVKLAR_STUDENT_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.AVKLAR_STUDENT,
        kreverToTrinn = true
    ),
    AVKLAR_SYKDOM(
        kode = AVKLAR_SYKDOM_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.AVKLAR_SYKDOM,
        kreverToTrinn = true,
        kvalitetssikres = true
    ),
    KVALITETSSIKRING(
        kode = KVALITETSSIKRING_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.KVALITETSSIKRING
    ),
    FASTSETT_ARBEIDSEVNE(
        kode = FASTSETT_ARBEIDSEVNE_KODE,
        type = BehovType.MANUELT_FRIVILLIG,
        løsesISteg = StegType.FASTSETT_ARBEIDSEVNE,
        kreverToTrinn = true,
        kvalitetssikres = true
    ),
    FASTSETT_BEREGNINGSTIDSPUNKT(
        kode = FASTSETT_BEREGNINGSTIDSPUNKT_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.FASTSETT_BEREGNINGSTIDSPUNKT,
        kreverToTrinn = true
    ),
    FRITAK_MELDEPLIKT(
        kode = FRITAK_MELDEPLIKT_KODE,
        type = BehovType.MANUELT_FRIVILLIG,
        løsesISteg = StegType.FRITAK_MELDEPLIKT,
        kreverToTrinn = true,
        kvalitetssikres = true
    ),
    AVKLAR_BISTANDSBEHOV(
        kode = AVKLAR_BISTANDSBEHOV_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.VURDER_BISTANDSBEHOV,
        kreverToTrinn = true,
        kvalitetssikres = true
    ),
    AVKLAR_SYKEPENGEERSTATNING(
        kode = VURDER_SYKEPENGEERSTATNING_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.VURDER_SYKEPENGEERSTATNING,
        kreverToTrinn = true
    ),
    AVKLAR_BARNETILLEGG(
        kode = AVKLAR_BARNETILLEGG_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.BARNETILLEGG,
        kreverToTrinn = true
    ),
    AVKLAR_SONINGSFORRHOLD(
        kode = AVKLAR_SONINGSFORRHOLD_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.DU_ER_ET_ANNET_STED,
        kreverToTrinn = true
    ),
    AVKLAR_HELSEINSTITUSJON(
        kode = AVKLAR_HELSEINSTITUSJON_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.DU_ER_ET_ANNET_STED,
        kreverToTrinn = true
    ),
    AVKLAR_SAMORDNING_GRADERING(
        kode = AVKLAR_SAMORDNING_GRADERING_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.SAMORDNING_GRADERING
    ),
    FORESLÅ_VEDTAK(
        kode = FORESLÅ_VEDTAK_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.FORESLÅ_VEDTAK,
    ),
    FATTE_VEDTAK(
        kode = FATTE_VEDTAK_KODE,
        type = BehovType.MANUELT_PÅKREVD,
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
        VENTEPUNKT(Definisjon::validerVentepunkt)
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

    private fun validerVentepunkt() {
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

    fun erVentepunkt(): Boolean {
        return type == BehovType.VENTEPUNKT
    }

    fun utledFrist(frist: LocalDate?): LocalDate {
        if (!erVentepunkt()) {
            throw IllegalStateException("Forsøker utlede frist for et behov som ikke er ventepunkt")
        }
        if (frist != null) {
            return frist
        }
        return LocalDate.now().plus(defaultFrist)
    }
}
