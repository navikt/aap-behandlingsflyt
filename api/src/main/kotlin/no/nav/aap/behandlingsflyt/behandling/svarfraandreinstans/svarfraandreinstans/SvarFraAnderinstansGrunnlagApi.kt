package no.nav.aap.behandlingsflyt.behandling.svarfraandreinstans.svarfraandreinstans

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.svarFraAnderinstansGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("api/svar-fra-andreinstans/{referanse}/grunnlag/svar-fra-andreinstans") {
        authorizedGet<BehandlingReferanse, SvarFraAnderinstansGrunnlagDto>(
            AuthorizationParamPathConfig(
                behandlingPathParam = BehandlingPathParam(
                    "referanse"
                )
            )
        ) { req ->
            val respons = dataSource.transaction(readOnly = true) {
                val repositoryProvider = repositoryRegistry.provider(it)
                val behandling = repositoryProvider.provide<BehandlingRepository>()
                    .hent(req)

                val hendelse = repositoryProvider.provide<MottattDokumentRepository>()
                    .hentDokumenterAvType(behandling.id, InnsendingType.KABAL_HENDELSE)
                    .first()
                    .strukturerteData<KabalHendelseV0>()?.data
                requireNotNull(hendelse) { "Fant ikke tilhørende kabalhendelse" }


                SvarFraAnderinstansGrunnlagDto(
                    svarFraAndreinstans = hendelse.tilDto()
                )
            }

            respond(respons)
        }
    }
}