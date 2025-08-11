package no.nav.aap.behandlingsflyt.behandling.rettighetsperiode

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.søknad.DatoFraDokumentUtleder
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.time.LocalDate
import javax.sql.DataSource


data class RettighetsperiodeGrunnlagResponse(
    val vurdering: RettighetsperiodeVurderingResponse?,
    val søknadsdato: LocalDate?,
    val harTilgangTilÅSaksbehandle: Boolean
)

data class RettighetsperiodeVurderingResponse(
    val begrunnelse: String,
    val harRettUtoverSøknadsdato: Boolean,
    val startDato: LocalDate?,
    val harKravPåRenter: Boolean?,
    val vurdertAv: VurdertAvResponse
)


fun NormalOpenAPIRoute.rettighetsperiodeGrunnlagAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling/{referanse}/grunnlag/rettighetsperiode")
        .getGrunnlag<BehandlingReferanse, RettighetsperiodeGrunnlagResponse>(
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.VURDER_RETTIGHETSPERIODE.kode.toString()

        ) { req ->
            val rettighetsperiodeGrunnlagDto = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val rettighetsperiodeRepository = repositoryProvider.provide<VurderRettighetsperiodeRepository>()
                val mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>()
                val datoFraDokumentUtleder = DatoFraDokumentUtleder(mottattDokumentRepository)

                val behandling = behandlingRepository.hent(BehandlingReferanse(req.referanse))
                val vurdering = rettighetsperiodeRepository.hentVurdering(behandling.id)

                val ansattNavnOgEnhet = vurdering?.let { AnsattInfoService(GatewayProvider).hentAnsattNavnOgEnhet(it.vurdertAv) }

                RettighetsperiodeGrunnlagResponse(
                    vurdering =
                        rettighetsperiodeRepository.hentVurdering(behandling.id)?.let {
                            RettighetsperiodeVurderingResponse(
                                begrunnelse = it.begrunnelse,
                                startDato = it.startDato,
                                harRettUtoverSøknadsdato = it.harRettUtoverSøknadsdato,
                                harKravPåRenter = it.harKravPåRenter,
                                vurdertAv =
                                    VurdertAvResponse(
                                        ident = it.vurdertAv,
                                        dato =
                                            it.vurdertDato?.toLocalDate()
                                                ?: error("Mangler vurdertdato på rettighetsperiodevurderingen"),
                                        ansattnavn = ansattNavnOgEnhet?.navn,
                                        enhetsnavn = ansattNavnOgEnhet?.enhet
                                    )
                            )
                        },
                    søknadsdato = datoFraDokumentUtleder.utledSøknadsdatoForSak(behandling.sakId)?.toLocalDate(),
                    harTilgangTilÅSaksbehandle = kanSaksbehandle()
                )
            }
            respond(rettighetsperiodeGrunnlagDto)
        }
}
