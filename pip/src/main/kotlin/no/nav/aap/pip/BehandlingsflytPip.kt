package no.nav.aap.pip

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.pip.PipRepository.IdentPåSak.Companion.filterDistinctIdent
import no.nav.aap.tilgang.authorizedGetWithApprovedList
import javax.sql.DataSource

fun NormalOpenAPIRoute.behandlingsflytPip(dataSource: DataSource) {
    val tilgangAzp = requiredConfigForKey("integrasjon.tilgang.azp")
    route("/pip/api") {
        route("/sak/{saksnummer}/identer") {
            authorizedGetWithApprovedList<SakDTO, IdenterDTO>(tilgangAzp) { req ->
                val saksnummer = req.saksnummer
                val (søker, barn) = dataSource.transaction(readOnly = true) { connection ->
                    val søker = SakRepositoryImpl(connection).finnSøker(Saksnummer(saksnummer))
                    val barn = BarnRepository(connection).hentHvisEksisterer(Saksnummer(saksnummer))
                    søker to barn
                }
                respond(
                    IdenterDTO(
                        søker = søker.identer().map { it.identifikator },
                        barn = barn?.tilUnikeIdentIdentifikatorer() ?: emptyList(),
                    )
                )
            }
        }

        route("/behandling/{behandlingsnummer}/identer") {
            authorizedGetWithApprovedList<BehandlingDTO, IdenterDTO>(tilgangAzp) { req ->
                val behandlingsnummer = req.behandlingsnummer
                val (søker, barn) = dataSource.transaction(readOnly = true) { connection ->
                    val søker = BehandlingRepositoryImpl(connection).finnSøker(BehandlingReferanse(behandlingsnummer))
                    val barn =
                        BarnRepository(connection).hentHvisEksistererForTilgangPip(BehandlingReferanse(behandlingsnummer))
                    søker to barn
                }
                respond(
                    IdenterDTO(
                        søker = søker.identer().map { it.identifikator },
                        barn = barn.flatMap { it.tilUnikeIdentIdentifikatorer() }.distinct(),
                    )
                )
            }
        }
    }
    route("/pip/api2") {
        route("/sak/{saksnummer}/identer") {
            authorizedGetWithApprovedList<SakDTO, IdenterDTO>(tilgangAzp) { req ->
                val saksnummer = req.saksnummer
                val identer = dataSource.transaction(readOnly = true) { connection ->
                    PipRepository(connection).finnIdenterPåSak(Saksnummer(saksnummer))
                }
                respond(
                    IdenterDTO(
                        søker = identer.filterDistinctIdent(PipRepository.IdentPåSak.Opprinnelse.PERSON),
                        barn = identer.filterDistinctIdent(PipRepository.IdentPåSak.Opprinnelse.BARN)
                    )
                )
            }
        }

        route("/behandling/{behandlingsnummer}/identer") {
            authorizedGetWithApprovedList<BehandlingDTO, IdenterDTO>(tilgangAzp) { req ->
                val behandlingsnummer = req.behandlingsnummer
                val identer = dataSource.transaction(readOnly = true) { connection ->
                    PipRepository(connection).finnIdenterPåBehandling(BehandlingReferanse(behandlingsnummer))
                }
                respond(
                    IdenterDTO(
                        søker = identer.filterDistinctIdent(PipRepository.IdentPåSak.Opprinnelse.PERSON),
                        barn = identer.filterDistinctIdent(PipRepository.IdentPåSak.Opprinnelse.BARN)
                    )
                )
            }
        }
    }
}

private fun BarnGrunnlag.tilUnikeIdentIdentifikatorer(): List<String> {
    val oppgitteBarnIds = oppgitteBarn?.identer?.map { it.identifikator } ?: emptyList()
    val registerBarnIds = registerbarn?.identer?.map { it.identifikator } ?: emptyList()
    val vurderteBarnIds = vurderteBarn?.barn?.map { it.ident.identifikator } ?: emptyList()
    return (oppgitteBarnIds + registerBarnIds + vurderteBarnIds).toSet().toList()
}
