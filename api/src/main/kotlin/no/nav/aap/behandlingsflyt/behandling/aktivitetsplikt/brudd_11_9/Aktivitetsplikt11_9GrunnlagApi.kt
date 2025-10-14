package no.nav.aap.behandlingsflyt.behandling.aktivitetsplikt.brudd_11_9

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingGateway
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Grunn
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_BRUDD_11_9_KODE
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.HentSakDTO
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.getGrunnlag
import no.nav.aap.utbetal.trekk.TrekkResponsDto
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.aktivitetsplikt11_9GrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    route("api/aktivitetsplikt/{referanse}/grunnlag/brudd-11-9") {
        getGrunnlag<BehandlingReferanse, Aktivitetsplikt11_9GrunnlagDto>(
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = VURDER_BRUDD_11_9_KODE
        ) { behandlingsreferanse ->
            val respons = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val aktivitetsplikt11_9Repository = repositoryProvider.provide<Aktivitetsplikt11_9Repository>()

                val behandling: Behandling =
                    BehandlingReferanseService(behandlingRepository).behandling(behandlingsreferanse)

                val grunnlag = aktivitetsplikt11_9Repository.hentHvisEksisterer(behandling.id)

                if (grunnlag == null) {
                    Aktivitetsplikt11_9GrunnlagDto(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                    )
                }

                val nåTilstand = grunnlag?.gjeldendeVurderinger().orEmpty()
                val vurderinger = nåTilstand.filter { it.vurdertIBehandling == behandling.id }

                val vedtatteVurderinger = behandling.forrigeBehandlingId
                    ?.let { aktivitetsplikt11_9Repository.hentHvisEksisterer(it) }
                    ?.gjeldendeVurderinger().orEmpty()

                Aktivitetsplikt11_9GrunnlagDto(
                    harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                    vurderinger = vurderinger.map { it.tilDto(ansattInfoService) }.toSet(),
                    vedtatteVurderinger = vedtatteVurderinger.map { it.tilDto(ansattInfoService) }.toSet(),
                )
            }
            respond(respons)
        }
    }
    route("api/aktivitetsplikt/trekk/{saksnummer}") {
        authorizedGet<HentSakDTO, AktivitetspliktMedTrekkDto>(
            AuthorizationParamPathConfig(
                sakPathParam = SakPathParam("saksnummer")
            ),
            null,
            TagModule(listOf(Tags.Sak)),
        ) { req ->
            val aktivitetspliktVurderingerMedTrekk = dataSource.transaction(readOnly = true) { connection ->
                val utbetalingGateway = gatewayProvider.provide<UtbetalingGateway>()
                val repositoryProvider = repositoryRegistry.provider(connection)
                val aktivitetsplikt11_9Repository = repositoryProvider.provide<Aktivitetsplikt11_9Repository>()
                val sak = repositoryProvider.provide<SakRepository>().hent(saksnummer = Saksnummer(req.saksnummer))
                val sakOgBehandlingService = SakOgBehandlingService(
                    repositoryProvider = repositoryProvider,
                    gatewayProvider = gatewayProvider
                )
                val sisteFattedeVedtaksBehandling = sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sak.id)

                sisteFattedeVedtaksBehandling?.let {
                    val aktivitetspliktGrunnlag = aktivitetsplikt11_9Repository.hentHvisEksisterer(it.id)
                    aktivitetspliktGrunnlag?.let {
                        val trekk = utbetalingGateway.hentTrekk(req.saksnummer)
                        aktivitetspliktGrunnlag.tilDtoMedTrekk(trekk, ansattInfoService)
                    }
                }
            }

            respond(AktivitetspliktMedTrekkDto(aktivitetspliktVurderingerMedTrekk ?: emptyList()))
        }
    }
}

internal fun Aktivitetsplikt11_9Grunnlag.tilDtoMedTrekk(
    trekk: TrekkResponsDto,
    ansattInfoService: AnsattInfoService
): List<AktivitetspliktVurderingMedTrekkDto> {
    return gjeldendeVurderinger().map { aktivitetspliktVurdering ->
        val aktueltTrekk = trekk.trekkListe.find { it.dato == aktivitetspliktVurdering.dato }
        AktivitetspliktVurderingMedTrekkDto(
            dato = aktivitetspliktVurdering.dato,
            begrunnelse = aktivitetspliktVurdering.begrunnelse,
            brudd = aktivitetspliktVurdering.brudd,
            grunn = aktivitetspliktVurdering.grunn,
            vurdertAv = VurdertAvResponse.fraIdent(
                aktivitetspliktVurdering.vurdertAv,
                aktivitetspliktVurdering.opprettet,
                ansattInfoService
            ),
            registrertTrekk = aktueltTrekk?.let {
                AktivitetspliktTrekkDto(
                    beløp = aktueltTrekk.beløp,
                    posteringer = aktueltTrekk.posteringer.map {
                        AktivitetspliktTrekkPosteringDto(
                            dato = it.dato,
                            beløp = it.beløp
                        )
                    }
                )
            }
        )
    }
}

data class AktivitetspliktMedTrekkDto(
    val vurderingerMedTrekk: List<AktivitetspliktVurderingMedTrekkDto>
)

data class AktivitetspliktVurderingMedTrekkDto(
    val dato: LocalDate,
    val begrunnelse: String,
    val brudd: Brudd,
    val grunn: Grunn,
    val vurdertAv: VurdertAvResponse?,
    val registrertTrekk: AktivitetspliktTrekkDto?,
)

data class AktivitetspliktTrekkDto(
    val beløp: Int,
    val posteringer: List<AktivitetspliktTrekkPosteringDto>
)

data class AktivitetspliktTrekkPosteringDto(
    val dato: LocalDate,
    val beløp: Int,
)

data class Aktivitetsplikt11_9GrunnlagDto(
    val vurderinger: Set<Aktivitetsplikt11_9VurderingDto> = emptySet(),
    val vedtatteVurderinger: Set<Aktivitetsplikt11_9VurderingDto> = emptySet(),
    val harTilgangTilÅSaksbehandle: Boolean,
)

data class Aktivitetsplikt11_9VurderingDto(
    val dato: LocalDate,
    val begrunnelse: String,
    val brudd: Brudd,
    val grunn: Grunn,
    val vurdertAv: VurdertAvResponse?,
)

internal fun Aktivitetsplikt11_9Vurdering.tilDto(ansattInfoService: AnsattInfoService): Aktivitetsplikt11_9VurderingDto {
    return Aktivitetsplikt11_9VurderingDto(
        dato = dato,
        begrunnelse = begrunnelse,
        brudd = brudd,
        grunn = grunn,
        vurdertAv = VurdertAvResponse.fraIdent(vurdertAv, opprettet, ansattInfoService),
    )
}
