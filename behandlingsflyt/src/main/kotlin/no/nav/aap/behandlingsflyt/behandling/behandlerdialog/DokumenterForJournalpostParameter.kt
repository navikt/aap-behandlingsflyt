package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

import no.nav.aap.dokumentinnhenting.kontrakt.BegrensetJournalpostDto

// TODO: Fjern og bruk type fra kontrakt etter deploy av dokumentinnhenting!
data class DokumenterForJournalpostParameter(val journalpostId: String)

// TODO: Fjern og bruk type fra kontrakt etter deploy av dokumentinnhenting!
data class HentDokumentoversiktJournalpostListeResponse(val journalposter: List<BegrensetJournalpostDto>)
