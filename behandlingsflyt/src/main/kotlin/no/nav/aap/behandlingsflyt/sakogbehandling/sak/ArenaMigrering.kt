package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakMedVedtakResponse
import java.time.LocalDateTime

data class ArenaMigrering(
    val sakId: SakId,
    val saksnummerArena: String,
    val ident: String,
    val migrertTidspunkt: LocalDateTime,
    val arenaSakData: ArenaSakMedVedtakResponse? = null,
)
