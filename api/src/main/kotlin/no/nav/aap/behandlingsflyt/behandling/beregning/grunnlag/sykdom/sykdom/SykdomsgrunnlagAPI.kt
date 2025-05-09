package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.InnhentetSykdomsOpplysninger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.RegistrertYrkesskade
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.TilgangGatewayImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.sykdomsgrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/sykdom/sykdom") {
            authorizedGet<BehandlingReferanse, SykdomGrunnlagDto>(
                AuthorizationParamPathConfig(behandlingPathParam = BehandlingPathParam("referanse"))
            ) { req ->
                val response = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()
                    val yrkesskadeRepository = repositoryProvider.provide<YrkesskadeRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                    val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandlingId = behandling.id)
                    val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId = behandling.id)

                    val innhentedeYrkesskader = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader.orEmpty()
                        .map { yrkesskade -> RegistrertYrkesskade(yrkesskade, "Yrkesskaderegisteret") }

                    val nåTilstand = sykdomGrunnlag?.sykdomsvurderinger.orEmpty()

                    val historikkSykdomsvurderinger =
                        sykdomRepository.hentHistoriskeSykdomsvurderinger(behandling.sakId, behandling.id)

                    val vedtatteSykdomsvurderinger = behandling.forrigeBehandlingId
                        ?.let { sykdomRepository.hentHvisEksisterer(it) }
                        ?.sykdomsvurderinger.orEmpty()

                    val vedtatteSykdomsvurderingerIder = vedtatteSykdomsvurderinger.map { it.id }
                    val sykdomsvurderinger = nåTilstand.filterNot { it.id in vedtatteSykdomsvurderingerIder }

                    val harTilgangTilÅSaksbehandle = TilgangGatewayImpl.sjekkTilgangTilBehandling(
                        behandling.referanse.referanse,
                        Definisjon.AVKLAR_SYKDOM.kode.toString(),
                        token()
                    )


                    SykdomGrunnlagDto(
                        opplysninger = InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = innhentedeYrkesskader,
                        ),
                        skalVurdereYrkesskade = innhentedeYrkesskader.isNotEmpty(),
                        sykdomsvurdering = sykdomsvurderinger
                            .maxByOrNull { it.opprettet }
                            ?.toDto(),
                        sykdomsvurderinger = sykdomsvurderinger
                            .sortedBy { it.vurderingenGjelderFra ?: LocalDate.MIN }
                            .map { it.toDto() },
                        historikkSykdomsvurderinger = historikkSykdomsvurderinger
                            .sortedBy { it.opprettet }
                            .map { it.toDto() },
                        gjeldendeVedtatteSykdomsvurderinger = vedtatteSykdomsvurderinger
                            .sortedBy { it.vurderingenGjelderFra ?: LocalDate.MIN }
                            .map { it.toDto() },
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle
                    )
                }

                respond(response)
            }
        }
        route("/{referanse}/grunnlag/sykdom/yrkesskade") {
            authorizedGet<BehandlingReferanse, YrkesskadeVurderingGrunnlagDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
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

                    yrkesskadeGrunnlag to sykdomGrunnlag

                    val innhentedeYrkesskader = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader.orEmpty()
                        .map { yrkesskade -> RegistrertYrkesskade(yrkesskade, "Yrkesskaderegisteret") }

                    val harTilgangTilÅSaksbehandle = TilgangGatewayImpl.sjekkTilgangTilBehandling(
                        req.referanse,
                        Definisjon.AVKLAR_YRKESSKADE.kode.toString(),
                        token()
                    )


                    YrkesskadeVurderingGrunnlagDto(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        opplysninger = InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = innhentedeYrkesskader,
                        ),
                        yrkesskadeVurdering = sykdomGrunnlag?.yrkesskadevurdering?.toDto(),
                    )
                }

                respond(response)
            }
        }
    }
}

