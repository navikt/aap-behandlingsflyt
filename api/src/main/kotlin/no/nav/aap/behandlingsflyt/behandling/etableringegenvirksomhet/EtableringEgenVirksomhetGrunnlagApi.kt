package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.etableringEgenVirksomhetApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("/api/behandling/{referanse}/grunnlag/etableringegenvirksomhet") {
        getGrunnlag<BehandlingReferanse, EtableringEgenVirksomhetGrunnlagResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.ETABLERING_EGEN_VIRKSOMHET.kode.toString()
        ) { behandlingReferanse ->
            val response =
                dataSource.transaction { connection ->
                    EtableringEgenVirksomhetGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = true,
                        sisteVedtatteVurderinger = TODO(),
                        nyeVurderinger = TODO(),
                        kanVurderes = TODO(),
                        behøverVurderinger = TODO(),
                        kvalitetssikretAv = TODO(),
                    )
                }
            respond(
                response
            )
        }
    }
}