package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon
import org.slf4j.LoggerFactory


class StoppetHendelseJobbUtfører private constructor() : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val hendelse = input.payload<BehandlingFlytStoppetHendelse>()

        log.info("Varsler hendelse til OppgaveStyring. ${hendelse.saksnummer} :: ${hendelse.referanse.referanse}")
        GatewayProvider.provide<OppgavestyringGateway>().varsleHendelse(hendelse)
    }

    companion object : ProviderJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
            return StoppetHendelseJobbUtfører()
        }

        override val type = "flyt.hendelse"
        override val navn = "Oppgavestyrings hendelse"
        override val beskrivelse = "Produsere hendelse til oppgavestyring"
    }
}
