package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.BestillLegeerklæringDto
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.ForhåndsvisBrevRequest
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.HentStatusLegeerklæring
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.PurringLegeerklæring
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.BrevRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.BrevResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumeninnhentingGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringBestillingRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringPurringRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringStatusResponse
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.MDC
import tilgang.Operasjon
import java.time.LocalDate
import java.time.Period
import javax.sql.DataSource

fun NormalOpenAPIRoute.dokumentinnhentingAPI(dataSource: DataSource) {
    val saksbehandlingAzp = requiredConfigForKey("integrasjon.saksbehandling.azp")
    route("/api/dokumentinnhenting/syfo") {
        route("/bestill") {
            authorizedPost<Unit, String, BestillLegeerklæringDto>(
                AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SAKSBEHANDLE,
                    approvedApplications = setOf(saksbehandlingAzp),
                    applicationsOnly = true
                )
            )
            { _, req ->
                val bestillingUuid = dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)

                    val låsRepository = repositoryProvider.provide(TaSkriveLåsRepository::class)
                    val lås = låsRepository.lås(req.behandlingsReferanse)
                    var bestillingUUID: String?

                    MDC.putCloseable("sakId", lås.sakSkrivelås.id.toString()).use {
                        MDC.putCloseable("behandlingId", lås.behandlingSkrivelås.id.toString()).use {
                            val sak =
                                repositoryProvider.provide(SakRepository::class).hent((Saksnummer(req.saksnummer)))
                            val behandling = repositoryProvider.provide(BehandlingRepository::class)
                                .hent(BehandlingReferanse(req.behandlingsReferanse))
                            val avklaringsbehovene = repositoryProvider.provide(AvklaringsbehovRepository::class)
                                .hentAvklaringsbehovene(behandling.id)

                            val personIdent = sak.person.aktivIdent()
                            val personinfo =
                                GatewayProvider.provide(PersoninfoGateway::class)
                                    .hentPersoninfoForIdent(personIdent, token())

                            avklaringsbehovene.validateTilstand(behandling = behandling)
                            avklaringsbehovene.leggTil(
                                definisjoner = listOf(Definisjon.BESTILL_LEGEERKLÆRING),
                                funnetISteg = behandling.aktivtSteg(),
                                grunn = ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
                                bruker = bruker(),
                                frist = LocalDate.now() + Period.ofWeeks(4),
                            )
                            avklaringsbehovene.validerPlassering(behandling = behandling)

                            val sakService = SakService(repositoryProvider.provide(SakRepository::class))
                            val behandlingHendelseService =
                                BehandlingHendelseServiceImpl(FlytJobbRepository((connection)), sakService)

                            behandlingHendelseService.stoppet(behandling, avklaringsbehovene)

                            bestillingUUID = DokumeninnhentingGateway().bestillLegeerklæring(
                                LegeerklæringBestillingRequest(
                                    req.behandlerRef,
                                    req.behandlerNavn,
                                    req.behandlerHprNr,
                                    req.veilederNavn,
                                    personIdent.identifikator,
                                    personinfo.fulltNavn(),
                                    req.fritekst,
                                    req.saksnummer,
                                    req.dokumentasjonType,
                                    req.behandlingsReferanse
                                )
                            )

                            låsRepository.verifiserSkrivelås(lås)
                        }
                    }
                    bestillingUUID
                }

                respond(requireNotNull(bestillingUuid))
            }
        }
        route("/status/{saksnummer}") {
            authorizedGet<HentStatusLegeerklæring, List<LegeerklæringStatusResponse>>(
                AuthorizationParamPathConfig(
                    approvedApplications = setOf(saksbehandlingAzp),
                    applicationsOnly = true
                )
            ) { par ->
                val status = DokumeninnhentingGateway().legeerklæringStatus(par.saksnummer)
                respond(status)
            }
        }
        route("/brevpreview") {
            authorizedPost<Unit, BrevResponse, ForhåndsvisBrevRequest>(
                AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SAKSBEHANDLE,
                    approvedApplications = setOf(saksbehandlingAzp),
                    applicationsOnly = true,
                )
            ) { _, req ->
                val brevPreview = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = RepositoryProvider(connection).provide(SakRepository::class)
                    val sak = repositoryProvider.hent((Saksnummer(req.saksnummer)))

                    val personIdent = sak.person.aktivIdent()
                    val personinfo =
                        GatewayProvider.provide(PersoninfoGateway::class).hentPersoninfoForIdent(personIdent, token())

                    val brevRequest = BrevRequest(
                        personinfo.fulltNavn(),
                        personIdent.identifikator,
                        req.fritekst,
                        req.veilederNavn,
                        req.dokumentasjonType,
                    )
                    DokumeninnhentingGateway().forhåndsvisBrev(brevRequest)
                }
                respond(brevPreview)
            }
        }
        //TODO: Fikse denne til å benytte tilgangstyring, mangler journal/sak/behandlingkontekst i request
        route("/purring/{dialogmeldinguuid}") {
            post<PurringLegeerklæring, String, Unit> { par, _ ->
                val request = LegeerklæringPurringRequest(par.dialogmeldingPurringUUID)
                val bestillingUUID = DokumeninnhentingGateway().purrPåLegeerklæring(request)
                respond(bestillingUUID)
            }
        }
    }
}