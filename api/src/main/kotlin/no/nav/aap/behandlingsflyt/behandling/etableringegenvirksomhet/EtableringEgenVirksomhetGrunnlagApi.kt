package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.harTilgangOgKanSaksbehandle
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
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.etableringEgenVirksomhetApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    route("/api/behandling/{referanse}/grunnlag/etableringegenvirksomhet") {
        getGrunnlag<BehandlingReferanse, EtableringEgenVirksomhetGrunnlagResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.ETABLERING_EGEN_VIRKSOMHET.kode.toString()
        ) { behandlingReferanse ->
            val response =
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val etableringEgenVirksomhetRepository =
                        repositoryProvider.provide<EtableringEgenVirksomhetRepository>()
                    val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()
                    val bistandRepository = repositoryProvider.provide<BistandRepository>()

                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)
                    val etableringEgenVirksomhetService = EtableringEgenVirksomhetService(repositoryProvider)

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)
                    val sak = sakRepository.hent(behandling.sakId)
                    val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

                    val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandling.id)
                    val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)

                    val etableringEgenVirksomhetGrunnlag =
                        etableringEgenVirksomhetRepository.hentHvisEksisterer(behandling.id)
                    val forrigeGrunnlag =
                        behandling.forrigeBehandlingId?.let { etableringEgenVirksomhetRepository.hentHvisEksisterer(it) }
                            ?: EtableringEgenVirksomhetGrunnlag(emptyList())

                    val ikkeVurderbarePerioder =
                        utledIkkeVurderbarePerioder(sykdomGrunnlag, bistandGrunnlag, sak.rettighetsperiode.fom)

                    EtableringEgenVirksomhetGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = harTilgangOgKanSaksbehandle(
                            harTilgang = kanSaksbehandle(),
                            avklaringsbehovene = avklaringsbehovene
                        ),
                        sisteVedtatteVurderinger = EtableringEgenVirksomhetVurderingResponse.fraDomene(
                            tidslinje = forrigeGrunnlag.gjeldendeVurderingerSomTidslinje(),
                            vurdertAvService = vurdertAvService,
                            etableringEgenVirksomhetService = etableringEgenVirksomhetService

                        ),
                        nyeVurderinger = etableringEgenVirksomhetGrunnlag?.vurderinger.orEmpty()
                            .filter { it.vurdertIBehandling == behandling.id }
                            .map {
                                EtableringEgenVirksomhetVurderingResponse.fraDomene(
                                    it,
                                    vurdertAvService,
                                    etableringEgenVirksomhetService
                                )
                            },
                        kanVurderes = listOf(sak.rettighetsperiode),
                        behøverVurderinger = listOf(),
                        kvalitetssikretAv = vurdertAvService.kvalitetssikretAv(
                            definisjon = Definisjon.ETABLERING_EGEN_VIRKSOMHET,
                            behandlingId = behandling.id
                        ),
                        ikkeVurderbarePerioder = ikkeVurderbarePerioder
                    )
                }
            respond(
                response
            )
        }
    }
}

private fun utledIkkeVurderbarePerioder(
    sykdomGrunnlag: SykdomGrunnlag?,
    bistandGrunnlag: BistandGrunnlag?,
    fom: LocalDate
): List<Periode> {
    val sykdomsvurderinger = sykdomGrunnlag?.somSykdomsvurderingstidslinje().orEmpty()
    val bistandsvurderinger =
        bistandGrunnlag?.somBistandsvurderingstidslinje().orEmpty()

    val zipped = Tidslinje.zip2(sykdomsvurderinger, bistandsvurderinger)

    val førsteDagIOppfyltPeriode = zipped
        .filter {
            it.verdi.first?.erOppfyltOrdinær(fom, it.periode) == true || it.verdi.second?.erBehovForBistand() != true
        }.perioder().toList().first().fom

    val mapped = zipped
        .filter {
            it.verdi.first?.erOppfyltOrdinær(
                fom,
                it.periode
            ) != true || it.verdi.second?.erBehovForArbeidsrettetTiltak != true
        }

    return mapped.perioder().plus(Periode(førsteDagIOppfyltPeriode, førsteDagIOppfyltPeriode)).toList()
}