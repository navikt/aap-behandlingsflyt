package no.nav.aap.behandlingsflyt.behandling.dialogmelding

// TODO: Fjern og bruk type fra kontrakt etter deploy av dokumentinnhenting!
data class HentDialogmeldingerForSakParams(val saksnummer: String)

// TODO: Fjern og bruk type fra kontrakt etter deploy av dokumentinnhenting!
data class HentDokumentoversiktJournalpostListeParams(val journalpostIdListe: List<String>)
