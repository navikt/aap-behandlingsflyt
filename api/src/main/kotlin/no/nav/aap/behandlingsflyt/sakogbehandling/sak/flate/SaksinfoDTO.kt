package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime

data class SaksinfoDTO(
    val saksnummer: String,
    val opprettetTidspunkt: LocalDateTime,
    val periode: Periode,
    val ident: String,
    val resultat: Resultat? = null
)