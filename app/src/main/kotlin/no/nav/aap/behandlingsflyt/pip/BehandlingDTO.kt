package no.nav.aap.behandlingsflyt.pip

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.UUID

internal data class BehandlingDTO(@PathParam("behandlingsnummer") val behandlingsnummer: UUID)
