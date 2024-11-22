package no.nav.aap.behandlingsflyt.server.prosessering

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.flyt.steg.internal.StegKonstruktørImpl
import no.nav.aap.behandlingsflyt.flyt.ventebehov.VentebehovEvaluererServiceImpl
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.periodisering.PerioderTilVurderingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId

class ProsesserBehandlingJobbUtfører(
    private val låsRepository: TaSkriveLåsRepository,
    private val kontroller: FlytOrkestrator
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val behandlingId = BehandlingId(input.behandlingId())
        val skrivelås = låsRepository.lås(sakId, behandlingId)

        val kontekst = kontroller.opprettKontekst(sakId, behandlingId)

        kontroller.forberedBehandling(kontekst)
        kontroller.prosesserBehandling(kontekst)

        låsRepository.verifiserSkrivelås(skrivelås)
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            return ProsesserBehandlingJobbUtfører(
                TaSkriveLåsRepository(connection),
                FlytOrkestrator(
                    stegKonstruktør = StegKonstruktørImpl(connection),
                    ventebehovEvaluererService = VentebehovEvaluererServiceImpl(connection),
                    behandlingRepository = behandlingRepository,
                    behandlingFlytRepository = behandlingRepository,
                    avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection),
                    informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(connection),
                    sakRepository = SakRepositoryImpl(connection),
                    perioderTilVurderingService = PerioderTilVurderingService(
                        SakService(SakRepositoryImpl(connection)),
                        BehandlingRepositoryImpl(connection)
                    ),
                    behandlingHendelseService = BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection),
                        SakService(SakRepositoryImpl(connection))
                    ),
                )
            )
        }

        override fun type(): String {
            return "flyt.prosesserBehandling"
        }

        override fun navn(): String {
            return "Prosesser behandling"
        }

        override fun beskrivelse(): String {
            return "Ansvarlig for å drive prosessen på en gitt behandling"
        }
    }
}
