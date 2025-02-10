package no.nav.aap.behandlingsflyt.datadeling

import java.time.LocalDate

data class SakStatus(
    val sakId: String,
    val vedtakStatusKode: VedtakStatus,
    val periode: Maksimum.Periode,
    val kilde: String = "Kelvin"
) {
    enum class VedtakStatus {
        AVSLU,
        FORDE,
        GODKJ,
        INNST,
        IVERK,
        KONT,
        MOTAT,
        OPPRE,
        REGIS,
        UKJENT
    }
}


data class Maksimum(
    val vedtak: List<Vedtak>
) {

    data class Vedtak(
        val dagsats: Int,
        val status: String, //Hypotese, vedtaksstatuskode
        val saksnummer: String,
        val vedtaksdato: String, //reg_dato
        val periode: Periode,
        val rettighetsType: String, ////aktivitetsfase //Aktfasekode
        val beregningsgrunnlag: Int,
        val barnMedStonad: Int,
        val kildesystem: String = "Kelvin",
        val samordningsId: String? = null,
        val opphorsAarsak: String? = null,
        val vedtaksTypeKode: String,
        val vedtaksTypeNavn: String,
        val utbetaling: List<UtbetalingMedMer>,
    )

    class Periode(
        val fraOgMedDato: LocalDate?,
        val tilOgMedDato: LocalDate?
    )

    data class UtbetalingMedMer(
        val reduksjon: Reduksjon? = null,
        val utbetalingsgrad: Int? = null,
        val periode: Periode,
        val belop: Int,
        val dagsats: Int,
        val barnetilegg: Int,
    )

    data class Reduksjon(
        val timerArbeidet: Double,
        val annenReduksjon: Float
    )

}