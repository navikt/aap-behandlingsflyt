package no.nav.aap.behandlingsflyt.flyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.BehandlingTilstandValidator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingService
import no.nav.aap.behandlingsflyt.flyt.flate.visning.DynamiskStegGruppeVisningService
import no.nav.aap.behandlingsflyt.flyt.flate.visning.ProsesseringStatus
import no.nav.aap.behandlingsflyt.flyt.flate.visning.Visning
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbStatus
import no.nav.aap.motor.api.JobbInfoDto
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.RelevanteIdenter
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
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val resultatUtleder = ResultatUtleder(repositoryProvider, gatewayProvider)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val avklaringsbehovRepository =
                        repositoryProvider.provide<AvklaringsbehovRepository>()

                    var behandling = behandling(behandlingRepository, req)
                    val sak = sakRepository.hent(behandling.sakId)
                    val avklaringsbehovene = lazy { avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id) }
                    val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()
                    val gruppeVisningService = DynamiskStegGruppeVisningService(repositoryProvider)
                    val avbrytAktivitetspliktbehandlingService =
                        AvbrytAktivitetspliktbehandlingService(repositoryProvider)

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
                        avklaringsbehovene.value,
                        flyt, aktivtSteg
                    )
                    val vurdertStegPair =
                        utledVurdertGruppe(prosessering, aktivtSteg, flyt, avklaringsbehovene.value)
                    val alleAvklaringsbehov = alleAvklaringsbehovInkludertFrivillige.alle()
                    val resultatKode = when {
                        ((behandling.typeBehandling() == TypeBehandling.Revurdering) && (resultatUtleder.utledResultatRevurderingsBehandling(
                            behandling
                        ) == Resultat.AVBRUTT)) -> ResultatKode.AVBRUTT

                        ((behandling.typeBehandling() == TypeBehandling.Aktivitetsplikt || behandling.typeBehandling() == TypeBehandling.Aktivitetsplikt11_9) && (avbrytAktivitetspliktbehandlingService.behandlingErAvbrutt(
                            behandling.id
                        ))) -> ResultatKode.AVBRUTT

                        else -> null
                    }

                    LoggingKontekst(
                        repositoryProvider.provide(),
                        LogKontekst(referanse = BehandlingReferanse(req.referanse))
                    ).use {
                        val behandlingVersjon = behandling.versjon
                        log.info("Henter flyt med behandlingversjon: $behandlingVersjon")
                    }


                    BehandlingFlytOgTilstandDto(
                        flyt = stegGrupper.map { (gruppe, steg) ->
                            erFullført = erFullført && erStegGruppeFullført(gruppe, aktivtSteg, behandling)
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
                                                    avklaringsbehov = behov,
                                                    kravdato = sak.rettighetsperiode.fom,
                                                )
                                            },
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

        route("/{referanse}/flyt/prosessering") {
            authorizedGet<BehandlingReferanse, Prosessering>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val prosessering = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()

                    val behandlingId = BehandlingReferanseService(behandlingRepository).behandling(req).id
                    val avklaringsbehovene = lazy { avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId) }

                    val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()

                    val jobber = flytJobbRepository.hentJobberForBehandling(behandlingId.toLong())
                        .filter { it.type() == ProsesserBehandlingJobbUtfører.type }

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
                }
                respond(prosessering)
            }
        }

        route("/{referanse}/sett-på-vent") {
            authorizedPost<BehandlingReferanse, BehandlingResultatDto, SettPåVentRequest>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    operasjon = Operasjon.SAKSBEHANDLE,
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    påkrevdRolle = Definisjon.MANUELT_SATT_PÅ_VENT.løsesAv
                )
            ) { request, body ->
                val (åpentAvklaringsbehov, relevanteIdenter) = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(request)
                    val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
                    val åpentAvklaringsbehov = avklaringsbehovene.åpne().filterNot { it.erVentepunkt() }
                        .sortedWith(behandling.flyt().avklaringsbehovComparator).first().definisjon
                    val relevanteIdenter = runBlocking {
                        relevanteIdenterForBehandlingResolver(
                            repositoryRegistry,
                            dataSource
                        ).resolve(request.referanse.toString())
                    }
                    Pair(åpentAvklaringsbehov, relevanteIdenter)
                }
                sjekkTilgangTilSettPåVent(
                    åpentAvklaringsbehov = åpentAvklaringsbehov,
                    tilgangGateway = tilgangGateway,
                    token = token(),
                    behandlingsreferanse = request.referanse,
                    relevanteIdenter = relevanteIdenter,
                )

                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    LoggingKontekst(
                        repositoryProvider.provide(),
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

                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(request)
                    val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

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

private fun erStegGruppeFullført(
    gruppe: StegGruppe,
    aktivtSteg: StegType,
    behandling: Behandling
): Boolean {
    if (gruppe == aktivtSteg.gruppe) {
        return behandling.aktivtStegTilstand().status() == StegStatus.AVSLUTTER
    }
    return true
}

private suspend fun sjekkTilgangTilSettPåVent(
    åpentAvklaringsbehov: Definisjon,
    tilgangGateway: TilgangGateway,
    token: OidcToken,
    behandlingsreferanse: UUID,
    relevanteIdenter: RelevanteIdenter
) {
    val harTilgang =
        tilgangGateway.sjekkTilgangTilBehandling(
            behandlingsreferanse,
            åpentAvklaringsbehov,
            token,
            relevanteIdenter
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

            val stegType = requireNotNull(flyt.skalTilStegForBehov(relevanteBehov)) {
                "Kunne ikke utlede skalTilStegForBehov med relevante behov: $relevanteBehov"
            }

            return stegType.gruppe to stegType
        } else if (aktivtSteg == StegType.FATTE_VEDTAK) {
            val relevanteBehov = avklaringsbehovene.alle().filter { it.erTotrinn() }
                .filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
                .filter { avklaringsbehov -> !avklaringsbehov.erTotrinnsVurdert() }

            val stegType = requireNotNull(flyt.skalTilStegForBehov(relevanteBehov)) {
                "Kunne ikke utlede skalTilStegForBehov med relevante behov: $relevanteBehov"
            }

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

private fun utledStatus(jobber: List<JobbInput>, avklaringsbehovene: Lazy<Avklaringsbehovene>): ProsesseringStatus {
    if (jobber.isEmpty()) {
        if (avklaringsbehovene.value.harÅpentBrevVentebehov()) {
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
        alleAvklaringsbehovInkludertFrivillige.hentBehovForDefinisjon(Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV)
            ?.erÅpent() == true || alleAvklaringsbehovInkludertFrivillige.hentBehovForDefinisjon(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV)
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

private fun behandling(behandlingRepository: BehandlingRepository, req: BehandlingReferanse): Behandling {
    return BehandlingReferanseService(behandlingRepository).behandling(req)
}

private fun erEtterBeslutterstegetHvisEksisterer(flyt: BehandlingFlyt, aktivtSteg: StegType): Boolean {
    return flyt.stegene().contains(StegType.FATTE_VEDTAK) && !flyt.erStegFør(
        aktivtSteg,
        StegType.FATTE_VEDTAK
    )
}

