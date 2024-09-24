package no.nav.aap.behandlingsflyt.behandling.vilkår.alder.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import javax.sql.DataSource

fun NormalOpenAPIRoute.aldersGrunnlagApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/alder") {
            get<BehandlingReferanse, AlderDTO> { req ->
                val alderDTO = dataSource.transaction(readOnly = true) { connection ->
                    val behandling = BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)
                    val aldersvilkårperioder =
                        VilkårsresultatRepository(connection).hent(behandling.id).finnVilkår(Vilkårtype.ALDERSVILKÅRET)
                            .vilkårsperioder()
                    val fødselsdato =
                        requireNotNull(PersonopplysningRepository(connection).hentHvisEksisterer(behandling.id)?.brukerPersonopplysning?.fødselsdato?.toLocalDate())

                    AlderDTO(fødselsdato, aldersvilkårperioder)
                }

                respond(alderDTO)
            }
        }
    }
}