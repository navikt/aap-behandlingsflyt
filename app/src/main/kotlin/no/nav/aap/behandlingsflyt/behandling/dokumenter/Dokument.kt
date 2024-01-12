package no.nav.aap.behandlingsflyt.behandling.dokumenter

import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

class Dokument(private val id: Long,
               private val sakId: Long,
               private val behandlingId: Long,
               private val innsendingstidspunkt: LocalDateTime,
               private val brevkode: Brevkode,
               private val journalpostId: JournalpostId
)
