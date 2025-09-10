package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.komponenter.verdityper.Beløp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

sealed class BrevBehov(val typeBrev: TypeBrev)

data class Innvilgelse(
    val virkningstidspunkt: LocalDate,
    val grunnlagBeregning: GrunnlagBeregning?,
    val tilkjentYtelse: TilkjentYtelse?,
) : BrevBehov(TypeBrev.VEDTAK_INNVILGELSE) {
    data class TilkjentYtelse(
        val dagsats: Beløp,
        val gradertDagsats: Beløp,
        val barnetillegg: Beløp,
        val gradertBarnetillegg: Beløp,
        val gradertDagsatsInkludertBarnetillegg: Beløp,
        val antallBarn: Int,
        val barnetilleggsats: Beløp
    )

    data class GrunnlagBeregning(
        val beregningstidspunkt: LocalDate?,
        val inntekterPerÅr: List<InntektPerÅr>,
        val beregningsgrunnlag: Beløp?,
    ) {
        data class InntektPerÅr(val år: Year, val inntekt: BigDecimal)
    }
}

object VurderesForUføretrygd : BrevBehov(TypeBrev.VEDTAK_VURDERES_FOR_UFØRETRYGD)
object Avslag : BrevBehov(TypeBrev.VEDTAK_AVSLAG)
object VedtakEndring : BrevBehov(TypeBrev.VEDTAK_ENDRING)
object VarselOmBestilling : BrevBehov(TypeBrev.VARSEL_OM_BESTILLING)
object ForhåndsvarselBruddAktivitetsplikt : BrevBehov(TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT)
object ForhåndsvarselKlageFormkrav : BrevBehov(TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV)
object KlageAvvist : BrevBehov(TypeBrev.KLAGE_AVVIST)
object KlageOpprettholdelse : BrevBehov(TypeBrev.KLAGE_OPPRETTHOLDELSE)
object KlageTrukket : BrevBehov(TypeBrev.KLAGE_TRUKKET)
object Forvaltningsmelding : BrevBehov(TypeBrev.FORVALTNINGSMELDING)
