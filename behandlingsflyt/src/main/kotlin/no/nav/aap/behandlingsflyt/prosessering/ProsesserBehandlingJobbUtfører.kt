package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopiererImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.flyt.steg.internal.StegKonstruktørImpl
import no.nav.aap.behandlingsflyt.flyt.ventebehov.VentebehovEvaluererServiceImpl
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.periodisering.PerioderTilVurderingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory


class ProsesserBehandlingJobbUtfører(
    private val låsRepository: TaSkriveLåsRepository,
    private val kontroller: FlytOrkestrator
) : JobbUtfører {

private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val behandlingId = BehandlingId(input.behandlingId())
        val skrivelås = låsRepository.lås(sakId, behandlingId)

        val kontekst = kontroller.opprettKontekst(sakId, behandlingId)

        kontroller.forberedOgProsesserBehandling(kontekst)

        log.info("Prosesserer behandling for jobb ${input.type()} med behandlingId ${behandlingId}")

        låsRepository.verifiserSkrivelås(skrivelås)
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryRegistry.provider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val låsRepository = repositoryProvider.provide<TaSkriveLåsRepository>()
            val avklaringsbehovRepository =
                repositoryProvider.provide<AvklaringsbehovRepository>()
            return ProsesserBehandlingJobbUtfører(
                låsRepository,
                FlytOrkestrator(
                    stegKonstruktør = StegKonstruktørImpl(connection),
                    ventebehovEvaluererService = VentebehovEvaluererServiceImpl(connection),
                    behandlingRepository = behandlingRepository,
                    avklaringsbehovRepository = avklaringsbehovRepository,
                    informasjonskravGrunnlag = InformasjonskravGrunnlagImpl(repositoryProvider.provide(), connection),
                    sakRepository = sakRepository,
                    perioderTilVurderingService = PerioderTilVurderingService(
                        SakService(sakRepository),
                        behandlingRepository,
                    ),
                    sakOgBehandlingService = SakOgBehandlingService(
                        GrunnlagKopiererImpl(connection),
                        sakRepository,
                        behandlingRepository
                    ),
                    behandlingHendelseService = BehandlingHendelseServiceImpl(
                        repositoryProvider.provide<FlytJobbRepository>(),
                        repositoryProvider.provide<BrevbestillingRepository>(),
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
