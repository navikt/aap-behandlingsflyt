package no.nav.aap.behandlingsflyt.flyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.BehandlingTilstandValidator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.flate.VilkårDTO
import no.nav.aap.behandlingsflyt.flyt.flate.VilkårsperiodeDTO
import no.nav.aap.behandlingsflyt.flyt.flate.visning.DynamiskStegGruppeVisningService
import no.nav.aap.behandlingsflyt.flyt.flate.visning.ProsesseringStatus
import no.nav.aap.behandlingsflyt.flyt.flate.visning.Visning
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbStatus
import no.nav.aap.motor.api.JobbInfoDto
import org.slf4j.MDC
import javax.sql.DataSource

fun NormalOpenAPIRoute.flytApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/flyt") {
            get<BehandlingReferanse, BehandlingFlytOgTilstandDto> { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                    val vilkårsresultatRepository = repositoryProvider.provide(VilkårsresultatRepository::class)
                    val avklaringsbehovRepository = repositoryProvider.provide(AvklaringsbehovRepository::class)

                    var behandling = behandling(behandlingRepository, req)
                    val flytJobbRepository = FlytJobbRepository(connection)
                    val gruppeVisningService = DynamiskStegGruppeVisningService(connection)

                    val jobber = flytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
                    val prosessering =
                        Prosessering(
                            utledStatus(jobber),
                            jobber.map {
                                JobbInfoDto(
                                    id = it.jobbId(),
                                    type = it.type(),
                                    status = it.status(),
                                    planlagtKjøretidspunkt = it.nesteKjøring(),
                                    metadata = mapOf(),
                                    antallFeilendeForsøk = it.antallRetriesForsøkt(),
                                    feilmelding = hentFeilmeldingHvisBehov(
                                        it.status(),
                                        it.jobbId(),
                                        flytJobbRepository
                                    ),
                                    beskrivelse = it.beskrivelse(),
                                    navn = it.navn()
                                )
                            })
                    // Henter denne ut etter status er utledet for å være sikker på at dataene er i rett tilstand
                    behandling = behandling(behandlingRepository, req)
                    val flyt = utledType(behandling.typeBehandling()).flyt()

                    val stegGrupper: Map<StegGruppe, List<StegType>> =
                        flyt.stegene().groupBy { steg -> steg.gruppe }
                    val aktivtSteg = behandling.aktivtSteg()
                    var erFullført = true
                    val avklaringsbehovene = avklaringsbehov(
                        avklaringsbehovRepository,
                        behandling.id
                    )
                    val alleAvklaringsbehovInkludertFrivillige = FrivilligeAvklaringsbehov(
                        avklaringsbehovene,
                        flyt, aktivtSteg
                    )
                    val vurdertStegPair = utledVurdertGruppe(prosessering, aktivtSteg, flyt, avklaringsbehovene)
                    val vilkårsresultat = vilkårResultat(vilkårsresultatRepository, behandling.id)
                    val alleAvklaringsbehov = alleAvklaringsbehovInkludertFrivillige.alle()
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
                                            .filter { avklaringsbehov -> avklaringsbehov.skalLøsesISteg(stegType) }
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
                        aktivGruppe = aktivtSteg.gruppe,
                        vurdertSteg = vurdertStegPair?.second,
                        vurdertGruppe = vurdertStegPair?.first,
                        behandlingVersjon = behandling.versjon,
                        prosessering = prosessering,
                        visning = utledVisning(
                            aktivtSteg = aktivtSteg,
                            flyt = flyt,
                            alleAvklaringsbehovInkludertFrivillige = alleAvklaringsbehovInkludertFrivillige,
                            status = prosessering.status
                        )
                    )
                }
                respond(dto)
            }
        }
        route("/{referanse}/resultat") {
            get<BehandlingReferanse, BehandlingResultatDto> { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                    val vilkårsresultatRepository = repositoryProvider.provide(VilkårsresultatRepository::class)

                    val behandling = behandling(behandlingRepository, req)

                    val vilkårResultat = vilkårResultat(vilkårsresultatRepository, behandling.id)

                    BehandlingResultatDto(alleVilkår(vilkårResultat))
                }
                respond(dto)
            }
        }
        route("/{referanse}/sett-på-vent") {
            post<BehandlingReferanse, BehandlingResultatDto, SettPåVentRequest> { request, body ->
                dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val taSkriveLåsRepository = repositoryProvider.provide(TaSkriveLåsRepository::class)
                    val lås = taSkriveLåsRepository.lås(request.referanse)
                    val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                    val sakRepository = repositoryProvider.provide(SakRepository::class)
                    BehandlingTilstandValidator(
                        BehandlingReferanseService(behandlingRepository),
                        FlytJobbRepository(connection)
                    ).validerTilstand(
                        request,
                        body.behandlingVersjon
                    )

                    MDC.putCloseable("sakId", lås.sakSkrivelås.id.toString()).use {
                        MDC.putCloseable("behandlingId", lås.behandlingSkrivelås.id.toString()).use {
                            AvklaringsbehovOrkestrator(
                                connection,
                                BehandlingHendelseServiceImpl(
                                    FlytJobbRepository(connection),
                                    SakService(sakRepository)
                                )
                            ).settBehandlingPåVent(
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
                }
                respondWithStatus(HttpStatusCode.NoContent)
            }
        }
        route("/{referanse}/vente-informasjon") {
            get<BehandlingReferanse, Venteinformasjon> { request ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                    val avklaringsbehovRepository = repositoryProvider.provide(AvklaringsbehovRepository::class)

                    val behandling = behandling(behandlingRepository, request)
                    val avklaringsbehovene = avklaringsbehov(avklaringsbehovRepository, behandling.id)

                    val ventepunkter = avklaringsbehovene.hentÅpneVentebehov()
                    if (avklaringsbehovene.erSattPåVent()) {
                        val avklaringsbehov = ventepunkter.first()
                        Venteinformasjon(
                            avklaringsbehov.frist(),
                            avklaringsbehov.begrunnelse(),
                            avklaringsbehov.grunn()
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

private fun utledStatus(oppgaver: List<JobbInput>): ProsesseringStatus {
    if (oppgaver.isEmpty()) {
        return ProsesseringStatus.FERDIG
    }
    if (oppgaver.any { it.status() == JobbStatus.FEILET }) {
        return ProsesseringStatus.FEILET
    }
    return ProsesseringStatus.JOBBER
}

private fun utledVisning(
    aktivtSteg: StegType,
    flyt: BehandlingFlyt,
    alleAvklaringsbehovInkludertFrivillige: FrivilligeAvklaringsbehov,
    status: ProsesseringStatus
): Visning {
    val jobber = status in listOf(ProsesseringStatus.JOBBER, ProsesseringStatus.FEILET)
    val påVent = alleAvklaringsbehovInkludertFrivillige.erSattPåVent()
    val beslutterReadOnly = aktivtSteg != StegType.FATTE_VEDTAK
    val erTilKvalitetssikring =
        alleAvklaringsbehovInkludertFrivillige.hentBehovForDefinisjon(Definisjon.KVALITETSSIKRING)?.erÅpent() == true
    val saksbehandlerReadOnly = erTilKvalitetssikring || !flyt.erStegFør(aktivtSteg, StegType.FATTE_VEDTAK)
    val visBeslutterKort =
        !beslutterReadOnly || (!saksbehandlerReadOnly && alleAvklaringsbehovInkludertFrivillige.harVærtSendtTilbakeFraBeslutterTidligere())
    val visKvalitetssikringKort = utledVisningAvKvalitetsikrerKort(alleAvklaringsbehovInkludertFrivillige)
    val kvalitetssikringReadOnly = visKvalitetssikringKort && flyt.erStegFør(aktivtSteg, StegType.KVALITETSSIKRING)

    if (jobber) {
        return Visning(
            saksbehandlerReadOnly = true,
            beslutterReadOnly = true,
            kvalitetssikringReadOnly = true,
            visBeslutterKort = visBeslutterKort,
            visKvalitetssikringKort = visKvalitetssikringKort,
            visVentekort = påVent
        )
    } else {
        return Visning(
            saksbehandlerReadOnly = påVent || saksbehandlerReadOnly,
            beslutterReadOnly = påVent || beslutterReadOnly,
            kvalitetssikringReadOnly = påVent || kvalitetssikringReadOnly,
            visBeslutterKort = visBeslutterKort,
            visKvalitetssikringKort = visKvalitetssikringKort,
            visVentekort = påVent
        )
    }
}

private fun utledVisningAvKvalitetsikrerKort(
    avklaringsbehovene: FrivilligeAvklaringsbehov
): Boolean {
    if (avklaringsbehovene.skalTilbakeføresEtterKvalitetssikring()) {
        return true
    }
    if (avklaringsbehovene.hentBehovForDefinisjon(Definisjon.KVALITETSSIKRING)?.erÅpent() == true) {
        return true
    }
    return false
}

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
            })
    }
}

private fun behandling(behandlingRepository: BehandlingRepository, req: BehandlingReferanse): Behandling {
    return BehandlingReferanseService(behandlingRepository).behandling(req)
}

private fun avklaringsbehov(avklaringsbehovRepository: AvklaringsbehovRepository, behandlingId: BehandlingId): Avklaringsbehovene {
    return avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
}

private fun vilkårResultat(vilkårsresultatRepository: VilkårsresultatRepository, behandlingId: BehandlingId): Vilkårsresultat {
    return vilkårsresultatRepository.hent(behandlingId)
}

private fun hentUtRelevantVilkårForSteg(vilkårsresultat: Vilkårsresultat, stegType: StegType): VilkårDTO? {
    var vilkår: Vilkår? = null
    if (stegType == StegType.AVKLAR_SYKDOM) {
        vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
    }
    if (stegType == StegType.VURDER_ALDER) {
        vilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)
    }
    if (stegType == StegType.VURDER_BISTANDSBEHOV) {
        vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
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
        })
}
