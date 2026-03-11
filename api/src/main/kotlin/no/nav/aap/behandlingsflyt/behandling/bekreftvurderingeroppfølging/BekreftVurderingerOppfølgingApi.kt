package no.nav.aap.behandlingsflyt.behandling.bekreftvurderingeroppfølging

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurderingService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Rolle
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.bekreftVurderingerOppfølgingApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/bekreft-vurderinger-oppfolging") {
            getGrunnlag<BehandlingReferanse, BekreftVurderingerOppfølgingDto>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.BEKREFT_VURDERINGER_OPPFØLGING.kode.toString()
            ) { req ->
                val respons = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val mellomlagretVurderingerService = MellomlagretVurderingService(repositoryProvider)
                    val veiledersMellomlagredeVurderinger =
                        mellomlagretVurderingerService.hentMellomlagredeVurderingerFørSteg(
                            req,
                            Definisjon.BEKREFT_VURDERINGER_OPPFØLGING.løsesISteg,
                            løsesAv = listOf(Rolle.SAKSBEHANDLER_OPPFOLGING)
                        )
                    BekreftVurderingerOppfølgingDto(veiledersMellomlagredeVurderinger)
                }
                respond(respons)
            }
        }
    }
}
                    
