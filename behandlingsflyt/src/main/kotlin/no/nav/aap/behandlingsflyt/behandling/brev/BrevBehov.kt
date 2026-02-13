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

data class UtvidVedtakslengde(
    val utvidetAapFomDato: LocalDate,
    val sisteDagMedYtelse: LocalDate
) : BrevBehov(TypeBrev.VEDTAK_UTVID_VEDTAKSLENGDE)

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
