package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import java.time.LocalDate

sealed class BrevBehov(val typeBrev: TypeBrev)

data class Innvilgelse(
    val virkningstidspunkt: LocalDate,
    val sisteDagMedYtelse: LocalDate,
    val grunnlagBeregning: GrunnlagBeregning?,
    val tilkjentYtelse: TilkjentYtelse?,
    val sykdomsvurdering: String?
) : BrevBehov(TypeBrev.VEDTAK_INNVILGELSE)

data class VurderesForUføretrygd(
    val kravdatoUføretrygd: LocalDate,
    val sisteDagMedYtelse: LocalDate,
    val grunnlagBeregning: GrunnlagBeregning?,
    val tilkjentYtelse: TilkjentYtelse?
) : BrevBehov(TypeBrev.VEDTAK_11_18)

data class Arbeidssøker(
    val datoAvklartForJobbsøk: LocalDate,
    val sisteDagMedYtelse: LocalDate,
    val tilkjentYtelse: TilkjentYtelse?,
) : BrevBehov(TypeBrev.VEDTAK_11_17)

data class UtvidVedtakslengdeEttÅr(
    override val utvidetAapFomDato: LocalDate,
    override val sisteDagMedYtelse: LocalDate,
) : BrevBehov(TypeBrev.VEDTAK_UTVID_VEDTAKSLENGDE), UtvidVedtakslengde

data class UtvidVedtakslengdeUnderEttÅrMedlemskap(
    override val utvidetAapFomDato: LocalDate,
    override val sisteDagMedYtelse: LocalDate,
) : BrevBehov(TypeBrev.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_MEDLEMSKAP), UtvidVedtakslengde

data class UtvidVedtakslengdeUnderEttÅrOppholdskrav(
    override val utvidetAapFomDato: LocalDate,
    override val sisteDagMedYtelse: LocalDate,
) : BrevBehov(TypeBrev.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_3), UtvidVedtakslengde

data class UtvidVedtakslengdeUnderEttÅrBrukerOver67(
    override val utvidetAapFomDato: LocalDate,
    override val sisteDagMedYtelse: LocalDate,
) : BrevBehov(TypeBrev.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_4), UtvidVedtakslengde

data class UtvidVedtakslengdeUnderEttÅrOrdinærkvoteBruktOpp(
    override val utvidetAapFomDato: LocalDate,
    override val sisteDagMedYtelse: LocalDate,
) : BrevBehov(TypeBrev.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_12), UtvidVedtakslengde

data class UtvidVedtakslengdeUnderEttÅrStraffegjennomføring(
    override val utvidetAapFomDato: LocalDate,
    override val sisteDagMedYtelse: LocalDate,
) : BrevBehov(TypeBrev.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_26), UtvidVedtakslengde

data class UtvidVedtakslengdeUnderEttÅrAnnenFullYtelse(
    override val utvidetAapFomDato: LocalDate,
    override val sisteDagMedYtelse: LocalDate,
) : BrevBehov(TypeBrev.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_27), UtvidVedtakslengde

data class Avslag(val sykdomsvurdering: String?): BrevBehov(TypeBrev.VEDTAK_AVSLAG)
object VedtakEndring : BrevBehov(TypeBrev.VEDTAK_ENDRING)
object BarnetilleggSatsRegulering : BrevBehov(TypeBrev.BARNETILLEGG_SATS_REGULERING)
object VarselOmBestilling : BrevBehov(TypeBrev.VARSEL_OM_BESTILLING)
object ForhåndsvarselBruddAktivitetsplikt : BrevBehov(TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT)
object ForhåndsvarselKlageFormkrav : BrevBehov(TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV)
object KlageAvvist : BrevBehov(TypeBrev.KLAGE_AVVIST)
object KlageOpprettholdelse : BrevBehov(TypeBrev.KLAGE_OPPRETTHOLDELSE)
object KlageTrukket : BrevBehov(TypeBrev.KLAGE_TRUKKET)
object KlageMottatt : BrevBehov(TypeBrev.KLAGE_MOTTATT)
object Forvaltningsmelding : BrevBehov(TypeBrev.FORVALTNINGSMELDING)
object VedtakAktivitetsplikt11_7 : BrevBehov(TypeBrev.VEDTAK_11_7)
object VedtakAktivitetsplikt11_9 : BrevBehov(TypeBrev.VEDTAK_11_9)
object VedtakArbeidsopptrapping11_23SjetteLedd : BrevBehov(TypeBrev.VEDTAK_11_23_SJETTE_LEDD)

interface UtvidVedtakslengde {
    val utvidetAapFomDato: LocalDate
    val sisteDagMedYtelse: LocalDate
}