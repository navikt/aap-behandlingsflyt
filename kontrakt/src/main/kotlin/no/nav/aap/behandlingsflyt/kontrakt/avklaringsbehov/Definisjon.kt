package no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import java.time.LocalDate
import java.time.Period
import java.util.*
import java.util.stream.Collectors

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class Definisjon(
    @JsonProperty("kode") val kode: AvklaringsbehovKode,
    val type: BehovType,
    @JsonIgnore private val defaultFrist: Period = Period.ZERO,
    @JsonProperty("løsesISteg") val løsesISteg: StegType = StegType.UDEFINERT,
    val kreverToTrinn: Boolean = false,
    val kvalitetssikres: Boolean = false
) {
    MANUELT_SATT_PÅ_VENT(
        kode = AvklaringsbehovKode.`9001`,
        type = BehovType.VENTEPUNKT,
        defaultFrist = Period.ofWeeks(3),
    ),
    BESTILL_BREV(
        kode = AvklaringsbehovKode.`9002`,
        løsesISteg = StegType.BREV,
        type = BehovType.VENTEPUNKT,
        defaultFrist = Period.ofDays(1),
    ),
    SKRIV_BREV(
        kode = AvklaringsbehovKode.`5050`,
        løsesISteg = StegType.BREV,
        type = BehovType.MANUELT_PÅKREVD,
    ),
    AVKLAR_STUDENT(
        kode = AvklaringsbehovKode.`5001`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.AVKLAR_STUDENT,
        kreverToTrinn = true
    ),
    AVKLAR_SYKDOM(
        kode = AvklaringsbehovKode.`5003`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.AVKLAR_SYKDOM,
        kreverToTrinn = true,
        kvalitetssikres = true
    ),
    KVALITETSSIKRING(
        kode = AvklaringsbehovKode.`5097`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.KVALITETSSIKRING
    ),
    FASTSETT_ARBEIDSEVNE(
        kode = AvklaringsbehovKode.`5004`,
        type = BehovType.MANUELT_FRIVILLIG,
        løsesISteg = StegType.FASTSETT_ARBEIDSEVNE,
        kreverToTrinn = true,
        kvalitetssikres = true
    ),
    FASTSETT_BEREGNINGSTIDSPUNKT(
        kode = AvklaringsbehovKode.`5008`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.FASTSETT_BEREGNINGSTIDSPUNKT,
        kreverToTrinn = true
    ),
    FRITAK_MELDEPLIKT(
        kode = AvklaringsbehovKode.`5005`,
        type = BehovType.MANUELT_FRIVILLIG,
        løsesISteg = StegType.FRITAK_MELDEPLIKT,
        kreverToTrinn = true,
        kvalitetssikres = true
    ),
    AVKLAR_BISTANDSBEHOV(
        kode = AvklaringsbehovKode.`5006`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.VURDER_BISTANDSBEHOV,
        kreverToTrinn = true,
        kvalitetssikres = true
    ),
    AVKLAR_SYKEPENGEERSTATNING(
        kode = AvklaringsbehovKode.`5007`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.VURDER_SYKEPENGEERSTATNING,
        kreverToTrinn = true
    ),
    AVKLAR_BARNETILLEGG(
        kode = AvklaringsbehovKode.`5009`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.BARNETILLEGG,
        kreverToTrinn = true
    ),
    AVKLAR_SONINGSFORRHOLD(
        kode = AvklaringsbehovKode.`5010`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.DU_ER_ET_ANNET_STED,
        kreverToTrinn = true
    ),
    AVKLAR_HELSEINSTITUSJON(
        kode = AvklaringsbehovKode.`5011`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.DU_ER_ET_ANNET_STED,
        kreverToTrinn = true
    ),
    AVKLAR_SAMORDNING_GRADERING(
        kode = AvklaringsbehovKode.`5012`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.SAMORDNING_GRADERING
    ),
    FORESLÅ_VEDTAK(
        kode = AvklaringsbehovKode.`5098`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.FORESLÅ_VEDTAK,
    ),
    FATTE_VEDTAK(
        kode = AvklaringsbehovKode.`5099`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.FATTE_VEDTAK,
    );

    companion object {
        fun forKode(definisjon: String): Definisjon {
            return entries.single { it.kode == AvklaringsbehovKode.valueOf(definisjon) }
        }

        fun forKode(definisjon: AvklaringsbehovKode): Definisjon {
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

    fun erVentebehov(): Boolean {
        return type == BehovType.VENTEPUNKT
    }

    fun utledFrist(frist: LocalDate?): LocalDate {
        if (!erVentebehov()) {
            throw IllegalStateException("Forsøker utlede frist for et behov som ikke er ventepunkt")
        }
        if (frist != null) {
            return frist
        }
        return LocalDate.now().plus(defaultFrist)
    }
}
