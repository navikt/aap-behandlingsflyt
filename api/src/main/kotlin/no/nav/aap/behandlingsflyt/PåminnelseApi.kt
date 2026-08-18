package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.KandidatForPåminnelseRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDate
import javax.sql.DataSource

// Det skal sendes påminnelse i dag for bestillinger opprettet for tre uker og én dag siden
private val bestillingOpprettetDatoForPurringIDag = if (Miljø.erProd()) {
    LocalDate.now().minusWeeks(3).minusDays(1)
} else {
    LocalDate.now().minusDays(1)
}

fun NormalOpenAPIRoute.påminnelseApi(
    dataSource: DataSource, repositoryRegistry: RepositoryRegistry
) {
    route("/api/dokumentinnhenting/paaminnelse") {
        authorizedGet<Unit, List<BehandlingReferanse>>(
            AuthorizationMachineToMachineConfig(authorizedRoles = listOf("finn-kandidater-for-paaminnelse"))
        ) { _ ->
            val behandlingsreferanser = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val kandidatForPåminnelseRepository = repositoryProvider.provide<KandidatForPåminnelseRepository>()

                kandidatForPåminnelseRepository.finnKandidaterForPåminnelse(
                    LocalDate.now(),
                    bestillingOpprettetDato = bestillingOpprettetDatoForPurringIDag
                )
            }
            respond(behandlingsreferanser)
        }
    }
}