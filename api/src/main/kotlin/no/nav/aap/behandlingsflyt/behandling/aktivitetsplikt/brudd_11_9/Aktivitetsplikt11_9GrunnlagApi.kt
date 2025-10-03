package no.nav.aap.behandlingsflyt.behandling.aktivitetsplikt.brudd_11_9

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Grunn
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_BRUDD_11_9_KODE
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.collections.orEmpty

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

                val vedtatteVurderinger = behandling.forrigeBehandlingId
                    ?.let { aktivitetsplikt11_9Repository.hentHvisEksisterer(it) }
                    ?.vurderinger.orEmpty()
                val vurderinger = nåTilstand.filter { it.vurdertIBehandling == behandling.id  }

                Aktivitetsplikt11_9GrunnlagDto(
                    harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                    vurderinger = vurderinger.map { it.tilDto(ansattInfoService) }.toSet(),
                    vedtatteVurderinger = vedtatteVurderinger.map { it.tilDto(ansattInfoService) }.toSet(),
                )
            }
            respond(respons)
        }
    }
}

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
