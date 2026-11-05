package no.nav.aap.utenlandsopphold

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

data class SøknadUtenlandsoppholdVurderingResponse(
    val søknadsperiode: Periode,
    val land: String,
    val resultat: SøknadUtenlandsoppholdResultat,
    val godkjentPeriode: Periode?,
    val antallGodkjenteDager: Int?,
    val begrunnelse: String,
    val vurderingerMeta: VurderingerMetaResponse,
)

data class BekreftBrevSendtResponse(
    val vurderingerMeta: VurderingerMetaResponse,
)

data class SøknadUtenlandsoppholdGrunnlagResponse(
    val søknad: SøknadUtenlandsoppholdDokumentDto,
    val vurdering: SøknadUtenlandsoppholdVurderingResponse?,
    val bekreftelse: BekreftBrevSendtResponse?,
)

fun NormalOpenAPIRoute.søknadUtenlandsoppholdGrunnlagApi(
    dataSource: DataSource, repositoryRegistry: RepositoryRegistry, gatewayProvider: GatewayProvider,
) {
    route("/api/behandling/{referanse}/grunnlag/soknad-utenlandsopphold")
        .getGrunnlag<BehandlingReferanse, SøknadUtenlandsoppholdGrunnlagResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            påkrevdRolle = Definisjon.VURDER_SØKNAD_UTENLANDSOPPHOLD.løsesAv
        ) { req ->
            val respons = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandling = BehandlingReferanseService(repositoryProvider.provide<BehandlingRepository>())
                    .behandling(req)

                val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)

                val søknad = requireNotNull(
                    MottaDokumentService(repositoryProvider.provide()).hentSøknadUtenlandsoppholdDokument(behandling.id)
                ) { "Skal alltid finnes dokument på behandling av søknad om opphold i utlandet. BehandlingId: ${behandling.id}" }

                val vurdering = repositoryProvider.provide<SøknadUtenlandsoppholdRepository>()
                    .hentHvisEksisterer(behandling.id)?.vurdering
                val bekreftelseBehov = repositoryProvider.provide<AvklaringsbehovRepository>()
                    .hentAvklaringsbehovene(behandling.id)
                    .hentBehovForDefinisjon(Definisjon.BEKREFT_BREV_SENDT_MANUELT)
                    ?.takeIf { it.harLøsning() }

                SøknadUtenlandsoppholdGrunnlagResponse(
                    søknad = søknad,
                    vurdering = vurdering?.let {
                        SøknadUtenlandsoppholdVurderingResponse(
                            søknadsperiode = it.søknadsperiode,
                            land = it.land,
                            resultat = it.resultat,
                            godkjentPeriode = it.godkjentPeriode,
                            antallGodkjenteDager = it.antallGodkjenteDager,
                            begrunnelse = it.begrunnelse,
                            vurderingerMeta = vurdertAvService.byggVurderingerMeta(
                                definisjon = Definisjon.VURDER_SØKNAD_UTENLANDSOPPHOLD,
                                behandlingId = behandling.id,
                                vurdertAv = vurdertAvService.medNavnOgEnhet(it.vurdertAv, it.vurdertTidspunkt),
                            ),
                        )
                    },
                    bekreftelse = bekreftelseBehov?.let {
                        BekreftBrevSendtResponse(
                            vurderingerMeta = vurdertAvService.byggVurderingerMeta(
                                definisjon = Definisjon.BEKREFT_BREV_SENDT_MANUELT,
                                behandlingId = behandling.id,
                                vurdertAv = vurdertAvService.medNavnOgEnhet(requireNotNull(it.løstAv()), it.sistAvsluttet()),
                            ),
                        )
                    },
                )
            }
            respond(respons)
        }
}
