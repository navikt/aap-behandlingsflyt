package no.nav.aap.domene.behandling.dokumenter

import no.nav.aap.domene.typer.JournalpostId
import java.time.LocalDateTime

class Dokument(private val id: Long,
               private val fagsakId: Long,
               private val behandlingId: Long,
               private val innsendingstidspunkt: LocalDateTime,
               private val brevkode: Brevkode,
               private val journalpostId: JournalpostId) {
}
