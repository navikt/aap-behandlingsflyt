package no.nav.aap.behandlingsflyt.behandling.oppfolgingsbehandling

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDateTime
import java.util.UUID



data class BehandlingReferanseMedSteg(@param:PathParam("referanse") val referanse: UUID, @param:PathParam("avklaringsbehovKode") val avklaringsbehovKode: String)
