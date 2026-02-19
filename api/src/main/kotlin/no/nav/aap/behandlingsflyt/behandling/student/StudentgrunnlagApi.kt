package no.nav.aap.behandlingsflyt.behandling.student

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.studentgrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/student") {
            getGrunnlag<BehandlingReferanse, StudentGrunnlagResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_STUDENT.kode.toString()
            ) { req ->
                val response = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val studentRepository = repositoryProvider.provide<StudentRepository>()
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()

                    val behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    val sak = sakRepository.hent(behandling.sakId)


                    val studentGrunnlag: StudentGrunnlag? =
                        studentRepository.hentHvisEksisterer(behandlingId = behandling.id)

                    val nyeVurderinger =
                        studentGrunnlag?.studentvurderingerVurdertIBehandling(behandling.id)
                            .orEmpty()
                            .sortedBy { it.fom }
                            .map { StudentVurderingResponse.fraDomene(it, vurdertAvService) }

                    val sisteVedtatte = StudentVurderingResponse.fraDomene(
                        studentGrunnlag?.vedtattStudenttidslinje(behandling.id).orEmpty(),
                        vurdertAvService
                    )

                    val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
                    val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_SYKDOM)


                    StudentGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        // TODO: Slett
                        studentvurdering = studentGrunnlag?.vurderinger?.maxByOrNull { it.vurdertTidspunkt }
                            ?.let { StudentVurderingResponse.fraDomene(it, vurdertAvService) },
                        oppgittStudent = studentGrunnlag?.oppgittStudent,
                        nyeVurderinger = nyeVurderinger,
                        sisteVedtatteVurderinger = sisteVedtatte,
                        kanVurderes = listOf(sak.rettighetsperiode),
                        behøverVurderinger = avklaringsbehov?.perioderVedtaketBehøverVurdering().orEmpty().toList(),
                        ikkeRelevantePerioder = emptyList(/* Steget bruker ikke periodsert avkalringsbehov, så opplysningene er ikke lett tilgjengelig.*/),
                    )
                }

                respond(response)
            }
        }
    }
}
