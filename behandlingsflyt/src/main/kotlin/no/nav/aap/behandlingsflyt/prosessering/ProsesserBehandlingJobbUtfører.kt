package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.flyt.steg.internal.StegKonstruktørImpl
import no.nav.aap.behandlingsflyt.flyt.ventebehov.VentebehovEvaluererServiceImpl
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.periodisering.PerioderTilVurderingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingFlytRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakFlytRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.repository.RepositoryFactory

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
            val repositoryFactory = RepositoryFactory(connection)
            val behandlingRepository = repositoryFactory.create(BehandlingRepository::class)
            val behandlingFlytRepository = repositoryFactory.create(BehandlingFlytRepository::class)
            val sakRepository = repositoryFactory.create(SakRepository::class)
            val sakFlytRepository = repositoryFactory.create(SakFlytRepository::class)
            return ProsesserBehandlingJobbUtfører(
                TaSkriveLåsRepositoryImpl(connection),
                FlytOrkestrator(
                    stegKonstruktør = StegKonstruktørImpl(connection),
                    ventebehovEvaluererService = VentebehovEvaluererServiceImpl(connection),
                    behandlingRepository = behandlingRepository,
                    behandlingFlytRepository = behandlingFlytRepository,
                    avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection),
                    informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(connection),
                    sakRepository = sakFlytRepository,
                    perioderTilVurderingService = PerioderTilVurderingService(
                        SakService(sakRepository),
                        behandlingRepository
                    ),
                    behandlingHendelseService = BehandlingHendelseServiceImpl(
                        FlytJobbRepository(connection),
                        SakService(sakRepository)
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
