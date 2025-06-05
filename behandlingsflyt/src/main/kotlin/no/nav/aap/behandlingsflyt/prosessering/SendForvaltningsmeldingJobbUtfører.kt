package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon

class SendForvaltningsmeldingJobbUtfører(
    private val brevbestillingService: BrevbestillingService
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val behandlingId = BehandlingId(input.behandlingId())
        val typeBrev = TypeBrev.FORVALTNINGSMELDING
        brevbestillingService.bestillV2(
            behandlingId,
            typeBrev,
            "typeBrev-${behandlingId.id}",
            ferdigstillAutomatisk = true
        )
    }

    companion object : ProviderJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
            return SendForvaltningsmeldingJobbUtfører(BrevbestillingService(repositoryProvider))
        }

        override val beskrivelse = "Sender forvaltningsmelding til bruker"
        override val navn = "Brev forvaltningsmelding"
        override val type = "brev.forvaltningsmelding"
    }
}