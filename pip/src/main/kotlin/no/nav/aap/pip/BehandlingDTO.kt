package no.nav.aap.pip

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.UUID

data class BehandlingDTO(@PathParam("behandlingsnummer") val behandlingsnummer: UUID)
