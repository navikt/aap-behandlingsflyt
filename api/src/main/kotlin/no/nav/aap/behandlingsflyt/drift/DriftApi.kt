package no.nav.aap.behandlingsflyt.drift

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.tidligerevurderinger.BehandlingsutfallType
import no.nav.aap.behandlingsflyt.behandling.tidligerevurderinger.TidligereVurderingDto
import no.nav.aap.behandlingsflyt.behandling.tidligerevurderinger.TidligereVurderingerDto
import no.nav.aap.behandlingsflyt.behandling.tidligerevurderinger.TidligereVurderingerReq
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelse2Dto
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.periodisering.FlytKontekstMedPeriodeService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksnummerParameter
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.tilgang.plugin.kontrakt.BehandlingreferanseResolver
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status as MottattDokumentStatus
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status as BehandlingStatus
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status as SakStatus

/**
 * API for å utføre manuelle operasjoner med forsøk på å rette opp i låste saker av varierende grunn.
 * Ikke bruk dette ved mindre du vet hva du gjør.
 * Med tid kan vi ha et admin-verktøy for alle disse.
 * */

private val log = LoggerFactory.getLogger("driftApi")
fun NormalOpenAPIRoute.driftApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/drift") {
        data class KjorFraSteg(val steg: StegType)
        route("/behandling/{referanse}/kjor-fra-steg") {
            authorizedPost<BehandlingReferanse, Unit, KjorFraSteg>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    operasjon = Operasjon.DRIFTE
                )
            ) { params, request ->
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val driftfunksjoner = Driftfunksjoner(repositoryProvider, gatewayProvider)

                    val behandling = behandlingRepository.hent(BehandlingReferanse(params.referanse))
                    driftfunksjoner.kjørFraSteg(behandling, request.steg)
                }
                respondWithStatus(HttpStatusCode.NoContent)
            }
        }

        data class ProsesserBehandling(val skalForberede: Boolean)
        route("/behandling/{referanse}/prosesser") {
            authorizedPost<BehandlingReferanse, Unit, ProsesserBehandling>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    operasjon = Operasjon.DRIFTE
                )
            ) { params, request ->
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val driftfunksjoner = Driftfunksjoner(repositoryProvider, gatewayProvider)

                    val behandling = behandlingRepository.hent(BehandlingReferanse(params.referanse))
                    driftfunksjoner.prosesserBehandling(behandling, request.skalForberede)
                }
                respondWithStatus(HttpStatusCode.NoContent)
            }
        }


        route("/behandling/{referanse}/utvid-rettighetsperiode-og-kjor-fra-start") {
            authorizedPost<BehandlingReferanse, Unit, Unit>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    operasjon = Operasjon.DRIFTE
                )
            ) { params, request ->
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val driftfunksjoner = Driftfunksjoner(repositoryProvider, gatewayProvider)

                    val behandling = behandlingRepository.hent(BehandlingReferanse(params.referanse))
                    driftfunksjoner.utvidRettghetsperiodeOgKjørFraStart(behandling)
                }
                respondWithStatus(HttpStatusCode.NoContent)
            }
        }

        data class AvbrytBrevBody(val begrunnelse: String)
        route("/brev/{brevbestillingReferanse}/avbryt") {
            authorizedPost<BrevbestillingReferanse, Unit, AvbrytBrevBody>(
                AuthorizationParamPathConfig(
                    operasjon = Operasjon.DRIFTE,
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    behandlingPathParam = BehandlingPathParam(
                        "brevbestillingReferanse",
                        behandlingFraBrevbestilling(repositoryRegistry, dataSource)
                    )
                )
            ) { param, req ->
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val driftsfunksjoner = Driftfunksjoner(repositoryProvider, gatewayProvider)

                    driftsfunksjoner.avbrytVedtsaksbrevBestilling(bruker(), param, req.begrunnelse)

                    log.info("Brevbestilling med referanse $param er avbrutt av ${bruker()}.")
                }
                respondWithStatus(HttpStatusCode.NoContent)
            }
        }

        route("/behandling/{referanse}/vilkår") {
            authorizedPost<BehandlingReferanse, List<VilkårDriftsinfoDTO>, Unit>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    operasjon = Operasjon.DRIFTE
                )
            ) { params, _ ->
                val vilkår = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()

                    val vilkårRepository = repositoryProvider.provide<VilkårsresultatRepository>()

                    val behandling = behandlingRepository.hent(BehandlingReferanse(params.referanse))

                    vilkårRepository.hent(behandling.id)
                        .alle()
                        .map { vilkår ->
                            VilkårDriftsinfoDTO(
                                vilkår.type,
                                perioder = vilkår.vilkårsperioder().map { vp ->
                                    ForenkletVilkårsperiode(
                                        vp.periode,
                                        vp.utfall,
                                        vp.manuellVurdering,
                                        vp.avslagsårsak,
                                        vp.innvilgelsesårsak
                                    )
                                },
                                vurdertTidspunkt = vilkår.vurdertTidspunkt
                            )
                        }
                        .sortedBy { it.vurdertTidspunkt }
                }

                krevDtoErUtenFødselsnummer(vilkår)

                respond(vilkår)
            }
        }

        route("/behandling/{referanse}/tilkjent-ytelse") {
            authorizedPost<BehandlingReferanse, TilkjentYtelse2Dto, Unit>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    operasjon = Operasjon.DRIFTE
                )
            ) { params, _ ->
                val tilkjentYtelseDto = TilkjentYtelseService(dataSource, repositoryRegistry)
                    .hentTilkjentYtelse(params)

                krevDtoErUtenFødselsnummer(tilkjentYtelseDto)

                respond(tilkjentYtelseDto)
            }
        }

        @Suppress("unused")
        class StansOpphørDTO(
            val fom: LocalDate,
            val stansOpphør: String,
            val årsaker: List<String>,
        )
        @Suppress("unused")
        class RettighetstypePeriodeDto(
            val periode: Periode,
            val rettighetstypeUnderveis: RettighetsType?,
            val rettighetstypeGrunnlag: RettighetsType?,
        )
        @Suppress("unused")
        class DriftRettighetsinfoDto(
            val sisteDagMedRett: LocalDate?,
            val rettighetsperioder: List<RettighetstypePeriodeDto>,
            val stansOpphør: List<StansOpphørDTO>,
        )
        route("/behandling/{referanse}/rettighetsinfo") {
            authorizedPost<BehandlingReferanse, DriftRettighetsinfoDto, Unit>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    operasjon = Operasjon.DRIFTE
                )
            ) { params, _ ->
                val res = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val rettighetstypeRepository = repositoryProvider.provide<RettighetstypeRepository>()
                    val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val stansOpphørRepository = repositoryProvider.provide<StansOpphørRepository>()

                    val behandling = behandlingRepository.hent(params)
                    val grunnlagRettighetstyper = rettighetstypeRepository
                        .hentHvisEksisterer(behandling.id)
                        ?.rettighetstypeTidslinje
                        .orEmpty()
                        .komprimer()
                    val underveisTidslinje = underveisRepository.hentHvisEksisterer(behandling.id)
                        ?.somTidslinje()
                        .orEmpty()
                        .mapNotNull { it.rettighetsType }
                        .komprimer()

                    val rettighetstyper =
                        grunnlagRettighetstyper.outerJoin(underveisTidslinje) { periode, rettighetstypeGrunnlag, rettighetstypeUnderveis ->
                            RettighetstypePeriodeDto(
                                periode = periode,
                                rettighetstypeGrunnlag = rettighetstypeGrunnlag,
                                rettighetstypeUnderveis = rettighetstypeUnderveis,
                            )
                        }


                    val sisteDagMedRett = grunnlagRettighetstyper.perioder().maxOfOrNull { it.tom }

                    DriftRettighetsinfoDto(
                        sisteDagMedRett = sisteDagMedRett,
                        rettighetsperioder = rettighetstyper.verdier().toList(),
                        stansOpphør = stansOpphørRepository.hentHvisEksisterer(behandling.id)
                            ?.gjeldendeStansOgOpphør()
                            .orEmpty()
                            .map {
                                StansOpphørDTO(
                                    fom = it.fom,
                                    stansOpphør = when (it.vurdering) {
                                        is Opphør -> "OPPHØR"
                                        is Stans -> "STANS"
                                    },
                                    årsaker = it.vurdering.årsaker.toList().map { it.toString() },
                                )
                            },
                    )
                }
                respond(res)
            }
        }

        route("/behandling/{referanse}/tidligere-vurderinger").authorizedGet<TidligereVurderingerReq, TidligereVurderingerDto>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam(
                    "referanse"
                ),
                operasjon = Operasjon.DRIFTE
            )
        ) { req ->
            val response = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)

                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val behandling =
                    BehandlingReferanseService(behandlingRepository).behandling(BehandlingReferanse(req.referanse))

                val kontekst = FlytKontekstMedPeriodeService(repositoryProvider, gatewayProvider).utled(
                    behandling.flytKontekst(),
                    req.førSteg
                )

                val tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider)
                TidligereVurderingerDto(
                    tidligereVurderinger.behandlingsutfall(kontekst, req.førSteg, req.etterSteg).segmenter().map {
                        val verdi = it.verdi
                        TidligereVurderingDto(
                            periode = it.periode,
                            utfall = BehandlingsutfallType.fraBehandlingsutfall(verdi),
                            rettighetstype = when (verdi) {
                                is TidligereVurderinger.PotensieltOppfylt -> verdi.rettighetstype
                                else -> null
                            },
                            muligRettighetstypeFraNavkontor = when (verdi) {
                                is TidligereVurderinger.PotensieltOppfylt -> verdi.muligRettFraNavKontor
                                else -> null
                            }
                        )
                    })
            }

            respond(response)
        }

        route("/sak/{saksnummer}/info") {
            authorizedPost<SaksnummerParameter, SakDriftsinfoDTO, Unit>(
                AuthorizationParamPathConfig(
                    sakPathParam = SakPathParam("saksnummer"),
                    operasjon = Operasjon.DRIFTE,
                ),
            ) { params, _ ->
                val sakDriftsinfoDTO = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)

                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()

                    val sak = sakRepository.hentHvisFinnes(Saksnummer(params.saksnummer))
                        ?: throw VerdiIkkeFunnetException("Sak med saksnummer ${params.saksnummer} finnes ikke")

                    val andreSakerPåBruker = sakRepository.finnSakerFor(sak.person.id)
                        .filterNot { it.id == sak.id }
                        .map { it.saksnummer.toString() }

                    val vedtak = behandlingRepository.hentAlleMedVedtakFor(sak.person.id)
                    val behandlinger = behandlingRepository.hentAlleFor(sak.id)
                        .map { behandling ->
                            val avklaringsbehovene = avklaringsbehovRepository
                                .hentAvklaringsbehovene(behandling.id)
                                .alle()
                                .flatMap { avklaringsbehov ->
                                    avklaringsbehov.historikk.map { endring ->
                                        ForenkletAvklaringsbehov(
                                            definisjon = avklaringsbehov.definisjon,
                                            status = endring.status,
                                            årsakTilSettPåVent = endring.grunn,
                                            perioderUgyldigVurdering = endring.perioderSomIkkeErTilstrekkeligVurdert,
                                            perioderKreverVurdering = endring.perioderVedtaketBehøverVurdering,
                                            tidsstempel = endring.tidsstempel,
                                            endretAv = endring.endretAv
                                        )
                                    }
                                }.sortedByDescending { it.tidsstempel }

                            BehandlingDriftsinfo.fra(behandling, avklaringsbehovene, vedtak.find { it.id == behandling.id }?.vedtakstidspunkt )
                        }
                        .sortedByDescending { it.opprettet }

                    SakDriftsinfoDTO(
                        saksnummer = sak.saksnummer.toString(),
                        status = sak.status(),
                        rettighetsperiode = sak.rettighetsperiode,
                        opprettetTidspunkt = sak.opprettetTidspunkt,
                        behandlinger = behandlinger,
                        andreSakerPåBruker = andreSakerPåBruker,
                    )
                }

                krevDtoErUtenFødselsnummer(sakDriftsinfoDTO)

                respond(sakDriftsinfoDTO)
            }
        }

        route("/sak/{saksnummer}/mottatte-dokumenter") {
            authorizedPost<SaksnummerParameter, List<MottattDokumentDriftsinfoDTO>, Unit>(
                AuthorizationParamPathConfig(
                    sakPathParam = SakPathParam("saksnummer"),
                    operasjon = Operasjon.DRIFTE,
                ),
            ) { params, _ ->
                val dokumenter = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)

                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>()

                    val sak = sakRepository.hentHvisFinnes(Saksnummer(params.saksnummer))
                        ?: throw VerdiIkkeFunnetException("Sak med saksnummer ${params.saksnummer} finnes ikke")

                    mottattDokumentRepository.hentDokumenterForSak(sak.id)
                        .map { dokument ->
                            MottattDokumentDriftsinfoDTO(
                                referanse = dokument.referanse,
                                mottattTidspunkt = dokument.mottattTidspunkt,
                                type = dokument.type,
                                kanal = dokument.kanal,
                                status = dokument.status
                            )
                        }
                        .sortedByDescending { it.mottattTidspunkt }
                }

                krevDtoErUtenFødselsnummer(dokumenter)

                respond(dokumenter)
            }
        }

        route("/sak/{saksnummer}/oppdater-person-identer") {
            authorizedPost<SaksnummerParameter, SakDriftsinfoDTO, Unit>(
                AuthorizationParamPathConfig(
                    sakPathParam = SakPathParam("saksnummer"),
                    operasjon = Operasjon.DRIFTE,
                ),
            ) { params, _ ->
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val driftfunksjoner = Driftfunksjoner(repositoryProvider, gatewayProvider)
                    driftfunksjoner.oppdaterPersonIdenter(Saksnummer(params.saksnummer))
                }

                respondWithStatus(HttpStatusCode.NoContent)
            }
        }

    }
}

private fun krevDtoErUtenFødselsnummer(dto: Any) {
    if (Regex("""(?<!\w)\d{11}(?!\w)""").containsMatchIn(DefaultJsonMapper.toJson(dto))) {
        throw IkkeTillattException("DTO-en inneholder (potensielt) sensitive personopplysninger!")
    }
}

private data class SakDriftsinfoDTO(
    val saksnummer: String,
    val status: SakStatus,
    val rettighetsperiode: Periode,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    val behandlinger: List<BehandlingDriftsinfo>,
    val andreSakerPåBruker: List<String>,
)

private data class BehandlingDriftsinfo(
    val referanse: UUID,
    val type: String,
    val status: BehandlingStatus,
    val vurderingsbehov: List<Vurderingsbehov>,
    val årsakTilOpprettelse: ÅrsakTilOpprettelse?,
    val opprettet: LocalDateTime,
    val vedtatt: LocalDateTime?,
    val avklaringsbehov: List<ForenkletAvklaringsbehov>,
) {
    companion object {
        fun fra(behandling: Behandling, avklaringsbehovene: List<ForenkletAvklaringsbehov>, vedtatt: LocalDateTime?) =
            BehandlingDriftsinfo(
                referanse = behandling.referanse.referanse,
                type = behandling.typeBehandling().identifikator(),
                status = behandling.status(),
                vurderingsbehov = behandling.vurderingsbehov().map(VurderingsbehovMedPeriode::type).distinct(),
                årsakTilOpprettelse = behandling.årsakTilOpprettelse,
                opprettet = behandling.opprettetTidspunkt,
                avklaringsbehov = avklaringsbehovene,
                vedtatt = vedtatt,
            )
    }
}

private data class ForenkletAvklaringsbehov(
    val definisjon: Definisjon,
    val status: Status,
    val perioderUgyldigVurdering: Set<Periode>?,
    val perioderKreverVurdering: Set<Periode>?,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val endretAv: String,
    val årsakTilSettPåVent: ÅrsakTilSettPåVent?
)

private data class VilkårDriftsinfoDTO(
    val type: Vilkårtype,
    val perioder: List<ForenkletVilkårsperiode>,
    val vurdertTidspunkt: LocalDateTime?,
)

private data class ForenkletVilkårsperiode(
    val periode: Periode,
    val utfall: Utfall,
    val manuellVurdering: Boolean,
    val avslagsårsak: Avslagsårsak?,
    val innvilgelsesårsak: Innvilgelsesårsak?
)

private data class MottattDokumentDriftsinfoDTO(
    val referanse: InnsendingReferanse,
    val mottattTidspunkt: LocalDateTime,
    val type: InnsendingType,
    val kanal: Kanal,
    val status: MottattDokumentStatus = MottattDokumentStatus.MOTTATT,
)

fun behandlingFraBrevbestilling(
    repositoryRegistry: RepositoryRegistry,
    dataSource: DataSource
): BehandlingreferanseResolver {
    return BehandlingreferanseResolver { referanse ->
        dataSource.transaction(readOnly = true) {
            repositoryRegistry.provider(it).provide(BrevbestillingRepository::class)
                .hentBehandlingsreferanseForBestilling(BrevbestillingReferanse(UUID.fromString(referanse))).referanse
        }
    }
}
