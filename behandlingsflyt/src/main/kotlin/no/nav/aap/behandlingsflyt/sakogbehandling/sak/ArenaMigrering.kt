package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import java.time.LocalDateTime

data class ArenaMigrering(
    val sakId: SakId,
    val saksnummerArena: String,
    val ident: String,
    val migrertTidspunkt: LocalDateTime,
)
