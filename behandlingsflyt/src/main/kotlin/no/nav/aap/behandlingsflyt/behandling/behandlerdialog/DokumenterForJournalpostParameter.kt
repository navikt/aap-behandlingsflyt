package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class DokumenterForJournalpostParameter(@param:PathParam("journalpostId") val journalpostId: String)