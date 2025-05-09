package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.BestillLegeerklæringDto
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.ForhåndsvisBrevRequest
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.HentStatusLegeerklæring
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.PurringLegeerklæringRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.BrevRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.BrevResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumentinnhentingGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringBestillingRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringPurringRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringStatusResponse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.mdc.LogKontekst
import no.nav.aap.behandlingsflyt.mdc.LoggingKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import javax.sql.DataSource

fun NormalOpenAPIRoute.dokumentinnhentingAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    val dokumentinnhentingGateway = GatewayProvider.provide<DokumentinnhentingGateway>()
    route("/api/dokumentinnhenting/syfo") {
        route("/bestill") {
            authorizedPost<Unit, String, BestillLegeerklæringDto>(
                AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SAKSBEHANDLE,
                    applicationsOnly = false
                )
            )
            { _, req ->
                val bestillingUuid = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)

                    LoggingKontekst(
                        repositoryProvider,
                        LogKontekst(referanse = BehandlingReferanse(req.behandlingsReferanse))
                    ).use {
                        val låsRepository = repositoryProvider.provide<TaSkriveLåsRepository>()
                        val lås = låsRepository.lås(req.behandlingsReferanse)

                        val sak =
                            repositoryProvider.provide<SakRepository>().hent((Saksnummer(req.saksnummer)))
                        val behandling = repositoryProvider.provide<BehandlingRepository>()
                            .hent(BehandlingReferanse(req.behandlingsReferanse))

                        AvklaringsbehovOrkestrator(repositoryProvider)
                            .settPåVentMensVentePåMedisinskeOpplysninger(behandling.id, bruker())

                        val personIdent = sak.person.aktivIdent()
                        val personinfo =
                            GatewayProvider.provide(PersoninfoGateway::class)
                                .hentPersoninfoForIdent(personIdent, token())

                        val bestillingUUID: String = dokumentinnhentingGateway.bestillLegeerklæring(
                            LegeerklæringBestillingRequest(
                                bestillerNavIdent = bruker().ident,
                                behandlerRef = req.behandlerRef,
                                behandlerNavn = req.behandlerNavn,
                                behandlerHprNr = req.behandlerHprNr,
                                personIdent = personIdent.identifikator,
                                personNavn = personinfo.fulltNavn(),
                                dialogmeldingTekst = req.fritekst,
                                saksnummer = req.saksnummer,
                                dokumentasjonType = req.dokumentasjonType,
                                behandlingsReferanse = req.behandlingsReferanse
                            )
                        )

                        låsRepository.verifiserSkrivelås(lås)

                        bestillingUUID
                    }
                }

                respond(bestillingUuid)
            }
        }
        route("/status/{saksnummer}") {
            authorizedGet<HentStatusLegeerklæring, List<LegeerklæringStatusResponse>>(
                AuthorizationParamPathConfig(
                    applicationsOnly = false,
                    sakPathParam = SakPathParam("saksnummer")
                )
            ) { par ->
                val status = dokumentinnhentingGateway.legeerklæringStatus(par.saksnummer)
                respond(status)
            }
        }
        route("/brevpreview") {
            authorizedPost<Unit, BrevResponse, ForhåndsvisBrevRequest>(
                AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SE,
                    applicationsOnly = false
                )
            ) { _, req ->
                val brevPreview = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection).provide<SakRepository>()
                    val sak = repositoryProvider.hent((Saksnummer(req.saksnummer)))

                    val personIdent = sak.person.aktivIdent()
                    val personinfo =
                        GatewayProvider.provide(PersoninfoGateway::class).hentPersoninfoForIdent(personIdent, token())

                    val brevRequest = BrevRequest(
                        bestillerNavIdent = bruker().ident,
                        personNavn = personinfo.fulltNavn(),
                        personIdent = personIdent.identifikator,
                        dialogmeldingTekst = req.fritekst,
                        dokumentasjonType = req.dokumentasjonType,
                    )
                    dokumentinnhentingGateway.forhåndsvisBrev(brevRequest)
                }
                respond(brevPreview)
            }
        }

        route("/purring") {
            authorizedPost<Unit, String, PurringLegeerklæringRequest>(
                AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SAKSBEHANDLE,
                    applicationsOnly = false
                )
            ) { _, req ->
                val request = LegeerklæringPurringRequest(req.dialogmeldingPurringUUID)
                val bestillingUUID = dokumentinnhentingGateway.purrPåLegeerklæring(request)
                respond(bestillingUUID)
            }
        }

        // TODO: Enten slette eller aktivere denne når vi vet hvordan bestilling-statuser skal settes til mottat
        /*
        route("/status/markerbestillingmottatt") {
            authorizedPost<Unit, LegeerklæringStatusResponse, MarkerBestillingSomMottattRequest>(
                AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SAKSBEHANDLE,
                    applicationsOnly = false
                )
            ) { _, req ->
                val request = MarkerDialogmeldingSomMottattRequest(req.dialogmeldingBestillingUUID)
                val response = dokumentinnhentingGateway.markerDialogmeldingStatusSomMottatt(request)
                respond(response)
            }
        }*/
    }
}