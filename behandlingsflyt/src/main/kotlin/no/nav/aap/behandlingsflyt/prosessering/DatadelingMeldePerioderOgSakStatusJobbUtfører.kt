package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon

class DatadelingMeldePerioderOgSakStatusJobbUtfører(
    private val apiInternGateway: ApiInternGateway,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val hendelse = input.payload<BehandlingFlytStoppetHendelse>()
        val behandling = behandlingRepository.hent(hendelse.referanse)
        val sak = sakRepository.hent(behandling.sakId)
        val personIdent = sak.person.aktivIdent().identifikator

        val perioder = meldeperiodeRepository.hent(behandling.id)
        // TODO: slå sammen til ett endepunkt i apiinterngateway
        apiInternGateway.sendPerioder(personIdent, perioder)
        apiInternGateway.sendSakStatus(
            sak.person.aktivIdent().identifikator,
            SakStatus.fromKelvin(sak.saksnummer.toString(), sak.status(), sak.rettighetsperiode)
        )
    }

    companion object : ProvidersJobbSpesifikasjon {
        override val beskrivelse = "Sender meldekort perioder og vedtaksdata til api-intern."
        override val navn = "DatadelingMeldePerioderJobbUtfører"
        override val type = "flyt.DatadelingMeldePerioder"

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return DatadelingMeldePerioderOgSakStatusJobbUtfører(
                apiInternGateway = gatewayProvider.provide(),
                behandlingRepository = repositoryProvider.provide(),
                sakRepository = repositoryProvider.provide(),
                meldeperiodeRepository = repositoryProvider.provide()
            )
        }
    }
}