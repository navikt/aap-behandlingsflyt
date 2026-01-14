package no.nav.aap.behandlingsflyt.behandling.inntektsbortfall

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall.InntektSiste3ÅrOver3G
import no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall.InntektSisteÅrOver1G
import no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall.InntektsbortfallKanBehandlesAutomatisk
import no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall.InntektsbortfallVurderingService
import no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall.Under62ÅrVedSøknadstidspunkt
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.InntektsbortfallVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.math.BigDecimal
import java.time.LocalDate
import javax.sql.DataSource

data class InntektsbortfallGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val grunnlag: InntektsbortfallKanBehandlesAutomatiskDto,
    val vurdering: InntektsbortfallVurderingDto?
)

data class InntektsbortfallKanBehandlesAutomatiskDto(
    val kanBehandlesAutomatisk: Boolean,
    val inntektSisteÅrOver1G: InntektSisteÅrOver1GDto,
    val inntektSiste3ÅrOver3G: InntektSiste3ÅrOver3GDto,
    val under62ÅrVedSøknadstidspunkt: Under62ÅrVedSøknadstidspunktDto,
)

data class InntektSisteÅrOver1GDto(
    val gverdi: BigDecimal,
    val resultat: Boolean
)

data class InntektSiste3ÅrOver3GDto(
    val gverdi: BigDecimal,
    val resultat: Boolean
)

data class Under62ÅrVedSøknadstidspunktDto(
    val alder: Int,
    val resultat: Boolean
)

data class InntektsbortfallVurderingDto(
    val begrunnelse: String,
    val rettTilUttak: Boolean,
    val vurdertIBehandling: BehandlingId,
    override val besluttetAv: VurdertAvResponse?,
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertAv: VurdertAvResponse?,
    override val kvalitetssikretAv: VurdertAvResponse?,
) : VurderingDto

fun InntektsbortfallVurdering.tilDto(fom: LocalDate, vurdertAvService: VurdertAvService): InntektsbortfallVurderingDto =
    InntektsbortfallVurderingDto(
        begrunnelse = begrunnelse,
        rettTilUttak = rettTilUttak,
        vurdertIBehandling = vurdertIBehandling,
        besluttetAv = vurdertAvService.besluttetAv(
            definisjon = Definisjon.FRITAK_MELDEPLIKT,
            behandlingId = vurdertIBehandling
        ),
        kvalitetssikretAv = vurdertAvService.besluttetAv(
            definisjon = Definisjon.FRITAK_MELDEPLIKT,
            behandlingId = vurdertIBehandling
        ),
        fom = fom,
        tom = null,
        vurdertAv = vurdertAvService.medNavnOgEnhet(vurdertAv, opprettetTid),
    )

fun Under62ÅrVedSøknadstidspunkt.tilDto(): Under62ÅrVedSøknadstidspunktDto = Under62ÅrVedSøknadstidspunktDto(
    alder = alder,
    resultat = resultat
)

fun InntektSiste3ÅrOver3G.tilDto(): InntektSiste3ÅrOver3GDto =
    InntektSiste3ÅrOver3GDto(
        gverdi = gverdi.verdi(),
        resultat = resultat
    )

fun InntektSisteÅrOver1G.tilDto(): InntektSisteÅrOver1GDto =
    InntektSisteÅrOver1GDto(
        gverdi = gverdi.verdi(),
        resultat = resultat
    )

fun InntektsbortfallKanBehandlesAutomatisk.tilDto(): InntektsbortfallKanBehandlesAutomatiskDto =
    InntektsbortfallKanBehandlesAutomatiskDto(
        kanBehandlesAutomatisk = kanBehandlesAutomatisk,
        inntektSisteÅrOver1G = inntektSisteÅrOver1G.tilDto(),
        inntektSiste3ÅrOver3G = inntektSiste3ÅrOver3G.tilDto(),
        under62ÅrVedSøknadstidspunkt = under62ÅrVedSøknadstidspunkt.tilDto()
    )


fun NormalOpenAPIRoute.inntektsbortfallGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling/{referanse}/grunnlag/inntektsbortfall") {
        getGrunnlag<BehandlingReferanse, InntektsbortfallGrunnlagResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.VURDER_INNTEKTSBORTFALL.kode.toString()

        ) { behandlingReferanse ->
            val (inntektsbortfallKanBehandlesAutomatisk, vurdering) = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val sakRepository = repositoryProvider.provide<SakRepository>()
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val personopplysningRepository = repositoryProvider.provide<PersonopplysningRepository>()
                val manuellInntektGrunnlagRepository = repositoryProvider.provide<ManuellInntektGrunnlagRepository>()
                val inntektGrunnlagRepository = repositoryProvider.provide<InntektGrunnlagRepository>()
                val inntektsbortfallRepository = repositoryProvider.provide<InntektsbortfallRepository>()

                val behandling: Behandling =
                    BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)

                val sak = sakRepository.hent(behandling.sakId)

                val beregningService = BeregningService(repositoryProvider)
                val relevanteBeregningsår =
                    beregningService.utledRelevanteBeregningsÅr(behandling.id)
                val brukerPersonopplysning =
                    personopplysningRepository.hentBrukerPersonOpplysningHvisEksisterer(behandling.id)!!
                val manuelleInntekter = manuellInntektGrunnlagRepository.hentHvisEksisterer(behandling.id)
                val inntektGrunnlag = inntektGrunnlagRepository.hentHvisEksisterer(behandling.id)

                val kombinerteInntekter = beregningService.kombinerInntektOgManuellInntekt(
                    inntektGrunnlag?.inntekter.orEmpty(),
                    manuelleInntekter?.manuelleInntekter.orEmpty()
                )

                val vurdering = inntektsbortfallRepository.hentHvisEksisterer(behandling.id)
                    ?.tilDto(sak.rettighetsperiode.fom, VurdertAvService(repositoryProvider, gatewayProvider))

                Pair(
                    InntektsbortfallVurderingService(
                        rettighetsperiode = sak.rettighetsperiode,
                        relevanteBeregningsår = relevanteBeregningsår,
                    ).vurderInntektsbortfall(
                        brukerPersonopplysning.fødselsdato,
                        kombinerteInntekter
                    ).tilDto(), vurdering
                )
            }

            respond(
                InntektsbortfallGrunnlagResponse(
                    harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                    grunnlag = inntektsbortfallKanBehandlesAutomatisk,
                    vurdering = vurdering
                )
            )
        }
    }
}
