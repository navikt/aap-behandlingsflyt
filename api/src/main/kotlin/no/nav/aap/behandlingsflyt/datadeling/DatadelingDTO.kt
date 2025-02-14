package no.nav.aap.behandlingsflyt.datadeling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import java.time.LocalDate

data class SakStatus(
    val sakId: String,
    val vedtakStatusKode: VedtakStatus,
    val periode: Maksimum.Periode,
    val kilde: String = "Kelvin"
) {
    companion object {
        fun fromKelvin(saksnummer: String, status: Status, periode: Maksimum.Periode): SakStatus {
            return SakStatus(
                sakId = saksnummer,
                vedtakStatusKode = SakStatus.fromStatus(status),
                periode = periode,
            )
        }

        private fun fromStatus(status: Status): SakStatus.VedtakStatus {
            return when (status) {
                Status.AVSLUTTET -> VedtakStatus.AVSLU
                Status.UTREDES -> VedtakStatus.REGIS
                Status.LØPENDE -> VedtakStatus.IVERK
                Status.OPPRETTET -> VedtakStatus.OPPRE
                else -> {
                    VedtakStatus.UKJENT
                }
            }
        }
    }

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SakStatus

        if (sakId != other.sakId) return false
        if (vedtakStatusKode != other.vedtakStatusKode) return false
        if (periode != other.periode) return false
        if (kilde != other.kilde) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sakId.hashCode()
        result = 31 * result + vedtakStatusKode.hashCode()
        result = 31 * result + periode.hashCode()
        result = 31 * result + kilde.hashCode()
        return result
    }
}


data class Maksimum(
    val vedtak: List<Vedtak>
) {

    data class Vedtak(
        val vedtaksId: String,
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
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Periode

            if (fraOgMedDato != other.fraOgMedDato) return false
            if (tilOgMedDato != other.tilOgMedDato) return false

            return true
        }

        override fun hashCode(): Int {
            var result = fraOgMedDato?.hashCode() ?: 0
            result = 31 * result + (tilOgMedDato?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "Periode(fraOgMedDato=$fraOgMedDato, tilOgMedDato=$tilOgMedDato)"
        }

    }

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