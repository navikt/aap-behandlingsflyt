package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import java.util.UUID

data class HentUbesvarteForespørslerLegeerklæringDto(
    @param:PathParam("behandlingsReferanse") val behandlingsReferanse: UUID
)