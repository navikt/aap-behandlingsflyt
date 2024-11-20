package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId

class UbehandletLegeerklæring(
    val journalpostId: JournalpostId,
    val periode: Periode
)

class UbehandletDialogmelding(
    val journalpostId: JournalpostId,
    val periode: Periode
)