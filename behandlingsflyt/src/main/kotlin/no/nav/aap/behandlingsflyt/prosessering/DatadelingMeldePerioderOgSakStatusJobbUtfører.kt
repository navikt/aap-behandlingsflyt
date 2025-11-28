package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon

class DatadelingMeldePerioderOgSakStatusJobbUtfører(
    private val apiInternGateway: ApiInternGateway,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository,
    private val underveisRepository: UnderveisRepository,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val hendelse = input.payload<BehandlingFlytStoppetHendelse>()
        val behandling = behandlingRepository.hent(hendelse.referanse)
        val sak = sakRepository.hent(behandling.sakId)
        val personIdent = sak.person.aktivIdent()

        val aktuellPeriode = underveisRepository.hentHvisEksisterer(behandling.id)?.somTidslinje()?.helePerioden()
            ?: sak.rettighetsperiodeEttÅrFraStartDato()
        val meldeperioder = meldeperiodeRepository.hentMeldeperioder(behandling.id, aktuellPeriode)
        apiInternGateway.sendPerioder(personIdent.identifikator, meldeperioder)

        apiInternGateway.sendSakStatus(
            personIdent.identifikator,
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
                meldeperiodeRepository = repositoryProvider.provide(),
                underveisRepository = repositoryProvider.provide(),
            )
        }
    }
}