package no.nav.aap.behandlingsflyt.behandling.rettighet

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetsperiodeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.rettighetApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
) {
    route("/api/behandling/{referanse}/rettighet") {
        getGrunnlag<BehandlingReferanse, List<RettighetDto>>(
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.AVKLAR_BARNETILLEGG.kode.toString()
        ) { req ->
            val respons: List<RettighetDto> = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                val underveisgrunnlagRepository = repositoryProvider.provide<UnderveisRepository>()
                val underveisgrunnlag = underveisgrunnlagRepository.hent(behandling.id)
                val rettighetstypePerioderMap: Map<RettighetsType, List<Underveisperiode>> =
                    RettighetsType.entries.associate { type -> type to underveisgrunnlag.perioder.filter { it.rettighetsType == type } }

                val rettighetDtoListe = rettighetstypePerioderMap.map { (type, perioder) ->
                    val rettighetKvoter = underveisgrunnlag.utledKvoterForRettighetstype(type)
                    val startdato = underveisgrunnlag.utledStartdatoForRettighet(type)
                    val gjenværendeKvote = rettighetKvoter.gjenværendeKvote

                    val maksDato =
                        when (type) {
                            RettighetsType.BISTANDSBEHOV, RettighetsType.SYKEPENGEERSTATNING
                                -> underveisgrunnlag.utledMaksdatoForRettighet(type)
                            RettighetsType.STUDENT, RettighetsType.ARBEIDSSØKER, RettighetsType.VURDERES_FOR_UFØRETRYGD
                                -> RettighetsperiodeService().utledMaksdatoForRettighet(type, startdato)
                        }

                        RettighetDto(
                            kvote = rettighetKvoter.totalKvote,
                            bruktKvote = rettighetKvoter.bruktKvote,
                            gjenværendeKvote = gjenværendeKvote,
                            startdato = startdato,
                            maksDato = maksDato,
                            opphørsdato = perioder.first { it.avslagsårsak == UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP }.periode.tom,
                            stansdato = perioder.last { it.avslagsårsak == UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT_11_7_STANS }.periode.tom // TODO Erstatt med ikke-deprecated
                        )
                }
                rettighetDtoListe
            }
            respond(respons)
        }
    }
}
