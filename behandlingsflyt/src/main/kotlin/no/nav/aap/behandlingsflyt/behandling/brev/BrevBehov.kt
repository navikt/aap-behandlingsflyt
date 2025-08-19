package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

sealed class BrevBehov(val typeBrev: TypeBrev)

data class Innvilgelse(
    val virkningstidspunkt: LocalDate,
    val grunnlagBeregning: GrunnlagBeregning?
) : BrevBehov(TypeBrev.VEDTAK_INNVILGELSE) {
    data class GrunnlagBeregning(
        val dagsats: BigDecimal?,
        val beregningstidspunkt: LocalDate?,
        val beregningsgrunnlagBeløp: BigDecimal?,
        val inntekterPerÅr: List<InntektPerÅr>
    ) {
        data class InntektPerÅr(val år: Year, val inntekt: BigDecimal)
    }
}

object Avslag : BrevBehov(TypeBrev.VEDTAK_AVSLAG)
object VedtakEndring : BrevBehov(TypeBrev.VEDTAK_ENDRING)
object VarselOmBestilling : BrevBehov(TypeBrev.VARSEL_OM_BESTILLING)
object ForhåndsvarselBruddAktivitetsplikt : BrevBehov(TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT)
object ForhåndsvarselKlageFormkrav : BrevBehov(TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV)
object KlageAvvist : BrevBehov(TypeBrev.KLAGE_AVVIST)
object KlageOpprettholdelse : BrevBehov(TypeBrev.KLAGE_OPPRETTHOLDELSE)
object KlageTrukket : BrevBehov(TypeBrev.KLAGE_TRUKKET)
object Forvaltningsmelding : BrevBehov(TypeBrev.FORVALTNINGSMELDING)
