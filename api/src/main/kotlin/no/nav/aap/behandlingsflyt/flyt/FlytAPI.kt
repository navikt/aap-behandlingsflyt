package no.nav.aap.behandlingsflyt.flyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.BehandlingTilstandValidator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.flate.visning.DynamiskStegGruppeVisningService
import no.nav.aap.behandlingsflyt.flyt.flate.visning.ProsesseringStatus
import no.nav.aap.behandlingsflyt.flyt.flate.visning.Visning
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.MANUELT_SATT_PÅ_VENT_KODE
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.mdc.LogKontekst
import no.nav.aap.behandlingsflyt.mdc.LoggingKontekst
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbStatus
import no.nav.aap.motor.api.JobbInfoDto
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("flytApi")

fun NormalOpenAPIRoute.flytApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val unleashGateway = gatewayProvider.provide<UnleashGateway>()
    val tilgangGateway = gatewayProvider.provide<TilgangGateway>()

    route("/api/behandling") {
        route("/{referanse}/flyt") {
            authorizedGet<BehandlingReferanse, BehandlingFlytOgTilstandDto>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val resultatUtleder = ResultatUtleder(repositoryProvider)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val vilkårsresultatRepository =
                        repositoryProvider.provide<VilkårsresultatRepository>()
                    val avklaringsbehovRepository =
                        repositoryProvider.provide<AvklaringsbehovRepository>()

                    var behandling = behandling(behandlingRepository, req)
                    val avklaringsbehovene = avklaringsbehov(
                        avklaringsbehovRepository,
                        behandling.id
                    )
                    val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()
                    val gruppeVisningService = DynamiskStegGruppeVisningService(repositoryProvider)

                    val jobber = flytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
                        .filter { it.type() == ProsesserBehandlingJobbUtfører.type }

                    val prosessering =
                        Prosessering(
                            utledStatus(jobber, avklaringsbehovene),
                            jobber.map {
                                JobbInfoDto(
                                    id = it.jobbId(),
                                    type = it.type(),
                                    status = it.status(),
                                    planlagtKjøretidspunkt = it.nesteKjøring(),
                                    metadata = emptyMap(),
                                    antallFeilendeForsøk = it.antallRetriesForsøkt(),
                                    feilmelding = hentFeilmeldingHvisBehov(
                                        it.status(),
                                        it.jobbId(),
                                        flytJobbRepository
                                    ),
                                    beskrivelse = it.beskrivelse(),
                                    navn = it.navn(),
                                    opprettetTidspunkt = it.opprettetTidspunkt(),
                                )
                            })
                    // Henter denne ut etter status er utledet for å være sikker på at dataene er i rett tilstand
                    behandling = behandling(behandlingRepository, req)
                    val flyt = behandling.flyt()
                    val stegGrupper: Map<StegGruppe, List<StegType>> =
                        flyt.stegene().groupBy { steg -> steg.gruppe }
                    val aktivtSteg = behandling.aktivtSteg()
                    val aktivtStegDefinisjon = Definisjon.fraStegType(aktivtSteg)
                    var erFullført = true

                    val alleAvklaringsbehovInkludertFrivillige = FrivilligeAvklaringsbehov(
                        avklaringsbehovene,
                        flyt, aktivtSteg
                    )
                    val vurdertStegPair =
                        utledVurdertGruppe(prosessering, aktivtSteg, flyt, avklaringsbehovene)
                    val vilkårsresultat = vilkårResultat(vilkårsresultatRepository, behandling.id)
                    val alleAvklaringsbehov = alleAvklaringsbehovInkludertFrivillige.alle()
                    val resultatKode = when {
                        ((behandling.typeBehandling() == TypeBehandling.Revurdering) && (resultatUtleder.utledResultatRevurderingsBehandling(
                            behandling) == Resultat.AVBRUTT)) -> ResultatKode.AVBRUTT

                        else -> null
                    }

                    LoggingKontekst(
                        repositoryProvider,
                        LogKontekst(referanse = BehandlingReferanse(req.referanse))
                    ).use {
                        val behandlingVersjon = behandling.versjon
                        log.info("Henter flyt med behandlingversjon: $behandlingVersjon")
                    }


                    BehandlingFlytOgTilstandDto(
                        flyt = stegGrupper.map { (gruppe, steg) ->
                            erFullført = erFullført && gruppe != aktivtSteg.gruppe
                            FlytGruppe(
                                stegGruppe = gruppe,
                                skalVises = gruppeVisningService.skalVises(gruppe, behandling.id),
                                erFullført = erFullført,
                                steg = steg.map { stegType ->
                                    FlytSteg(
                                        stegType = stegType,
                                        avklaringsbehov = alleAvklaringsbehov
                                            .filter { avklaringsbehov ->
                                                avklaringsbehov.skalLøsesISteg(
                                                    stegType
                                                )
                                            }
                                            .map { behov ->
                                                AvklaringsbehovDTO(
                                                    behov.definisjon,
                                                    behov.status(),
                                                    emptyList()
                                                )
                                            },
                                        vilkårDTO = hentUtRelevantVilkårForSteg(
                                            vilkårsresultat = vilkårsresultat,
                                            stegType = stegType
                                        )
                                    )
                                }
                            )
                        },
                        aktivtSteg = aktivtSteg,
                        aktivtStegDefinisjon = aktivtStegDefinisjon,
                        aktivGruppe = aktivtSteg.gruppe,
                        vurdertSteg = vurdertStegPair?.second,
                        vurdertGruppe = vurdertStegPair?.first,
                        behandlingVersjon = behandling.versjon,
                        prosessering = prosessering,
                        visning = utledVisning(
                            aktivtSteg = aktivtSteg,
                            flyt = flyt,
                            alleAvklaringsbehovInkludertFrivillige = alleAvklaringsbehovInkludertFrivillige,
                            status = prosessering.status,
                            typeBehandling = behandling.typeBehandling(),
                            avklaringsbehov = alleAvklaringsbehov,
                            bruker = bruker(),
                            behandlingStatus = behandling.status(),
                            unleashGateway = unleashGateway,
                            resultatKode
                        )
                    )
                }
                respond(dto)
            }
        }
        route("/{referanse}/resultat") {
            authorizedGet<BehandlingReferanse, BehandlingResultatDto>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    behandlingPathParam = BehandlingPathParam("referanse")
                ),
            ) { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val vilkårsresultatRepository =
                        repositoryProvider.provide<VilkårsresultatRepository>()

                    val behandling = behandling(behandlingRepository, req)

                    val vilkårResultat = vilkårResultat(vilkårsresultatRepository, behandling.id)

                    BehandlingResultatDto(alleVilkår(vilkårResultat))
                }
                respond(dto)
            }
        }
        route("/{referanse}/sett-på-vent") {
            authorizedPost<BehandlingReferanse, BehandlingResultatDto, SettPåVentRequest>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    operasjon = Operasjon.SAKSBEHANDLE,
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    avklaringsbehovKode = MANUELT_SATT_PÅ_VENT_KODE
                )
            ) { request, body ->
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    LoggingKontekst(
                        repositoryProvider,
                        LogKontekst(referanse = BehandlingReferanse(request.referanse))
                    ).use {
                        val behandlingRepository =
                            repositoryProvider.provide<BehandlingRepository>()
                        val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()
                        BehandlingTilstandValidator(
                            BehandlingReferanseService(behandlingRepository),
                            flytJobbRepository
                        ).validerTilstand(
                            request,
                            body.behandlingVersjon
                        )
                        val avklaringsbehovRepository =
                            repositoryProvider.provide<AvklaringsbehovRepository>()
                        val behandlingId = behandling(behandlingRepository, request).id
                        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
                        sjekkTilgangTilSettPåVent(avklaringsbehovene, tilgangGateway, request.referanse, token())


                        val taSkriveLåsRepository =
                            repositoryProvider.provide<TaSkriveLåsRepository>()
                        val lås = taSkriveLåsRepository.lås(request.referanse)
                        AvklaringsbehovOrkestrator(repositoryProvider, gatewayProvider)
                            .settBehandlingPåVent(
                                lås.behandlingSkrivelås.id, BehandlingSattPåVent(
                                    frist = body.frist,
                                    begrunnelse = body.begrunnelse,
                                    behandlingVersjon = body.behandlingVersjon,
                                    grunn = body.grunn,
                                    bruker = bruker()
                                )
                            )
                        taSkriveLåsRepository.verifiserSkrivelås(lås)

                    }
                }
                respondWithStatus(HttpStatusCode.NoContent)
            }
        }
        route("/{referanse}/vente-informasjon") {
            authorizedGet<BehandlingReferanse, Venteinformasjon>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            ) { request ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val avklaringsbehovRepository =
                        repositoryProvider.provide<AvklaringsbehovRepository>()

                    val behandling = behandling(behandlingRepository, request)
                    val avklaringsbehovene =
                        avklaringsbehov(avklaringsbehovRepository, behandling.id)

                    val ventepunkter = avklaringsbehovene.hentÅpneVentebehov()
                    if (avklaringsbehovene.erSattPåVent()) {
                        val avklaringsbehov = ventepunkter.first()
                        Venteinformasjon(
                            avklaringsbehov.definisjon,
                            avklaringsbehov.frist(),
                            avklaringsbehov.begrunnelse(),
                            requireNotNull(avklaringsbehov.venteårsak())
                        )
                    } else {
                        null
                    }
                }
                if (dto == null) {
                    respondWithStatus(HttpStatusCode.NoContent)
                } else {
                    respond(dto)
                }
            }
        }
    }
}

private fun sjekkTilgangTilSettPåVent(
    avklaringsbehovene: Avklaringsbehovene,
    tilgangGateway: TilgangGateway,
    behandlingsreferanse: UUID,
    token: OidcToken
) {
    val åpentAvklaringsbehov = avklaringsbehovene.åpne().first().definisjon
    val harTilgang =
        tilgangGateway.sjekkTilgangTilBehandling(
            behandlingsreferanse,
            åpentAvklaringsbehov,
            token
        )

    if (!harTilgang) {
        throw IkkeTillattException("Ikke tilgang til å sette behandlingen på vent.")
    }
}

// Utleder hvilken visningsgruppe frontend skal rute til for kvalitetsikring / totrinn
private fun utledVurdertGruppe(
    prosessering: Prosessering,
    aktivtSteg: StegType,
    flyt: BehandlingFlyt,
    avklaringsbehovene: Avklaringsbehovene
): Pair<StegGruppe, StegType>? {
    if (prosessering.status == ProsesseringStatus.FERDIG) {
        if (aktivtSteg == StegType.KVALITETSSIKRING) {
            val relevanteBehov = avklaringsbehovene.alle().filter { it.kreverKvalitetssikring() }
                .filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
                .filter { avklaringsbehov -> !avklaringsbehov.erKvalitetssikretTidligere() }

            val skalTilStegForBehov = flyt.skalTilStegForBehov(relevanteBehov)
            val stegType = requireNotNull(skalTilStegForBehov)

            return stegType.gruppe to stegType
        } else if (aktivtSteg == StegType.FATTE_VEDTAK) {
            val relevanteBehov = avklaringsbehovene.alle().filter { it.erTotrinn() }
                .filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
                .filter { avklaringsbehov -> !avklaringsbehov.erTotrinnsVurdert() }

            val skalTilStegForBehov = flyt.skalTilStegForBehov(relevanteBehov)
            val stegType = requireNotNull(skalTilStegForBehov)

            return stegType.gruppe to stegType
        }
    }
    return null
}

private fun hentFeilmeldingHvisBehov(
    status: JobbStatus,
    jobbId: Long,
    flytJobbRepository: FlytJobbRepository
): String? {
    if (status == JobbStatus.FEILET) {
        return flytJobbRepository.hentFeilmeldingForOppgave(jobbId)
    }
    return null
}

private fun utledStatus(jobber: List<JobbInput>, avklaringsbehovene: Avklaringsbehovene): ProsesseringStatus {
    if (jobber.isEmpty()) {
        if (avklaringsbehovene.harÅpentBrevVentebehov()) {
            log.info("Har åpent brevventebehov i behandling")
            return ProsesseringStatus.JOBBER
        }
        return ProsesseringStatus.FERDIG
    }
    if (jobber.any { it.status() == JobbStatus.FEILET }) {
        return ProsesseringStatus.FEILET
    }

    log.info("Har følgende jobber som ikke er utført: ${jobber.joinToString(", ") { it.navn() }}")
    return ProsesseringStatus.JOBBER
}

private fun utledVisning(
    aktivtSteg: StegType,
    flyt: BehandlingFlyt,
    alleAvklaringsbehovInkludertFrivillige: FrivilligeAvklaringsbehov,
    status: ProsesseringStatus,
    typeBehandling: TypeBehandling,
    avklaringsbehov: List<Avklaringsbehov>,
    bruker: Bruker,
    behandlingStatus: Status,
    unleashGateway: UnleashGateway,
    resultatKode: ResultatKode?
): Visning {
    val brukerHarIngenValidering = unleashGateway.isEnabled(BehandlingsflytFeature.IngenValidering, bruker.ident)

    val brukerHarKvalitetssikret = avklaringsbehov.filter { it.definisjon === Definisjon.KVALITETSSIKRING }
        .any { it.brukere().contains(bruker.ident) }
    val brukerHarBesluttet =
        avklaringsbehov.filter { it.definisjon === Definisjon.FATTE_VEDTAK }.any { it.brukere().contains(bruker.ident) }

    val jobberEllerFeilet = status in listOf(ProsesseringStatus.JOBBER, ProsesseringStatus.FEILET)
    val påVent = alleAvklaringsbehovInkludertFrivillige.erSattPåVent()
    val beslutterReadOnly = aktivtSteg != StegType.FATTE_VEDTAK
    val erTilKvalitetssikring =
        harÅpentKvalitetssikringsAvklaringsbehov(alleAvklaringsbehovInkludertFrivillige) && aktivtSteg == StegType.KVALITETSSIKRING

    val saksbehandlerReadOnly = if (brukerHarIngenValidering) {
        erTilKvalitetssikring || erEtterBeslutterstegetHvisEksisterer(
            flyt,
            aktivtSteg
        ) || behandlingStatus == Status.AVSLUTTET
    } else {
        erTilKvalitetssikring || erEtterBeslutterstegetHvisEksisterer(
            flyt,
            aktivtSteg
        ) || brukerHarKvalitetssikret || brukerHarBesluttet || behandlingStatus == Status.AVSLUTTET
    }

    val visBeslutterKort =
        !beslutterReadOnly || (!saksbehandlerReadOnly && alleAvklaringsbehovInkludertFrivillige.harVærtSendtTilbakeFraBeslutterTidligere())
    val visKvalitetssikringKort = utledVisningAvKvalitetsikrerKort(alleAvklaringsbehovInkludertFrivillige)
    val kvalitetssikringReadOnly = visKvalitetssikringKort && flyt.erStegFør(aktivtSteg, StegType.KVALITETSSIKRING)
    val visBrevkort =
        alleAvklaringsbehovInkludertFrivillige.hentBehovForDefinisjon(Definisjon.SKRIV_BREV)?.erÅpent() == true
                || alleAvklaringsbehovInkludertFrivillige.hentBehovForDefinisjon(Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV)
            ?.erÅpent() == true
                || alleAvklaringsbehovInkludertFrivillige.hentBehovForDefinisjon(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV)
            ?.erÅpent() == true

    if (jobberEllerFeilet) {
        return Visning(
            saksbehandlerReadOnly = true,
            beslutterReadOnly = true,
            kvalitetssikringReadOnly = true,
            visBeslutterKort = visBeslutterKort,
            visKvalitetssikringKort = visKvalitetssikringKort,
            visVentekort = påVent,
            visBrevkort = false,
            typeBehandling = typeBehandling,
            brukerHarBesluttet = brukerHarBesluttet,
            brukerHarKvalitetssikret = brukerHarKvalitetssikret,
            resultatKode = resultatKode
        )
    } else {
        return Visning(
            saksbehandlerReadOnly = påVent || saksbehandlerReadOnly,
            beslutterReadOnly = påVent || beslutterReadOnly,
            kvalitetssikringReadOnly = påVent || kvalitetssikringReadOnly,
            visBeslutterKort = visBeslutterKort,
            visKvalitetssikringKort = visKvalitetssikringKort,
            visVentekort = påVent,
            visBrevkort = visBrevkort,
            typeBehandling = typeBehandling,
            brukerHarBesluttet = if (brukerHarIngenValidering) {
                false
            } else {
                brukerHarBesluttet
            },
            brukerHarKvalitetssikret = if (brukerHarIngenValidering) {
                false
            } else {
                brukerHarKvalitetssikret
            },
            resultatKode = resultatKode
        )
    }
}

private fun utledVisningAvKvalitetsikrerKort(
    avklaringsbehovene: FrivilligeAvklaringsbehov
): Boolean {
    if (avklaringsbehovene.skalTilbakeføresEtterKvalitetssikring()) {
        return true
    }
    if (harÅpentKvalitetssikringsAvklaringsbehov(avklaringsbehovene)) {
        return true
    }
    return false
}

private fun harÅpentKvalitetssikringsAvklaringsbehov(avklaringsbehovene: FrivilligeAvklaringsbehov): Boolean =
    avklaringsbehovene.hentBehovForDefinisjon(Definisjon.KVALITETSSIKRING)?.erÅpent() == true

private fun alleVilkår(vilkårResultat: Vilkårsresultat): List<VilkårDTO> {
    return vilkårResultat.alle().map { vilkår ->
        VilkårDTO(
            vilkår.type,
            perioder = vilkår.vilkårsperioder().map { vp ->
                VilkårsperiodeDTO(
                    vp.periode,
                    vp.utfall,
                    vp.manuellVurdering,
                    vp.begrunnelse,
                    vp.avslagsårsak,
                    vp.innvilgelsesårsak
                )
            },
            vurdertDato = vilkår.vurdertTidspunkt?.toLocalDate()
        )
    }
}

private fun behandling(behandlingRepository: BehandlingRepository, req: BehandlingReferanse): Behandling {
    return BehandlingReferanseService(behandlingRepository).behandling(req)
}

private fun avklaringsbehov(
    avklaringsbehovRepository: AvklaringsbehovRepository,
    behandlingId: BehandlingId
): Avklaringsbehovene {
    return avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
}

private fun vilkårResultat(
    vilkårsresultatRepository: VilkårsresultatRepository,
    behandlingId: BehandlingId
): Vilkårsresultat {
    return vilkårsresultatRepository.hent(behandlingId)
}

private fun erEtterBeslutterstegetHvisEksisterer(flyt: BehandlingFlyt, aktivtSteg: StegType): Boolean {
    return flyt.stegene().contains(StegType.FATTE_VEDTAK) && !flyt.erStegFør(
        aktivtSteg,
        StegType.FATTE_VEDTAK
    )
}

private fun hentUtRelevantVilkårForSteg(vilkårsresultat: Vilkårsresultat, stegType: StegType): VilkårDTO? {
    var vilkår: Vilkår? = null
    if (stegType == StegType.AVKLAR_SYKDOM) {
        vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.SYKDOMSVILKÅRET)
    }
    if (stegType == StegType.VURDER_ALDER) {
        vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.ALDERSVILKÅRET)
    }
    if (stegType == StegType.VURDER_BISTANDSBEHOV) {
        vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.BISTANDSVILKÅRET)
    }
    if (stegType == StegType.VURDER_LOVVALG) {
        vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.LOVVALG)
    }
    if (stegType == StegType.VURDER_MEDLEMSKAP) {
        vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.MEDLEMSKAP)
    }
    if (stegType == StegType.OVERGANG_ARBEID) {
        vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.OVERGANGARBEIDVILKÅRET)
    }
    if (stegType == StegType.OVERGANG_UFORE) {
        vilkår = vilkårsresultat.optionalVilkår(Vilkårtype.OVERGANGUFØREVILKÅRET)
    }
    if (vilkår == null) {
        return null
    }
    return VilkårDTO(
        vilkår.type,
        perioder = vilkår.vilkårsperioder().map { vp ->
            VilkårsperiodeDTO(
                vp.periode,
                vp.utfall,
                vp.manuellVurdering,
                vp.begrunnelse,
                vp.avslagsårsak,
                vp.innvilgelsesårsak
            )
        },
        vurdertDato = vilkår.vurdertTidspunkt?.toLocalDate()
    )
}