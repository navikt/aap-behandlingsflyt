package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
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
        val triggere =
            input.optionalParameter("trigger")?.let { DefaultJsonMapper.fromJson<List<Vurderingsbehov>>(it) }
                .orEmpty()
        val kontekst = kontroller.opprettKontekst(sakId, behandlingId)

        kontroller.forberedOgProsesserBehandling(kontekst, triggere)

        log.info("Prosesserer behandling for jobb ${input.type()} med behandlingId $behandlingId")

        låsRepository.verifiserSkrivelås(skrivelås)
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return ProsesserBehandlingJobbUtfører(
                låsRepository = repositoryProvider.provide(),
                kontroller = FlytOrkestrator(repositoryProvider, gatewayProvider)
            )
        }

        override val type = "flyt.prosesserBehandling"
        override val navn = "Prosesser behandling"
        override val beskrivelse = "Ansvarlig for å drive prosessen på en gitt behandling"
    }
}
