package no.nav.aap.behandlingsflyt.behandling.arbeidsopptrapping

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
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
import kotlin.collections.map
import kotlin.collections.orEmpty

fun NormalOpenAPIRoute.arbeidsopptrappingGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling/{referanse}/grunnlag/arbeidsopptrapping") {
        getGrunnlag<BehandlingReferanse, ArbeidsopptrappingGrunnlagResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.ARBEIDSOPPTRAPPING.kode.toString()
        ) { behandlingReferanse ->
            val response =
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)
                    val sak = sakRepository.hent(behandling.sakId)
                    val arbeidsopptrappingRepository = repositoryProvider.provide<ArbeidsopptrappingRepository>()
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)

                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()
                    val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandling.id)
                    val bistandRepository = repositoryProvider.provide<BistandRepository>()
                    val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
                    val ikkeVurderbarePerioder =
                        utledIkkeVurderbarePerioder(sykdomGrunnlag, bistandGrunnlag, sak.rettighetsperiode.fom)

                    val arbeidsopptrappingGrunnlag = arbeidsopptrappingRepository.hentHvisEksisterer(behandling.id)

                    val forrigeGrunnlag =
                        behandling.forrigeBehandlingId?.let { arbeidsopptrappingRepository.hentHvisEksisterer(it) }
                            ?: ArbeidsopptrappingGrunnlag(emptyList())

                    ArbeidsopptrappingGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        sisteVedtatteVurderinger = ArbeidsopptrappingVurderingResponse.fraDomene(
                            forrigeGrunnlag.gjeldendeVurderinger(),
                            vurdertAvService
                        ),
                        nyeVurderinger = arbeidsopptrappingGrunnlag?.vurderinger.orEmpty()
                            .filter { it.vurdertIBehandling == behandling.id }
                            .map { ArbeidsopptrappingVurderingResponse.fraDomene(it, vurdertAvService) },
                        kanVurderes = listOf(sak.rettighetsperiode),
                        behøverVurderinger = listOf(),
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

    val mapped = Tidslinje.zip2(sykdomsvurderinger, bistandsvurderinger)
        .filter {
            it.verdi.first?.erOppfyltOrdinær(fom, it.periode) != true || it.verdi.second?.erBehovForBistand() != true
        }
    return mapped.perioder().toList()
}