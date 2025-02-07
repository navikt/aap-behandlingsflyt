package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.InnhentetSykdomsOpplysninger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.RegistrertYrkesskade
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.sykdomsgrunnlagApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/sykdom/sykdom") {
            authorizedGet<BehandlingReferanse, SykdomGrunnlagDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            ) { req ->
                val (yrkesskadeGrunnlag, sykdomGrunnlag) = dataSource.transaction(
                    readOnly = true,
                    block = hentUtYrkesskadOgSykdomsgrunnlag(
                        req
                    )
                )

                val innhentedeYrkesskader = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.map { yrkesskade ->
                    RegistrertYrkesskade(
                        ref = yrkesskade.ref,
                        skadedato = yrkesskade.skadedato,
                        kilde = "Yrkesskaderegisteret"
                    )
                } ?: emptyList()

                val sykdomsvurdering = sykdomGrunnlag?.sykdomsvurdering?.toDto()
                respond(
                    SykdomGrunnlagDto(
                        opplysninger = InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = innhentedeYrkesskader,
                        ),
                        sykdomsvurdering = sykdomsvurdering,
                        skalVurdereYrkesskade = innhentedeYrkesskader.isNotEmpty(),
                        sykdomsvurderinger = listOfNotNull(sykdomsvurdering),
                        historikkSykdomsvurderinger = listOf(),
                        gjeldendeVedtatteSykdomsvurderinger = listOf(),
                    )
                )
            }
        }
        route("/{referanse}/grunnlag/sykdom/yrkesskade") {
            authorizedGet<BehandlingReferanse, YrkesskadeVurderingGrunnlagDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val (yrkesskadeGrunnlag, sykdomGrunnlag) = dataSource.transaction(
                    readOnly = true,
                    block = hentUtYrkesskadOgSykdomsgrunnlag(req)
                )

                val innhentedeYrkesskader = yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.map { yrkesskade ->
                    RegistrertYrkesskade(
                        ref = yrkesskade.ref,
                        skadedato = yrkesskade.skadedato,
                        kilde = "Yrkesskaderegisteret"
                    )
                } ?: emptyList()
                respond(
                    YrkesskadeVurderingGrunnlagDto(
                        opplysninger = InnhentetSykdomsOpplysninger(
                            oppgittYrkesskadeISøknad = false,
                            innhentedeYrkesskader = innhentedeYrkesskader,
                        ),
                        yrkesskadeVurdering = sykdomGrunnlag?.yrkesskadevurdering?.toDto(),
                    )
                )
            }
        }
    }
}

private fun hentUtYrkesskadOgSykdomsgrunnlag(req: BehandlingReferanse) = { connection: DBConnection ->
    val repositoryProvider = RepositoryProvider(connection)
    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()
    val yrkesskadeRepository = repositoryProvider.provide<YrkesskadeRepository>()

    val behandling: Behandling =
        BehandlingReferanseService(behandlingRepository).behandling(req)

    val yrkesskadeGrunnlag =
        yrkesskadeRepository.hentHvisEksisterer(behandlingId = behandling.id)
    val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId = behandling.id)

    yrkesskadeGrunnlag to sykdomGrunnlag
}