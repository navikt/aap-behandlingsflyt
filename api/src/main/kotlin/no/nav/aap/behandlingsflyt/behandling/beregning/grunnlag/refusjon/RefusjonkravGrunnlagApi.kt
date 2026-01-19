package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.refusjon

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.AndreUtbetalingerYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.navenheter.NavKontorService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.andreYtelserOppgittISøknad.AndreYtelserOppgittISøknadRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.harTilgangOgKanSaksbehandle
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.refusjonGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val navKontorService = NavKontorService(gatewayProvider)
    val ansattInfoService = AnsattInfoService(gatewayProvider)
    val unleashGateway = gatewayProvider.provide<UnleashGateway>()

    route("/api/behandling") {
        route("/{referanse}/grunnlag/refusjon") {
            getGrunnlag<BehandlingReferanse, RefusjonkravGrunnlagResponse>(

                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.REFUSJON_KRAV.kode.toString()
            ) { req ->

                val response = if (unleashGateway.isEnabled(BehandlingsflytFeature.SosialRefusjon)){

                        dataSource.transaction(readOnly = true) { connection ->
                            val repositoryProvider = repositoryRegistry.provider(connection)
                            val refusjonkravRepository = repositoryProvider.provide<RefusjonkravRepository>()

                            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                            val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                            val vilkårsresultatRepository =
                                repositoryProvider.provide<VilkårsresultatRepository>()

                            //TODO: Skal fikses etter prodsetting slik at det bare er gjeldendeVurderinger man skal forholde seg til
                            val gjeldendeVurderinger = refusjonkravRepository.hentHvisEksisterer(behandling.id)?.map {
                                it.tilResponse(ansattInfoService)
                            }

                            val andreYtelserRepository = repositoryProvider.provide<AndreYtelserOppgittISøknadRepository>()
                            val andreUtbetalinger = andreYtelserRepository.hentHvisEksisterer(behandling.id)

                            val gjeldendeVurdering =
                                gjeldendeVurderinger?.firstOrNull()

                            val virkningstidspunkt = try {
                                if (behandling.erYtelsesbehandling()) {
                                    VirkningstidspunktUtleder(
                                        vilkårsresultatRepository = vilkårsresultatRepository
                                    ).utledVirkningsTidspunkt(behandling.id)
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                null
                            }

                            val økonomiskSosialHjelp: Boolean? = andreUtbetalinger?.stønad?.contains(AndreUtbetalingerYtelser.ØKONOMISK_SOSIALHJELP)

                            val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
                            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

                            val unleashGateway = gatewayProvider.provide<UnleashGateway>()

                            RefusjonkravGrunnlagResponse(
                                nåværendeVirkningsTidspunkt = virkningstidspunkt,
                                harTilgangTilÅSaksbehandle = if (unleashGateway.isEnabled(BehandlingsflytFeature.KvalitetssikringsSteg)) {
                                    harTilgangOgKanSaksbehandle(kanSaksbehandle(), avklaringsbehovene)
                                } else {
                                    kanSaksbehandle()
                                },
                                gjeldendeVurdering = gjeldendeVurdering,
                                gjeldendeVurderinger = gjeldendeVurderinger,
                                økonomiskSosialHjelp = økonomiskSosialHjelp,
                                historiskeVurderinger = null
                            )
                        }

                } else {
                    dataSource.transaction(readOnly = true) { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val refusjonkravRepository = repositoryProvider.provide<RefusjonkravRepository>()

                        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                        val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                        //TODO: Skal fikses etter prodsetting slik at det bare er gjeldendeVurderinger man skal forholde seg til
                        val gjeldendeVurderinger = refusjonkravRepository.hentHvisEksisterer(behandling.id)?.map {
                            it.tilResponse(ansattInfoService)
                        }
                        val gjeldendeVurdering =
                            gjeldendeVurderinger?.firstOrNull()
                        val historiskeVurderinger =
                            refusjonkravRepository
                                .hentHistoriskeVurderinger(behandling.sakId, behandling.id)
                                .map { it.tilResponse(ansattInfoService) }

                        val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
                        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

                        RefusjonkravGrunnlagResponse(
                            harTilgangTilÅSaksbehandle = harTilgangOgKanSaksbehandle(kanSaksbehandle(), avklaringsbehovene),
                            gjeldendeVurdering = gjeldendeVurdering,
                            gjeldendeVurderinger = gjeldendeVurderinger,
                            historiskeVurderinger = historiskeVurderinger,
                            økonomiskSosialHjelp = null,
                            nåværendeVirkningsTidspunkt = null,
                        )
                    }
                }
                respond(response)
            }
        }
    }

    route("/api/navenhet") {
        route("/{referanse}/finn") {
            authorizedPost<BehandlingReferanse, List<NavEnheterResponse>, NavEnheterRequest>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req, body ->
                val response = navKontorService.hentNavEnheter()?.filter { enhet ->
                    enhet.navn.contains(
                        body.navn,
                        ignoreCase = true
                    ) || enhet.enhetsNummer.contains(body.navn, ignoreCase = true)
                }
                    ?.map { enhet ->
                        NavEnheterResponse(navn = enhet.navn, enhetsnummer = enhet.enhetsNummer)
                    }.orEmpty()
                respond(response)
            }
        }
    }
}


private fun RefusjonkravVurdering.tilResponse(ansattInfoService: AnsattInfoService): RefusjonkravVurderingResponse {
    val navnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(vurdertAv)
    return RefusjonkravVurderingResponse(
        harKrav = harKrav,
        navKontor = navKontor,
        fom = fom,
        tom = tom,
        vurdertAv =
            VurdertAvResponse(
                ident = vurdertAv,
                dato = opprettetTid?.toLocalDate() ?: error("Fant ikke opprettet tid for refusjonkrav vurdering"),
                ansattnavn = navnOgEnhet?.navn,
                enhetsnavn = navnOgEnhet?.enhet
            )
    )
}