package no.nav.aap.behandlingsflyt.behandling.student

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.studentgrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)
    route("/api/behandling") {
        route("/{referanse}/grunnlag/student") {
            getGrunnlag<BehandlingReferanse, StudentGrunnlagResponse>(
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_STUDENT.kode.toString()
            ) { req ->
                val studentGrunnlag: StudentGrunnlag? =
                    dataSource.transaction(readOnly = true) { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                        val studentRepository = repositoryProvider.provide<StudentRepository>()
                        val behandling =
                            BehandlingReferanseService(behandlingRepository).behandling(req)

                        studentRepository.hentHvisEksisterer(behandlingId = behandling.id)
                    }

                if (studentGrunnlag != null) {
                    respond(
                        StudentGrunnlagResponse(
                            harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                            studentvurdering = studentGrunnlag.studentvurdering?.tilResponse(ansattInfoService),
                            oppgittStudent = studentGrunnlag.oppgittStudent
                        )
                    )
                } else {
                    respond(NoneStudentGrunnlagResponse())
                }
            }
        }
    }
}

private fun StudentVurdering.tilResponse(ansattInfoService: AnsattInfoService): StudentVurderingResponse {
    val navnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(this.vurdertAv)
    return StudentVurderingResponse(
        id = this.id,
        begrunnelse = this.begrunnelse,
        harAvbruttStudie = this.harAvbruttStudie,
        godkjentStudieAvLånekassen = this.godkjentStudieAvLånekassen,
        avbruttPgaSykdomEllerSkade = this.avbruttPgaSykdomEllerSkade,
        harBehovForBehandling = this.harBehovForBehandling,
        avbruttStudieDato = this.avbruttStudieDato,
        avbruddMerEnn6Måneder = this.avbruddMerEnn6Måneder,
        vurdertAv = VurdertAvResponse(
            ident = this.vurdertAv,
            dato = requireNotNull(this.vurdertTidspunkt?.toLocalDate()) {
                "Fant ikke vurdert tidspunkt for studentvurdering"
            },
            ansattnavn = navnOgEnhet?.navn,
            enhetsnavn = navnOgEnhet?.enhet,
        )
    )
}