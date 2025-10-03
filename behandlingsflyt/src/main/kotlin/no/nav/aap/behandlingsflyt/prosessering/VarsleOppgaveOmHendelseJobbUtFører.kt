package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory


class VarsleOppgaveOmHendelseJobbUtFører private constructor(
    private val oppgavestyringGateway: OppgavestyringGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val hendelse = input.payload<BehandlingFlytStoppetHendelse>()

        log.info("Varsler hendelse til OppgaveStyring. ${hendelse.saksnummer} :: ${hendelse.referanse.referanse}")
        oppgavestyringGateway.varsleHendelse(hendelse)
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return VarsleOppgaveOmHendelseJobbUtFører(
                gatewayProvider.provide(),
            )
        }

        override val type = "flyt.hendelse"
        override val navn = "Oppgavestyringshendelse"
        override val beskrivelse = "Produsere hendelse til oppgavestyring"
    }
}
