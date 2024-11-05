package no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov

/**
 * Disse verdiene må igjen gjenspeile enumene under
 */
const val MANUELT_SATT_PÅ_VENT_KODE = "9001"
const val BESTILL_BREV_KODE = "9002"
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
const val SKRIV_BREV_KODE = "5050"

enum class AvklaringsbehovKode {
    `9001`,
    `9002`,
    `5001`,
    `5003`,
    `5004`,
    `5005`,
    `5006`,
    `5007`,
    `5008`,
    `5009`,
    `5010`,
    `5011`,
    `5012`,
    `5097`,
    `5098`,
    `5099`,
    `5050`
}