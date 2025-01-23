package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.RepositoryProvider
import javax.sql.DataSource

fun NormalOpenAPIRoute.beregningVurderingAPI(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/beregning/tidspunkt") {
            get<BehandlingReferanse, BeregningTidspunktAvklaringDto> { req ->
                val responsDto = dataSource.transaction(readOnly = true) {
                    val repositoryProvider = RepositoryProvider(it)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    val skalVurdereUføre = UføreRepository(it).hentHvisEksisterer(behandling.id)?.vurdering != null
                    val beregningGrunnlag =
                        repositoryProvider.provide<BeregningVurderingRepository>()
                            .hentHvisEksisterer(behandlingId = behandling.id)

                    BeregningTidspunktAvklaringDto(
                        vurdering = beregningGrunnlag?.tidspunktVurdering,
                        skalVurdereYtterligere = skalVurdereUføre
                    )
                }

                respond(
                    responsDto
                )
            }
        }
        route("/{referanse}/grunnlag/beregning/yrkesskade") {
            get<BehandlingReferanse, BeregningYrkesskadeAvklaringDto> { req ->
                val responsDto = dataSource.transaction(readOnly = true) {
                    val repositoryProvider = RepositoryProvider(it)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()

                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    val yrkesskadevurdering =
                        sykdomRepository.hentHvisEksisterer(behandling.id)?.yrkesskadevurdering
                    val registerYrkeskade =
                        YrkesskadeRepository(it).hentHvisEksisterer(behandling.id)?.yrkesskader?.yrkesskader
                            ?: emptyList()
                    val beregningGrunnlag =
                        repositoryProvider.provide<BeregningVurderingRepository>()
                            .hentHvisEksisterer(behandlingId = behandling.id)

                    val relevanteSaker = yrkesskadevurdering?.relevanteSaker ?: emptyList()
                    val sakerMedDato =
                        relevanteSaker.map { sak -> registerYrkeskade.singleOrNull { it.ref == sak } }

                    yrkesskadevurdering?.relevanteSaker

                    BeregningYrkesskadeAvklaringDto(
                        skalVurderes = sakerMedDato.filterNotNull().map {
                            YrkesskadeTilVurdering(
                                it.ref, it.skadedato,
                                Grunnbeløp.finnGUnit(it.skadedato, Beløp(1)).beløp
                            )
                        },
                        vurderinger = beregningGrunnlag?.yrkesskadeBeløpVurdering?.vurderinger ?: emptyList()
                    )
                }

                respond(
                    responsDto
                )
            }
        }
    }
}