package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

import no.nav.aap.dokumentinnhenting.kontrakt.BegrensetJournalpostDto

// TODO: Fjern og bruk type fra kontrakt etter deploy av dokumentinnhenting!
data class HentDokumentoversiktJournalpostResponse(val journalpost: BegrensetJournalpostDto?)