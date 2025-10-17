package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import java.time.LocalDate

sealed class BrevBehov(val typeBrev: TypeBrev)

data class Innvilgelse(
    val virkningstidspunkt: LocalDate,
    val grunnlagBeregning: GrunnlagBeregning?,
    val tilkjentYtelse: TilkjentYtelse?,
) : BrevBehov(TypeBrev.VEDTAK_INNVILGELSE) {
}

data class VurderesForUføretrygd(val grunnlagBeregning: GrunnlagBeregning?) : BrevBehov(TypeBrev.VEDTAK_11_18)
object Arbeidssøker : BrevBehov(TypeBrev.VEDTAK_11_17)
object Avslag : BrevBehov(TypeBrev.VEDTAK_AVSLAG)
object VedtakEndring : BrevBehov(TypeBrev.VEDTAK_ENDRING)
object VarselOmBestilling : BrevBehov(TypeBrev.VARSEL_OM_BESTILLING)
object ForhåndsvarselBruddAktivitetsplikt : BrevBehov(TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT)
object ForhåndsvarselKlageFormkrav : BrevBehov(TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV)
object KlageAvvist : BrevBehov(TypeBrev.KLAGE_AVVIST)
object KlageOpprettholdelse : BrevBehov(TypeBrev.KLAGE_OPPRETTHOLDELSE)
object KlageTrukket : BrevBehov(TypeBrev.KLAGE_TRUKKET)
object Forvaltningsmelding : BrevBehov(TypeBrev.FORVALTNINGSMELDING)
object VedtakAktivitetsplikt11_7 : BrevBehov(TypeBrev.VEDTAK_11_7)
object VedtakAktivitetsplikt11_9 : BrevBehov(TypeBrev.VEDTAK_11_9)
