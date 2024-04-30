package no.nav.aap.behandlingsflyt.flyt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.avklaringsbehov.FrivilligeAvklaringsbehov
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.motor.OppgaveInput
import no.nav.aap.motor.OppgaveRepository
import no.nav.aap.motor.OppgaveStatus
import no.nav.aap.verdityper.flyt.StegGruppe
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

fun NormalOpenAPIRoute.flytApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/flyt") {
            get<BehandlingReferanse, BehandlingFlytOgTilstandDto> { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val behandling = behandling(connection, req)
                    val oppgaveRepository = OppgaveRepository(connection)
                    val flyt = utledType(behandling.typeBehandling()).flyt()

                    val stegGrupper: Map<StegGruppe, List<StegType>> =
                        flyt.stegene().groupBy { steg -> steg.gruppe }

                    val aktivtSteg = behandling.aktivtSteg()
                    var erFullført = true
                    val alleAvklaringsbehovInkludertFrivillige = FrivilligeAvklaringsbehov(
                        avklaringsbehov(
                            connection,
                            behandling.id
                        ),
                        flyt, aktivtSteg
                    )
                    val oppgaver = oppgaveRepository.hentOppgaveForBehandling(behandling.id)
                    val prosessering =
                        Prosessering(utledStatus(oppgaver), oppgaver.map { OppgaveDto(it.type(), it.status()) })
                    BehandlingFlytOgTilstandDto(
                        flyt = stegGrupper.map { (gruppe, steg) ->
                            erFullført = erFullført && gruppe != aktivtSteg.gruppe
                            FlytGruppe(
                                stegGruppe = gruppe,
                                erFullført = erFullført,
                                steg = steg.map { stegType ->
                                    FlytSteg(
                                        stegType = stegType,
                                        avklaringsbehov = alleAvklaringsbehovInkludertFrivillige.alle()
                                            .filter { avklaringsbehov -> avklaringsbehov.skalLøsesISteg(stegType) }
                                            .map { behov ->
                                                AvklaringsbehovDTO(
                                                    behov.definisjon,
                                                    behov.status(),
                                                    emptyList()
                                                )
                                            },
                                        vilkårDTO = hentUtRelevantVilkårForSteg(
                                            vilkårResultat(connection, behandling.id),
                                            stegType
                                        )
                                    )
                                }
                            )
                        },
                        aktivtSteg = aktivtSteg,
                        aktivGruppe = aktivtSteg.gruppe,
                        behandlingVersjon = behandling.versjon,
                        prosessering = prosessering,
                        visning = utledVisning(aktivtSteg, flyt, alleAvklaringsbehovInkludertFrivillige, prosessering.status)
                    )
                }
                respond(dto)
            }
        }
        route("/{referanse}/resultat") {
            get<BehandlingReferanse, BehandlingResultatDto> { req ->
                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val behandling = behandling(connection, req)

                    val vilkårResultat = vilkårResultat(connection, behandling.id)

                    BehandlingResultatDto(alleVilkår(vilkårResultat))
                }
                respond(dto)
            }
        }
    }
}

private fun utledStatus(oppgaver: List<OppgaveInput>): ProsesseringStatus {
    if (oppgaver.isEmpty()) {
        return ProsesseringStatus.FERDIG
    }
    if (oppgaver.any { it.status() == OppgaveStatus.FEILET }) {
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
    val beslutterReadOnly = aktivtSteg != StegType.FATTE_VEDTAK
    val saksbehandlerReadOnly = !flyt.erStegFør(aktivtSteg, StegType.FATTE_VEDTAK)
    val visBeslutterKort =
        !beslutterReadOnly || (!saksbehandlerReadOnly && alleAvklaringsbehovInkludertFrivillige.harVærtSendtTilbakeFraBeslutterTidligere())

    return Visning(
        saksbehandlerReadOnly = !jobber && saksbehandlerReadOnly,
        beslutterReadOnly = !jobber && beslutterReadOnly,
        visBeslutterKort = visBeslutterKort
    )

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

private fun behandling(connection: DBConnection, req: BehandlingReferanse): Behandling {
    return BehandlingReferanseService(connection).behandling(req)
}

private fun avklaringsbehov(connection: DBConnection, behandlingId: BehandlingId): Avklaringsbehovene {
    return AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandlingId)
}

private fun vilkårResultat(connection: DBConnection, behandlingId: BehandlingId): Vilkårsresultat {
    return VilkårsresultatRepository(connection).hent(behandlingId)
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
