package no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov

/**
 * Disse verdiene må igjen gjenspeile enumene under
 */
public const val MANUELT_SATT_PÅ_VENT_KODE: String = "9001"
public const val BESTILL_BREV_KODE: String = "9002"
public const val BESTILL_LEGEERKLÆRING_KODE: String = "9003"
public const val OPPRETT_HENDELSE_PÅ_SAK_KODE: String = "9004"
public const val VURDER_RETTIGHETSPERIODE_KODE: String = "5029"
public const val AVKLAR_STUDENT_KODE: String = "5001"
public const val AVKLAR_SYKDOM_KODE: String = "5003"
public const val FASTSETT_ARBEIDSEVNE_KODE: String = "5004"
public const val FRITAK_MELDEPLIKT_KODE: String = "5005"
public const val AVKLAR_BISTANDSBEHOV_KODE: String = "5006"
public const val VURDER_SYKEPENGEERSTATNING_KODE: String = "5007"
public const val FASTSETT_BEREGNINGSTIDSPUNKT_KODE: String = "5008"
public const val AVKLAR_BARNETILLEGG_KODE: String = "5009"
public const val AVKLAR_SONINGSFORRHOLD_KODE: String = "5010"
public const val AVKLAR_HELSEINSTITUSJON_KODE: String = "5011"
public const val AVKLAR_SAMORDNING_GRADERING_KODE: String = "5012"
public const val SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT: String = "5025"
public const val AVKLAR_SAMORDNING_UFØRE_KODE: String = "5024"
public const val AVKLAR_SAMORDNING_ANDRE_STATLIGE_YTELSER_KODE: String = "5027"
public const val AVKLAR_YRKESSKADE_KODE: String = "5013"
public const val FASTSETT_YRKESSKADE_BELØP_KODE: String = "5014"
public const val EFFEKTUER_11_7_KODE: String = "5015"
public const val FORHÅNDSVARSEL_AKTIVITETSPLIKT_KODE: String = "5016"
public const val KVALITETSSIKRING_KODE: String = "5097"
public const val FORESLÅ_VEDTAK_KODE: String = "5098"
public const val FATTE_VEDTAK_KODE: String = "5099"
public const val SKRIV_BREV_KODE: String = "5050"
public const val SKRIV_VEDTAKSBREV_KODE: String = "5051"
public const val SKRIV_FORHÅNDSVARSEL_AKTIVITETSPLIKT_BREV_KODE: String = "5052"
public const val AVKLAR_LOVVALG_MEDLEMSKAP_KODE: String = "5017"
public const val VENTE_PÅ_FIRST_EFFEKTUER_11_7_KODE: String = "5018"
public const val AVKLAR_UTENLANDSK_MEDLEMSKAP_KODE: String = "5019"
public const val AVKLAR_FORUTGÅENDE_MEDLEMSKAP_KODE: String = "5020"
public const val MANUELL_OVERSTYRING_LOVVALG: String = "5021"
public const val MANUELL_OVERSTYRING_MEDLEMSKAP: String = "5022"
public const val VURDER_TREKK_AV_SØKNAD_KODE: String = "5028"
public const val REFUSJON_KRAV: String = "5026"
public const val FASTSETT_PÅKLAGET_BEHANDLING_KODE: String = "5999"
public const val SAMORDNING_REFUSJONS_KRAV = "5056"
public const val FASTSETT_MANUELL_INNTEKT: String = "7001"
public const val VURDER_FORMKRAV_KODE: String = "6000"
public const val FASTSETT_BEHANDLENDE_ENHET_KODE: String = "6001"
public const val VURDER_KLAGE_KONTOR_KODE: String = "6002"
public const val VURDER_KLAGE_NAY_KODE: String = "6003"

@Suppress("EnumEntryName")
public enum class AvklaringsbehovKode {
    /**
     * [MANUELT_SATT_PÅ_VENT_KODE]
     */
    `9001`,

    /**
     * [BESTILL_BREV_KODE]
     */
    `9002`,

    /**
     * [BESTILL_LEGEERKLÆRING_KODE]
     */
    `9003`,

    /**
     * [OPPRETT_HENDELSE_PÅ_SAK_KODE]
     */
    `9004`,

    /**
     * [VURDER_RETTIGHETSPERIODE_KODE]
     */
    `5029`,

    /**
     * [AVKLAR_STUDENT_KODE]
     */
    `5001`,

    /**
     * [AVKLAR_SYKDOM_KODE]
     */
    `5003`,

    /**
     * [FASTSETT_ARBEIDSEVNE_KODE]
     */
    `5004`,

    /**
     * [FRITAK_MELDEPLIKT_KODE]
     */
    `5005`,

    /**
     * [AVKLAR_BISTANDSBEHOV_KODE]
     */
    `5006`,

    /**
     * [VURDER_SYKEPENGEERSTATNING_KODE]
     */
    `5007`,
    `5008`,
    `5009`,
    `5010`,
    `5011`,
    `5012`,
    `5013`,
    `5014`,
    `5015`,
    `5016`,
    `5017`,
    `5018`,
    `5020`,
    `5024`,
    `5097`,
    `5098`,
    `5099`,

    /**
     * [MANUELL_OVERSTYRING_LOVVALG]
     */
    `5021`,

    /**
     * [MANUELL_OVERSTYRING_MEDLEMSKAP]
     */
    `5022`,
    `5023`,

    /**
     * [SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT], for [Definisjon.SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT].
     */
    `5025`,

    /**
     * [AVKLAR_SAMORDNING_ANDRE_STATLIGE_YTELSER_KODE]
     */
    `5027`,

    /**
     * [VURDER_TREKK_AV_SØKNAD_KODE]
     */
    `5028`,

    /**
     * [AVKLAR_UTENLANDSK_MEDLEMSKAP_KODE]
     */
    `5019`,

    /**
     * [SKRIV_BREV_KODE]
     */
    `5050`,

    /**
     * [SKRIV_VEDTAKSBREV_KODE]
     */
    `5051`,

    /**
     * [SKRIV_FORHÅNDSVARSEL_AKTIVITETSPLIKT_BREV_KODE]
     */
    `5052`,

    /**
     * [REFUSJON_KRAV]
     * */
    `5026`,

    /**
     * [FASTSETT_PÅKLAGET_BEHANDLING_KODE]
     */
    `5999`,

    /**
     * [SAMORDNING_REFUSJONS_KRAV]
     * */
    `5056`,

    /**
     * [VURDER_FORMKRAV_KODE]
     */
    `6000`,

    /**
     * [FASTSETT_BEHANDLENDE_ENHET_KODE]
     */
    `6001`,

    /**
     * [VURDER_KLAGE_KONTOR_KODE]
     */
    `6002`,

    /**
     * [VURDER_KLAGE_NAY_KODE]
     */
    `6003`,

    /**
     * [FASTSETT_MANUELL_INNTEKT]
     */
    `7001`
}
