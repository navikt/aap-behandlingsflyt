package no.nav.aap.behandlingsflyt.behandling.barnetillegg.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.barnetillegg.BarnetilleggService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.verdityper.sakogbehandling.Ident
import javax.sql.DataSource

fun NormalOpenAPIRoute.barnetilleggApi(dataSource: DataSource) {
    route("/api/barnetillegg") {
        route("/grunnlag/{referanse}") {
            get<BehandlingReferanse, BarnetilleggDto> { req ->
                val dto = dataSource.transaction { connection ->
                    val behandling: Behandling =
                        BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)
                    val personopplysningRepository = PersonopplysningRepository(connection)
                    val barnRepository = BarnRepository(connection)
                    val barnetilleggService = BarnetilleggService(
                        BarnVurderingRepository(connection),
                        SakOgBehandlingService(connection),
                        barnRepository,
                        personopplysningRepository
                    )
                    val barnetilleggTidslinje = barnetilleggService.beregn(behandling.id)

                    val folkeregister = mutableSetOf<Ident>()
                    val uavklarteBarn = mutableSetOf<Ident>()
                    barnetilleggTidslinje.segmenter().forEach { segment ->
                        uavklarteBarn.addAll(segment.verdi.barnTilAvklaring())
                        folkeregister.addAll(segment.verdi.registerBarn())
                    }

                    val barnGrunnlag = barnRepository.hentHvisEksisterer(behandling.id)
                    val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(behandling.id)

                    BarnetilleggDto(
                        oppgitteBarn = barnGrunnlag?.oppgittBarn?.identer?.toList() ?: emptyList(),
                        folkeregisterbarn = uavklarteBarn.map { hentBarn(it, personopplysningGrunnlag!!) },
                        barnSomTrengerVurdering = uavklarteBarn.toList()
                    )
                }

                respond(dto)
            }
        }
    }
}

fun hentBarn(ident: Ident, personopplysningGrunnlag: PersonopplysningGrunnlag): IdentifiserteBarnDto {
    val personopplysning =
        requireNotNull(personopplysningGrunnlag.relatertePersonopplysninger?.personopplysninger?.single { relatertPersonopplysning -> relatertPersonopplysning.gjelderForIdent(ident) })

    return IdentifiserteBarnDto(
        ident,
        Barn(ident, personopplysning.fødselsdato, personopplysning.dødsdato).periodeMedRettTil()
    )
}
