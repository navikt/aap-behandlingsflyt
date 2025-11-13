package no.nav.aap.behandlingsflyt.behandling.oppfolgingsbehandling

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Opprinnelse
import java.util.UUID

data class OppfølgningOppgaveOpprinnelseResponse(
    val data: List<OppfølgningOppgaveOpprinnselseDto>
)

data class BehandlingReferanseMedSteg(
    @param:PathParam("referanse") val referanse: UUID,
    @param:PathParam("avklaringsbehovKode") val avklaringsbehovKode: String
)

data class OppfølgningOppgaveOpprinnselseDto(
    val behandlingReferanse: String,
    val opprinnelse: Opprinnelse
)