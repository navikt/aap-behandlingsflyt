package no.nav.aap.behandlingsflyt.behandling.aktivitetsplikt.brudd_11_7

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.brev.ForhåndsvarselBruddAktivitetsplikt
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Varsel
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.VURDER_BRUDD_11_7_KODE
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

fun NormalOpenAPIRoute.aktivitetsplikt11_7GrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    route("api/aktivitetsplikt/{referanse}/grunnlag/brudd-11-7") {
        getGrunnlag<BehandlingReferanse, Aktivitetsplikt11_7GrunnlagDto>(
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = VURDER_BRUDD_11_7_KODE
        ) { req ->
            val respons = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val aktivitetsplikt11_7Repository = repositoryProvider.provide<Aktivitetsplikt11_7Repository>()
                val brevbestillingService = BrevbestillingService(repositoryProvider, gatewayProvider)

                val behandling: Behandling =
                    BehandlingReferanseService(behandlingRepository).behandling(req)

                val grunnlag = aktivitetsplikt11_7Repository.hentHvisEksisterer(behandling.id)

                if (grunnlag == null) {
                    Aktivitetsplikt11_7GrunnlagDto(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        harSendtForhåndsvarsel = false,
                        varsel = null
                    )
                }

                val nåTilstand = grunnlag?.vurderinger.orEmpty()

                val vedtatteVurderinger = behandling.forrigeBehandlingId
                    ?.let { aktivitetsplikt11_7Repository.hentHvisEksisterer(it) }
                    ?.vurderinger.orEmpty()
                val vurdering = nåTilstand
                    .filterNot { it in vedtatteVurderinger }
                    .singleOrNull()

                val varsel = aktivitetsplikt11_7Repository.hentVarselHvisEksisterer(behandling.id)

                Aktivitetsplikt11_7GrunnlagDto(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        vurdering = vurdering?.tilDto(ansattInfoService),
                        vedtatteVurderinger = vedtatteVurderinger.map{it.tilDto(ansattInfoService)},
                        harSendtForhåndsvarsel = harSendtForhåndsvarselForBehandlingen(brevbestillingService, behandling),
                        varsel = varsel?.tilDto()
                    )
            }
            respond(respons)
        }
    }
}

private fun harSendtForhåndsvarselForBehandlingen(
    brevbestillingService: BrevbestillingService,
    behandling: Behandling
): Boolean {
    return brevbestillingService.hentBestillinger(behandling.id, ForhåndsvarselBruddAktivitetsplikt.typeBrev)
        .maxByOrNull { it.opprettet }
        ?.status == Status.FULLFØRT
}

data class Aktivitetsplikt11_7GrunnlagDto(
    val vurdering: Aktivitetsplikt11_7VurderingDto? = null,
    val vedtatteVurderinger: List<Aktivitetsplikt11_7VurderingDto> = emptyList(),
    val harTilgangTilÅSaksbehandle: Boolean,
    val harSendtForhåndsvarsel: Boolean,
    val varsel: Aktivitetsplikt11_7VarselDto?
)

data class Aktivitetsplikt11_7VurderingDto(
    val begrunnelse: String,
    val erOppfylt: Boolean,
    val utfall: Utfall?,
    val gjelderFra: LocalDate,
    val vurdertAv: VurdertAvResponse?,
    val skalIgnorereVarselFrist: Boolean
)

data class Aktivitetsplikt11_7VarselDto(
    val sendtDato: LocalDate? = null,
    val svarfrist: LocalDate? = null,
)

internal fun Aktivitetsplikt11_7Vurdering.tilDto(ansattInfoService: AnsattInfoService): Aktivitetsplikt11_7VurderingDto {
    return Aktivitetsplikt11_7VurderingDto(
        begrunnelse = begrunnelse,
        erOppfylt = erOppfylt,
        utfall = utfall,
        gjelderFra = gjelderFra,
        vurdertAv = VurdertAvResponse.fraIdent(vurdertAv, opprettet, ansattInfoService),
        skalIgnorereVarselFrist = this.skalIgnorereVarselFrist
    )
}

internal fun Aktivitetsplikt11_7Varsel.tilDto(): Aktivitetsplikt11_7VarselDto {
    return Aktivitetsplikt11_7VarselDto(
        sendtDato = sendtDato,
        svarfrist = svarfrist,
    )
}