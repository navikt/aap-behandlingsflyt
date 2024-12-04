package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class HentDokumentDTO(
    @PathParam(description = "Journalpost-ID") val journalpostId: String,
    @PathParam(description = "Dokumentinfo-ID") val dokumentinfoId: String
)
