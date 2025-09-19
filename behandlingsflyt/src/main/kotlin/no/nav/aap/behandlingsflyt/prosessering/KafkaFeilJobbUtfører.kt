package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import java.util.UUID

private const val MELDING_KILDE = "meldingkilde"

enum class Meldingkilde {
    KABAL
}

// NB: Disse jobbene vil ikke kjøre i rekkefølge
class KafkaFeilJobbUtfører(
    private val mottattHendelseService: MottattHendelseService,
    private val behandlingRepository: BehandlingRepository
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val (meldingkilde, melding) = input.payload<Pair<Meldingkilde, String>>()

        when (meldingkilde) {
            Meldingkilde.KABAL -> {
                val hendelse = DefaultJsonMapper.fromJson<KabalHendelseKafkaMelding>(melding)
                val saksnummer =
                    behandlingRepository.finnSaksnummer(BehandlingReferanse(UUID.fromString(hendelse.kildeReferanse)))
                mottattHendelseService.registrerMottattHendelse(hendelse.tilInnsending(saksnummer))
            }
        }
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return KafkaFeilJobbUtfører(
                mottattHendelseService = MottattHendelseService(repositoryProvider),
                behandlingRepository = repositoryProvider.provide()
            )
        }

        fun nyJobb(meldingkilde: Meldingkilde, melding: String): JobbInput =
            JobbInput(
                KafkaFeilJobbUtfører
            ).apply {
                medParameter(MELDING_KILDE, meldingkilde.name)
                medPayload(melding)
            }


        override val type = "hendelse.kafka.feilhåndterer"
        override val navn = "Kafka feilhåndterer"
        override val beskrivelse = "Håndterer kafkameldinger som feiler"
    }
}
