package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.InnhentetSykdomsOpplysninger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.RegistrertYrkesskade
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.sykdomsgrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    route("/api/behandling") {
        route("/{referanse}/grunnlag/sykdom/sykdom") {
            getGrunnlag<BehandlingReferanse, SykdomGrunnlagResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_SYKDOM.kode.toString()
            ) { req ->
                val response = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()
                    val yrkesskadeRepository = repositoryProvider.provide<YrkesskadeRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()

                    val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandlingId = behandling.id)
                    val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId = behandling.id)

                    val sistVedtatteSykdomGrunnlag =
                        behandling.forrigeBehandlingId?.let { sykdomRepository.hentHvisEksisterer(behandlingId = it) }

                    val innhentedeYrkesskader = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader.orEmpty()
                        .map { yrkesskade -> RegistrertYrkesskade(yrkesskade) }

                    val nyeVurderinger = sykdomGrunnlag
                        ?.sykdomsvurderingerVurdertIBehandling(behandlingId = behandling.id).orEmpty()
                        .sortedBy { it.vurderingenGjelderFra }
                        .map { SykdomsvurderingResponse.fraDomene(it, vurdertAvService) }

                    val sisteVedtatte = SykdomsvurderingResponse.fraDomene(
                        sykdomGrunnlag
                            ?.vedtattSykdomstidslinje(behandling.id).orEmpty(), vurdertAvService
                    )

                    val sak = sakRepository.hent(behandling.sakId)

                    val avklaringsbehov = avklaringsbehovRepository
                        .hentAvklaringsbehovene(behandling.id)
                        .hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)

                    SykdomGrunnlagResponse(
                        opplysninger = InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = innhentedeYrkesskader,
                        ),
                        skalVurdereYrkesskade = innhentedeYrkesskader.isNotEmpty(),
                        erÅrsakssammenhengYrkesskade = sistVedtatteSykdomGrunnlag?.yrkesskadevurdering?.erÅrsakssammenheng
                            ?: false,
                        sykdomsvurderinger = nyeVurderinger, // TODO: Fjern
                        nyeVurderinger = nyeVurderinger,
                        historikkSykdomsvurderinger = sykdomGrunnlag
                            ?.historiskeSykdomsvurderinger(behandling.id).orEmpty()
                            .sortedBy { it.opprettet }
                            .map { SykdomsvurderingResponse.fraDomene(it, vurdertAvService) },
                        gjeldendeVedtatteSykdomsvurderinger = sisteVedtatte, // TODO: Fjern
                        sisteVedtatteVurderinger = sisteVedtatte,
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        kvalitetssikretAv = vurdertAvService.kvalitetssikretAv(
                            definisjon = Definisjon.AVKLAR_SYKDOM,
                            behandlingId = behandling.id,
                        ),
                        kanVurderes = listOf(sak.rettighetsperiode),
                        behøverVurderinger = avklaringsbehov?.perioderVedtaketBehøverVurdering().orEmpty().toList(),
                        perioderSomIkkeErTilstrekkeligVurdert = avklaringsbehov?.perioderSomIkkeErTilstrekkeligVurdert()
                            .orEmpty().toList()
                    )
                }

                respond(response)
            }
        }
        route("/{referanse}/grunnlag/sykdom/yrkesskade") {
            getGrunnlag<BehandlingReferanse, YrkesskadeVurderingGrunnlagResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_YRKESSKADE.kode.toString()
            ) { req ->
                val response = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()
                    val yrkesskadeRepository = repositoryProvider.provide<YrkesskadeRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)

                    val yrkesskadeGrunnlag =
                        yrkesskadeRepository.hentHvisEksisterer(behandlingId = behandling.id)
                    val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId = behandling.id)

                    val innhentedeYrkesskader = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader.orEmpty()
                        .map { yrkesskade -> RegistrertYrkesskade(yrkesskade) }

                    YrkesskadeVurderingGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        opplysninger = InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = innhentedeYrkesskader,
                        ),
                        yrkesskadeVurdering = sykdomGrunnlag?.yrkesskadevurdering?.toResponse(ansattInfoService),
                    )
                }

                respond(response)
            }
        }
    }
}

private fun Yrkesskadevurdering.toResponse(ansattInfoService: AnsattInfoService): YrkesskadevurderingResponse {
    val navnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(vurdertAv)
    return YrkesskadevurderingResponse(
        begrunnelse = begrunnelse,
        relevanteSaker = relevanteSaker.map { it.referanse },
        relevanteYrkesskadeSaker = relevanteSaker.map { YrkesskadeSakResponse(it.referanse, it.manuellYrkesskadeDato) },
        andelAvNedsettelsen = andelAvNedsettelsen?.prosentverdi(),
        erÅrsakssammenheng = erÅrsakssammenheng,
        vurdertAv = VurdertAvResponse(
            ident = vurdertAv,
            dato = requireNotNull(vurdertTidspunkt?.toLocalDate()) { "Fant ikke vurderingstidspunkt for yrkesskadevurdering" },
            ansattnavn = navnOgEnhet?.navn,
            enhetsnavn = navnOgEnhet?.enhet,
        )
    )
}

