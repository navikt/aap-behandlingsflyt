package no.nav.aap.behandlingsflyt.behandling.foreslåvedtak

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.utils.tilForeslåVedtakDataTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FORESLÅ_VEDTAK_KODE
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.foreslaaVedtakAPI(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/foreslaa-vedtak").getGrunnlag<BehandlingReferanse, ForeslåVedtakResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = FORESLÅ_VEDTAK_KODE
        ) { behandlingReferanse ->
            val response =
                dataSource.transaction(readOnly = true) { conn ->
                    val repositoryProvider = repositoryRegistry.provider(conn)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)
                    val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
                    val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling.id)
                    val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
                    val vilkårsresultat = vilkårsresultatRepository.hent(behandling.id)

                    val avslagstidslinjer = utledAvslagstidslinjer(vilkårsresultat)
                    // Hvis avslag tidlig i behandlingen finnes ikke underveisgrunnlag
                    if (underveisGrunnlag == null) {
                        ForeslåVedtakResponse(emptyList())
                    } else {
                        val foreslåVedtakPerioder =
                            underveisGrunnlag
                                .tilForeslåVedtakDataTidslinje()
                                .segmenter()
                                .map {
                                    val avslagsårsaker =
                                        avslagstidslinjer
                                            .flatMap { tidslinje -> tidslinje.begrensetTil(it.periode).segmenter() }
                                            .filter { it.verdi.second == Utfall.IKKE_OPPFYLT }.map { it.verdi.first }
                                    ForeslåVedtakDto(
                                        periode = it.periode,
                                        utfall = it.verdi.utfall,
                                        rettighetsType = it.verdi.rettighetsType,
                                        avslagsårsak =
                                            AvslagsårsakDto(
                                                vilkårsavslag = avslagsårsaker,
                                                underveisavslag = it.verdi.underveisÅrsak
                                            )
                                    )
                                }
                        ForeslåVedtakResponse(
                            foreslåVedtakPerioder
                        )
                    }
                }
            respond(response)
        }
    }
}

private fun utledAvslagstidslinjer(vilkårsresultat: Vilkårsresultat): List<Tidslinje<Pair<String, Utfall>>> {
    val allevilkårmedavslag = vilkårsresultat.alle().filter { it.harPerioderSomIkkeErOppfylt() }
    return allevilkårmedavslag.map { vilkår ->
        vilkår.vilkårsperioder().map { Segment(it.periode, Pair(vilkår.type.hjemmel, it.utfall)) }.let { Tidslinje(it) }
    }
}

