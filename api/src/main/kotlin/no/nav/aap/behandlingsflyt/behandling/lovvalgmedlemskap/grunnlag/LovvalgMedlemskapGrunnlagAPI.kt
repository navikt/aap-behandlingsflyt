package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import javax.sql.DataSource

fun NormalOpenAPIRoute.lovvalgMedlemskapGrunnlagAPI(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/lovvalgmedlemskap") {
            get<BehandlingReferanse, LovvalgMedlemskapGrunnlagDto> { req ->
                val grunnlag = dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    val vilkårsPeriode = vilkårsresultatRepository.hent(behandling.id)
                        .finnVilkår(Vilkårtype.MEDLEMSKAP)
                        .vilkårsperioder()

                    LovvalgMedlemskapGrunnlagDto(vilkårsPeriode)
                }
                respond(grunnlag)
            }
        }
    }
}