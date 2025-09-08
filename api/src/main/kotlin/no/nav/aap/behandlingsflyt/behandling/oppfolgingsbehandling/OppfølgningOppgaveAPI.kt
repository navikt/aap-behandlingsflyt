package no.nav.aap.behandlingsflyt.behandling.oppfolgingsbehandling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.BehandlingReferanseMedAvklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurderingResponse
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Opprinnelse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource


fun NormalOpenAPIRoute.oppfølgingsOppgaveApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling").tag(Tags.Behandling) {
        route("/oppfølgningOppgaveOpprinnelse/{referanse}/{avklaringsbehovKode}") {
            authorizedGet<BehandlingReferanseMedSteg, OppfølgningOppgaveOpprinnelseResponse>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { params ->

                val behandlingsreferanse = BehandlingReferanse(params.referanse)
                val avklaringsbehovKode = AvklaringsbehovKode.valueOf(params.avklaringsbehovKode)

                val respons = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()

                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(behandlingsreferanse)

                    val alleBehandlingerPåSak = behandlingRepository.hentAlleFor(
                        sakId = behandling.sakId, behandlingstypeFilter = listOf(
                            TypeBehandling.OppfølgingsBehandling
                        )
                    )

                    alleBehandlingerPåSak.mapNotNull { behandling ->
                        MottaDokumentService(repositoryProvider.provide())
                            .hentOppfølgingsBehandlingDokument(behandlingId = behandling.id)
                            ?.let { Pair(behandling.id, it) }
                    }
                }

                val relevanteOppfølgningOppgaver = respons.filter {
                    it.second.opprinnelse != null
                            && it.second.opprinnelse!!.avklaringsbehovKode == avklaringsbehovKode.name
                            && it.second.opprinnelse!!.behandlingsreferanse == behandlingsreferanse.referanse.toString()
                }

                val respondList = relevanteOppfølgningOppgaver.map {
                    OppfølgningOppgaveOpprinnselseDto(
                        behandlingReferanse = it.first.id.toString(),
                        opprinnelse = Opprinnelse(
                            it.second.opprinnelse!!.behandlingsreferanse,
                            avklaringsbehovKode = it.second.opprinnelse!!.avklaringsbehovKode
                        )
                    )

                }

                respond(OppfølgningOppgaveOpprinnelseResponse( respondList))

            }
        }

    }
}
