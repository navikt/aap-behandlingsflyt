package no.nav.aap.behandlingsflyt.behandling.vilkår.alder.flate

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
import no.nav.aap.repository.RepositoryFactory
import javax.sql.DataSource

fun NormalOpenAPIRoute.aldersGrunnlagApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/alder") {
            get<BehandlingReferanse, AlderDTO> { req ->
                val alderDTO = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryFactory = RepositoryFactory(connection)
                    val behandlingRepository = repositoryFactory.create(BehandlingRepository::class)
                    val personopplysningRepository = repositoryFactory.create(PersonopplysningRepository::class)
                    val vilkårsresultatRepository = repositoryFactory.create(VilkårsresultatRepository::class)
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    val aldersvilkårperioder =
                        vilkårsresultatRepository.hent(behandling.id).finnVilkår(Vilkårtype.ALDERSVILKÅRET)
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