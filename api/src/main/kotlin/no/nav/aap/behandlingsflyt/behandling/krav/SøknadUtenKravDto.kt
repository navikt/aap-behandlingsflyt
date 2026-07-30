package no.nav.aap.behandlingsflyt.behandling.krav

import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

data class SøknadUtenKravDto(
    val journalpostId: JournalpostId,
    val mottattTidspunkt: LocalDateTime
)