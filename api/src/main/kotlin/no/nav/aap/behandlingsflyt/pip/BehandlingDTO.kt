package no.nav.aap.behandlingsflyt.pip

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.*

internal data class BehandlingDTO(@param:PathParam("behandlingsnummer") val behandlingsnummer: UUID)
