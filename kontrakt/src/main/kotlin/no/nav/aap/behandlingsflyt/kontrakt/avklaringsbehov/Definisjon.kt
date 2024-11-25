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
public enum class Definisjon(
    @JsonProperty("kode") public val kode: AvklaringsbehovKode,
    public val type: BehovType,
    @JsonIgnore private val defaultFrist: Period = Period.ZERO,
    @JsonProperty("løsesISteg") public val løsesISteg: StegType = StegType.UDEFINERT,
    public val kreverToTrinn: Boolean = false,
    public val kvalitetssikres: Boolean = false
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
    BESTILL_LEGEERKLÆRING(
        kode = AvklaringsbehovKode.`9003`,
        løsesISteg = StegType.AVKLAR_SYKDOM,
        type = BehovType.VENTEPUNKT,
        defaultFrist = Period.ofWeeks(4)
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
    FASTSETT_YRKESSKADEINNTEKT(
        kode = AvklaringsbehovKode.`5014`,
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
    AVKLAR_YRKESSKADE(
        kode = AvklaringsbehovKode.`5013`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.VURDER_YRKESSKADE,
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

    public companion object {
        public fun forKode(definisjon: String): Definisjon {
            return entries.single { it.kode == AvklaringsbehovKode.valueOf(definisjon) }
        }

        public fun forKode(definisjon: AvklaringsbehovKode): Definisjon {
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

    public enum class BehovType(public val valideringsFunksjon: Definisjon.() -> Unit) {
        /**
         * Manuelt påkrevd er at systemet vil trigge behovet og det er eneste måten å få det på.
         */
        MANUELT_PÅKREVD(Definisjon::validerManuelt),

        /**
         * Frivillig er at saksbehandler og system kan trigge behovet
         */
        MANUELT_FRIVILLIG(Definisjon::validerManuelt),

        /**
         * Ventebehov kan opprettes av saksbehandler og system, det er et behov som venter på tid og/eller en hendelse
         * (f.eks et dokument)
         */
        VENTEPUNKT(Definisjon::validerVentepunkt)
    }

    public fun skalLøsesISteg(steg: StegType, funnetISteg: StegType): Boolean {
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

    public fun erFrivillig(): Boolean {
        return type == BehovType.MANUELT_FRIVILLIG
    }

    public fun erVentebehov(): Boolean {
        return type == BehovType.VENTEPUNKT
    }

    public fun utledFrist(frist: LocalDate?): LocalDate {
        if (!erVentebehov()) {
            throw IllegalStateException("Forsøker utlede frist for et behov som ikke er ventepunkt")
        }
        if (frist != null) {
            return frist
        }
        return LocalDate.now().plus(defaultFrist)
    }
}
