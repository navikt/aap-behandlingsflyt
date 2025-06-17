package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.content.type.binary.BinaryResponse
import java.io.InputStream

@BinaryResponse(contentTypes = ["application/pdf"])
data class DokumentResponsDTO(val stream: InputStream)