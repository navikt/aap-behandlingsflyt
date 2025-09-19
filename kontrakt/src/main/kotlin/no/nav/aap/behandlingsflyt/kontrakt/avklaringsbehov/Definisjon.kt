package no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.tilgang.Rolle
import java.time.LocalDate
import java.time.Period
import java.util.*
import java.util.stream.Collectors

/**
 * Brukes for å definere et [no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov].
 *
 * @param løsesAv Hva slags roller som har lov til å løse dette avklaringsbehovet. Dette blir verifisert i tilgangsmodulen.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum class Definisjon(
    @param:JsonProperty("kode") public val kode: AvklaringsbehovKode,
    public val type: BehovType,
    @JsonIgnore private val defaultFrist: Period = Period.ZERO,
    @param:JsonProperty("løsesISteg") public val løsesISteg: StegType = StegType.UDEFINERT,
    public val kreverToTrinn: Boolean = false,
    public val kvalitetssikres: Boolean = false,
    public val løsesAv: List<Rolle>
) {
    MANUELT_SATT_PÅ_VENT(
        kode = AvklaringsbehovKode.`9001`,
        type = BehovType.VENTEPUNKT,
        defaultFrist = Period.ofWeeks(3),
        løsesAv = listOf(
            Rolle.SAKSBEHANDLER_OPPFOLGING,
            Rolle.SAKSBEHANDLER_NASJONAL
        )
    ),

    @Deprecated("Ikke i bruk lenger")
    BESTILL_BREV(
        kode = AvklaringsbehovKode.`9002`,
        løsesISteg = StegType.UDEFINERT,
        type = BehovType.BREV_VENTEPUNKT,
        defaultFrist = Period.ofDays(1),
        løsesAv = emptyList()
    ),
    VURDER_TREKK_AV_SØKNAD(
        kode = AvklaringsbehovKode.`5028`,
        løsesISteg = StegType.SØKNAD,
        type = BehovType.MANUELT_PÅKREVD,
        løsesAv = listOf(
            Rolle.SAKSBEHANDLER_OPPFOLGING,
            Rolle.SAKSBEHANDLER_NASJONAL
        ),
    ),
    AVBRYT_REVURDERING(
        kode = AvklaringsbehovKode.`5033`,
        løsesISteg = StegType.AVBRYT_REVURDERING,
        type = BehovType.MANUELT_PÅKREVD,
        løsesAv = listOf(
            Rolle.SAKSBEHANDLER_OPPFOLGING,
            Rolle.SAKSBEHANDLER_NASJONAL
        ),
    ),
    BESTILL_LEGEERKLÆRING(
        kode = AvklaringsbehovKode.`9003`,
        løsesISteg = StegType.UDEFINERT,
        type = BehovType.VENTEPUNKT,
        defaultFrist = Period.ofWeeks(4),
        løsesAv = listOf(
            Rolle.SAKSBEHANDLER_OPPFOLGING,
            Rolle.SAKSBEHANDLER_NASJONAL
        )
    ),
    OPPRETT_HENDELSE_PÅ_SAK(
        kode = AvklaringsbehovKode.`9004`,
        type = BehovType.VENTEPUNKT,
        løsesISteg = StegType.UDEFINERT,
        løsesAv = listOf(
            Rolle.SAKSBEHANDLER_NASJONAL,
            Rolle.SAKSBEHANDLER_OPPFOLGING,
        )
    ),

    @Deprecated("Bruk egne definisjoner for forskjellige brev")
    SKRIV_BREV(
        kode = AvklaringsbehovKode.`5050`,
        løsesISteg = StegType.UDEFINERT,
        type = BehovType.BREV,
        løsesAv = listOf(
            Rolle.SAKSBEHANDLER_OPPFOLGING,
            Rolle.SAKSBEHANDLER_NASJONAL,
            Rolle.BESLUTTER
        )
    ),
    SKRIV_VEDTAKSBREV(
        kode = AvklaringsbehovKode.`5051`,
        løsesISteg = StegType.BREV,
        type = BehovType.MANUELT_PÅKREVD,
        løsesAv = listOf(
            Rolle.BESLUTTER
        )
    ),
    VURDER_RETTIGHETSPERIODE(
        kode = AvklaringsbehovKode.`5029`,
        løsesISteg = StegType.VURDER_RETTIGHETSPERIODE,
        type = BehovType.MANUELT_PÅKREVD,
        kreverToTrinn = true,
        løsesAv = listOf(
            Rolle.SAKSBEHANDLER_NASJONAL
        )
    ),
    AVKLAR_STUDENT(
        kode = AvklaringsbehovKode.`5001`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.AVKLAR_STUDENT,
        kreverToTrinn = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
    ),
    OVERSTYR_IKKE_OPPFYLT_MELDEPLIKT(
        kode = AvklaringsbehovKode.`5002`,
        type = BehovType.OVERSTYR,
        løsesISteg = StegType.IKKE_OPPFYLT_MELDEPLIKT,
        kreverToTrinn = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)
    ),
    AVKLAR_SYKDOM(
        kode = AvklaringsbehovKode.`5003`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.AVKLAR_SYKDOM,
        kreverToTrinn = true,
        kvalitetssikres = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)
    ),
    KVALITETSSIKRING(
        kode = AvklaringsbehovKode.`5097`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.KVALITETSSIKRING,
        løsesAv = listOf(Rolle.KVALITETSSIKRER)
    ),
    FASTSETT_ARBEIDSEVNE(
        kode = AvklaringsbehovKode.`5004`,
        type = BehovType.MANUELT_FRIVILLIG,
        løsesISteg = StegType.FASTSETT_ARBEIDSEVNE,
        kreverToTrinn = true,
        kvalitetssikres = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)
    ),
    FASTSETT_BEREGNINGSTIDSPUNKT(
        kode = AvklaringsbehovKode.`5008`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.FASTSETT_BEREGNINGSTIDSPUNKT,
        kreverToTrinn = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
    ),
    FASTSETT_YRKESSKADEINNTEKT(
        kode = AvklaringsbehovKode.`5014`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.FASTSETT_BEREGNINGSTIDSPUNKT,
        kreverToTrinn = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
    ),
    SKRIV_SYKDOMSVURDERING_BREV(
        kode = AvklaringsbehovKode.`5053`,
        løsesISteg = StegType.SYKDOMSVURDERING_BREV,
        kvalitetssikres = true,
        kreverToTrinn = false,
        type = BehovType.MANUELT_PÅKREVD,
        løsesAv = listOf(
            Rolle.SAKSBEHANDLER_OPPFOLGING
        )
    ),
    FASTSETT_MANUELL_INNTEKT(
        kode = AvklaringsbehovKode.`7001`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.MANGLENDE_LIGNING,
        kreverToTrinn = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
    ),
    FRITAK_MELDEPLIKT(
        kode = AvklaringsbehovKode.`5005`,
        type = BehovType.MANUELT_FRIVILLIG,
        løsesISteg = StegType.FRITAK_MELDEPLIKT,
        kreverToTrinn = true,
        kvalitetssikres = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)
    ),
    AVKLAR_BISTANDSBEHOV(
        kode = AvklaringsbehovKode.`5006`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.VURDER_BISTANDSBEHOV,
        kreverToTrinn = true,
        kvalitetssikres = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)
    ),
    AVKLAR_OVERGANG_ARBEID(
        kode = AvklaringsbehovKode.`5032`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.OVERGANG_ARBEID,
        kreverToTrinn = true,
        kvalitetssikres = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)
    ),
    AVKLAR_OVERGANG_UFORE(
        kode = AvklaringsbehovKode.`5031`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.OVERGANG_UFORE,
        kreverToTrinn = true,
        kvalitetssikres = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)
    ),
    AVKLAR_SYKEPENGEERSTATNING(
        kode = AvklaringsbehovKode.`5007`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.VURDER_SYKEPENGEERSTATNING,
        kreverToTrinn = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
    ),
    AVKLAR_YRKESSKADE(
        kode = AvklaringsbehovKode.`5013`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.VURDER_YRKESSKADE,
        kreverToTrinn = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
    ),
    AVKLAR_BARNETILLEGG(
        kode = AvklaringsbehovKode.`5009`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.BARNETILLEGG,
        kreverToTrinn = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
    ),
    AVKLAR_SONINGSFORRHOLD(
        kode = AvklaringsbehovKode.`5010`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.DU_ER_ET_ANNET_STED,
        kreverToTrinn = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
    ),
    AVKLAR_HELSEINSTITUSJON(
        kode = AvklaringsbehovKode.`5011`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.DU_ER_ET_ANNET_STED,
        kreverToTrinn = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
    ),
    AVKLAR_SAMORDNING_GRADERING(
        kode = AvklaringsbehovKode.`5012`,
        type = BehovType.MANUELT_FRIVILLIG,
        løsesISteg = StegType.SAMORDNING_GRADERING,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
        kreverToTrinn = true,
    ),
    AVKLAR_SAMORDNING_UFØRE(
        kode = AvklaringsbehovKode.`5024`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.SAMORDNING_UFØRE,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
        kreverToTrinn = true,
    ),
    @Deprecated("Ikke lenger i bruk, erstattet av oppfølgingsoppgave")
    SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT(
        kode = AvklaringsbehovKode.`5025`,
        type = BehovType.VENTEPUNKT,
        løsesISteg = StegType.SAMORDNING_GRADERING,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL, Rolle.SAKSBEHANDLER_OPPFOLGING),
        defaultFrist = Period.ofWeeks(4),
    ),
    SAMORDNING_ANDRE_STATLIGE_YTELSER(
        kode = AvklaringsbehovKode.`5027`,
        type = BehovType.MANUELT_FRIVILLIG,
        løsesISteg = StegType.SAMORDNING_ANDRE_STATLIGE_YTELSER,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
        kreverToTrinn = true
    ),
    SAMORDNING_ARBEIDSGIVER(
        kode = AvklaringsbehovKode.`5030`,
        type = BehovType.MANUELT_FRIVILLIG,
        løsesISteg = StegType.SAMORDNING_ARBEIDSGIVER,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
        kreverToTrinn = true
    ),

    @Deprecated("Ikke i bruk")
    FORHÅNDSVARSEL_AKTIVITETSPLIKT(
        kode = AvklaringsbehovKode.`5016`,
        type = BehovType.BREV_VENTEPUNKT,
        løsesISteg = StegType.EFFEKTUER_11_7,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING),
    ),
    VENTE_PÅ_FRIST_EFFEKTUER_11_7(
        kode = AvklaringsbehovKode.`5018`,
        type = BehovType.VENTEPUNKT,
        løsesISteg = StegType.EFFEKTUER_11_7,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING),
        defaultFrist = Period.ofWeeks(3),
    ),
    AVKLAR_LOVVALG_MEDLEMSKAP(
        kode = AvklaringsbehovKode.`5017`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.VURDER_LOVVALG,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
        kreverToTrinn = true,
    ),
    EFFEKTUER_11_7(
        kode = AvklaringsbehovKode.`5015`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.EFFEKTUER_11_7,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING),
        kreverToTrinn = true,
    ),

    /**
     * Brukes til å stoppe i underveis for saksbehandler oppfølging
     */
    FORESLÅ_UTTAK(
        kode = AvklaringsbehovKode.`5096`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.FASTSETT_UTTAK,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)
    ),
    FORESLÅ_VEDTAK(
        kode = AvklaringsbehovKode.`5098`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.FORESLÅ_VEDTAK,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
    ),
    FATTE_VEDTAK(
        kode = AvklaringsbehovKode.`5099`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.FATTE_VEDTAK,
        løsesAv = listOf(Rolle.BESLUTTER)
    ),
    VENTE_PÅ_UTENLANDSK_VIDEREFØRING_AVKLARING(
        kode = AvklaringsbehovKode.`5019`,
        type = BehovType.VENTEPUNKT,
        løsesISteg = StegType.VURDER_LOVVALG,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING),
        defaultFrist = Period.ofYears(5),
    ),
    AVKLAR_FORUTGÅENDE_MEDLEMSKAP(
        kode = AvklaringsbehovKode.`5020`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.VURDER_MEDLEMSKAP,
        kreverToTrinn = true,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
    ),
    MANUELL_OVERSTYRING_LOVVALG(
        kode = AvklaringsbehovKode.`5021`,
        type = BehovType.OVERSTYR,
        løsesISteg = StegType.VURDER_LOVVALG,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
        kreverToTrinn = true
    ),
    MANUELL_OVERSTYRING_MEDLEMSKAP(
        kode = AvklaringsbehovKode.`5022`,
        type = BehovType.OVERSTYR,
        løsesISteg = StegType.VURDER_MEDLEMSKAP,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
        kreverToTrinn = true
    ),
    VENTE_PÅ_KLAGE_IMPLEMENTASJON(
        kode = AvklaringsbehovKode.`5023`,
        type = BehovType.VENTEPUNKT,
        løsesISteg = StegType.START_BEHANDLING,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
        defaultFrist = Period.ofYears(5),
    ),
    REFUSJON_KRAV(
        kode = AvklaringsbehovKode.`5026`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.REFUSJON_KRAV,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING),
    ),
    FASTSETT_PÅKLAGET_BEHANDLING(
        kode = AvklaringsbehovKode.`5999`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.PÅKLAGET_BEHANDLING,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
    ),
    SAMORDNING_REFUSJONS_KRAV(
        kode = AvklaringsbehovKode.`5056`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.SAMORDNING_TJENESTEPENSJON_REFUSJONSKRAV,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
        kreverToTrinn = true
    ),
    VURDER_FORMKRAV(
        kode = AvklaringsbehovKode.`6000`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.FORMKRAV,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
        kreverToTrinn = true
    ),
    FASTSETT_BEHANDLENDE_ENHET(
        kode = AvklaringsbehovKode.`6001`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.BEHANDLENDE_ENHET,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
    ),
    VURDER_KLAGE_KONTOR(
        kode = AvklaringsbehovKode.`6002`,
        kvalitetssikres = true,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.KLAGEBEHANDLING_KONTOR,
        løsesAv = listOf(
            Rolle.SAKSBEHANDLER_OPPFOLGING
        ),
        kreverToTrinn = true

    ),
    VURDER_KLAGE_NAY(
        kode = AvklaringsbehovKode.`6003`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.KLAGEBEHANDLING_NAY,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
        kreverToTrinn = true
    ),
    BEKREFT_TOTALVURDERING_KLAGE(
        kode = AvklaringsbehovKode.`6006`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.KLAGEBEHANDLING_OPPSUMMERING,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
        kreverToTrinn = false
    ),

    @Deprecated("Effektuer-steget er fjernet. Kun bevart for statistikk i DEV")
    EFFEKTUER_AVVIST_PÅ_FORMKRAV(
        kode = AvklaringsbehovKode.`6004`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.FORMKRAV,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
    ),
    FASTSETT_FULLMEKTIG(
        kode = AvklaringsbehovKode.`6009`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.FULLMEKTIG,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
    ),
    VURDER_TREKK_AV_KLAGE(
        kode = AvklaringsbehovKode.`6010`,
        løsesISteg = StegType.TREKK_KLAGE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesAv = listOf(
            Rolle.SAKSBEHANDLER_OPPFOLGING,
            Rolle.SAKSBEHANDLER_NASJONAL
        ),
    ),
    SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV(
        kode = AvklaringsbehovKode.`6005`,
        løsesISteg = StegType.FORMKRAV,
        type = BehovType.MANUELT_PÅKREVD,
        løsesAv = listOf(
            Rolle.SAKSBEHANDLER_NASJONAL
        )
    ),
    VENTE_PÅ_FRIST_FORHÅNDSVARSEL_KLAGE_FORMKRAV(
        kode = AvklaringsbehovKode.`6007`,
        type = BehovType.VENTEPUNKT,
        løsesISteg = StegType.FORMKRAV,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
        defaultFrist = Period.ofWeeks(3),
    ),
    HÅNDTER_SVAR_FRA_ANDREINSTANS(
        kode = AvklaringsbehovKode.`6008`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.SVAR_FRA_ANDREINSTANS,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
    ),

    // Oppfølgingsbehandling
    AVKLAR_OPPFØLGINGSBEHOV_LOKALKONTOR(
        kode = AvklaringsbehovKode.`8001`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.AVKLAR_OPPFØLGING,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING),
    ),
    AVKLAR_OPPFØLGINGSBEHOV_NAY(
        kode = AvklaringsbehovKode.`8002`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.AVKLAR_OPPFØLGING,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_NASJONAL),
    ),
    VENT_PÅ_OPPFØLGING(
        kode = AvklaringsbehovKode.`8003`,
        type = BehovType.VENTEPUNKT,
        løsesISteg = StegType.START_OPPFØLGINGSBEHANDLING,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING, Rolle.SAKSBEHANDLER_NASJONAL),
        defaultFrist = Period.ofWeeks(4),
    ),

    // Aktivitetsplikt
    VURDER_BRUDD_11_7(
        kode = AvklaringsbehovKode.`4101`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.VURDER_AKTIVITETSPLIKT_11_7,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING),
        kreverToTrinn = true
    ),
    SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV(
        kode = AvklaringsbehovKode.`5052`,
        løsesISteg = StegType.VURDER_AKTIVITETSPLIKT_11_7,
        type = BehovType.MANUELT_PÅKREVD,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)
    ),
    VENTE_PÅ_FRIST_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT(
        kode = AvklaringsbehovKode.`4102`,
        type = BehovType.VENTEPUNKT,
        løsesISteg = StegType.VURDER_AKTIVITETSPLIKT_11_7,
        løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING),
        defaultFrist = Period.ofWeeks(3),
    );

    public companion object {
        public fun forKode(definisjon: String): Definisjon {
            return entries.single { it.kode == AvklaringsbehovKode.valueOf(definisjon) }
        }

        public fun forKode(definisjon: AvklaringsbehovKode): Definisjon {
            return entries.single { it.kode == definisjon }
        }

        public fun fraStegType(steg: StegType): List<Definisjon> {
            return entries.filter { it.løsesISteg == steg }
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

        @JsonCreator
        @JvmStatic
        public fun fraKode(@JsonProperty("kode") kode: AvklaringsbehovKode): Definisjon = forKode(kode)
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
         * Brevpunkter
         */
        BREV(Definisjon::validerBrevpunkt),

        @Deprecated("Ikke i bruk lenger")
        BREV_VENTEPUNKT(Definisjon::validerBrevVentepunkt),

        /**
         * Ventebehov kan opprettes av saksbehandler og system, det er et behov som venter på tid og/eller en hendelse
         * (f.eks et dokument)
         */
        VENTEPUNKT(Definisjon::validerVentepunkt),

        /**
         * Overstyr er at saksbehandler kan overstyre automatiske vurderinger og trigge behovet
         * (f.eks et dokument)
         */
        OVERSTYR(Definisjon::validerManuelt)
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

    private fun validerBrevpunkt() {
        if (this == SKRIV_BREV && !this.løsesISteg.tekniskSteg) {
            throw IllegalArgumentException(
                "Brevbehov må være knyttet til et teknisk steg"
            )
        }
    }

    private fun validerBrevVentepunkt() {
        if (this == BESTILL_BREV && !this.løsesISteg.tekniskSteg) {
            throw IllegalArgumentException(
                "Brev-ventebehov må være knyttet til et teknisk steg"
            )
        }
    }

    private fun validerVentepunkt() {
        if (this in setOf(
                MANUELT_SATT_PÅ_VENT,
                BESTILL_LEGEERKLÆRING,
                OPPRETT_HENDELSE_PÅ_SAK
            )
        ) {
            if (this.løsesISteg != StegType.UDEFINERT) {
                throw IllegalArgumentException("Ventepunkt er lagt til feil steg")
            }
        } else {
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
        return type == BehovType.VENTEPUNKT || erBrevVentebehov()
    }

    public fun erOverstyring(): Boolean {
        return type == BehovType.OVERSTYR
    }

    public fun erBrevVentebehov(): Boolean {
        return type == BehovType.BREV_VENTEPUNKT
    }

    public fun erAutomatisk(): Boolean {
        return this == BESTILL_BREV
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
