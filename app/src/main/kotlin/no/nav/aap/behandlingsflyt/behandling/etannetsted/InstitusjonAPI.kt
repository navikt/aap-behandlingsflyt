package no.nav.aap.behandlingsflyt.behandling.etannetsted

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.InstitusjonsoppholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.SoningsGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.Soningsforhold
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Tidslinje

fun NormalOpenAPIRoute.institusjonAPI(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/institusjon/soning") {
            get<BehandlingReferanse, SoningsGrunnlag> { req ->
                val soningsgrunnlag = dataSource.transaction { connection ->
                    val behandling = BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)
                    val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)
                    val utlederService =
                        EtAnnetStedUtlederService(BarnetilleggRepository(connection), institusjonsoppholdRepository)
                    val behov = utlederService.utled(behandling.id)

                    // Hent ut rå fakta fra grunnlaget
                    val soningsopphold = institusjonsoppholdRepository.hentHvisEksisterer(behandling.id)
                    val soningsforholdInfo =
                        byggTidslinjeAvType(soningsopphold, Institusjonstype.FO)

                    val perioderMedSoning = behov.perioderTilVurdering.mapValue { it.soning }.komprimer()

                    val manglendePerioder = perioderMedSoning.segmenter()
                        .filterNot { it.verdi == null }
                        .map {
                            Soningsforhold(
                                vurderingsdato = it.periode.fom,
                                info = finnInfoOmOpphold(it.periode, soningsforholdInfo),
                                vurdering = null, // TODO: hente ut vurdering for perioden
                                status = it.verdi!!.vurdering
                            )
                        }

                    SoningsGrunnlag(manglendePerioder)
                }
                respond(soningsgrunnlag)
            }
        }
    }
    route("/api/behandling") {
        route("/{referanse}/grunnlag/institusjon/helse") {
            get<BehandlingReferanse, HelseinstitusjonGrunnlag> { req ->
                val soningsgrunnlag = dataSource.transaction { connection ->
                    HelseinstitusjonGrunnlag(false)
                }
                respond(soningsgrunnlag)
            }
        }
    }
}

private fun byggTidslinjeAvType(
    soningsopphold: InstitusjonsoppholdGrunnlag?, institusjonstype: Institusjonstype
): Tidslinje<Institusjon> {
    return Tidslinje(soningsopphold?.opphold?.filter { it.verdi.type == institusjonstype }
        ?: emptyList())
}

private fun finnInfoOmOpphold(
    periode: Periode,
    tidslinje: Tidslinje<no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon>
): InstitusjonsoppholdDto {
    return InstitusjonsoppholdDto.institusjonToDto(tidslinje.kryss(periode).segmenter().single())
}
