package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.alder

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.aldersGrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/alder") {
            authorizedGet<BehandlingReferanse, AlderDTO>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            ) { req ->
                val alderDTO = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val personopplysningRepository =
                        repositoryProvider.provide<PersonopplysningRepository>()
                    val vilkårsresultatRepository =
                        repositoryProvider.provide<VilkårsresultatRepository>()
                    val behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    val aldersvilkår =
                        vilkårsresultatRepository.hent(behandling.id)
                            .finnVilkår(Vilkårtype.ALDERSVILKÅRET)

                    val fødselsdato =
                        requireNotNull(
                            personopplysningRepository.hentBrukerPersonOpplysningHvisEksisterer(behandling.id)?.fødselsdato?.toLocalDate()
                        )

                    AlderDTO(
                        fødselsdato = fødselsdato,
                        vilkårsperioder = aldersvilkår.vilkårsperioder(),
                        vurdertDato = aldersvilkår.vurdertTidspunkt?.toLocalDate()
                    )
                }

                respond(alderDTO)
            }
        }
    }
}