package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.alder

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import javax.sql.DataSource

fun NormalOpenAPIRoute.aldersGrunnlagApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/alder") {
            get<BehandlingReferanse, AlderDTO> { req ->
                val alderDTO = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val personopplysningRepository =
                        repositoryProvider.provide<PersonopplysningRepository>()
                    val vilkårsresultatRepository =
                        repositoryProvider.provide<VilkårsresultatRepository>()
                    val behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    val aldersvilkårperioder =
                        vilkårsresultatRepository.hent(behandling.id)
                            .finnVilkår(Vilkårtype.ALDERSVILKÅRET)
                            .vilkårsperioder()
                    val fødselsdato =
                        requireNotNull(
                            personopplysningRepository.hentHvisEksisterer(behandling.id)?.brukerPersonopplysning?.fødselsdato?.toLocalDate()
                        )

                    AlderDTO(fødselsdato, aldersvilkårperioder)
                }

                respond(alderDTO)
            }
        }
    }
}