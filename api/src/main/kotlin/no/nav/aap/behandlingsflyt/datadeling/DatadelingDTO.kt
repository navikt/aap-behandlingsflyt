package no.nav.aap.behandlingsflyt.datadeling

import java.time.LocalDate

data class VedtakUtenUtbetalingDTO(
    val dagsats: Int,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String,
    val vedtaksdato: String, //reg_dato
    val vedtaksTypeKode: String,
    val vedtaksTypeNavn: String,
    val periode: Periode,
    val rettighetsType: String, ////aktivitetsfase //Aktfasekode
    val beregningsgrunnlag: Int,
    val barnMedStonad: Int,
    val kildesystem: String = "ARENA",
    val samordningsId: String? = null,
    val opphorsAarsak: String? = null,
) {
    class Periode(
        val fraOgMedDato: LocalDate?,
        val tilOgMedDato: LocalDate?
    )

}