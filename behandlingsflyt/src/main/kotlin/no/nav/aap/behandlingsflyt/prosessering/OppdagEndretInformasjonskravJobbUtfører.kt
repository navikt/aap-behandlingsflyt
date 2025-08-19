package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.KanTriggeRevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon

class OppdagEndretInformasjonskravJobbUtfører(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val relevanteInformasjonskrav: List<KanTriggeRevurdering> = listOf(
            BarnService.konstruer(repositoryProvider, gatewayProvider),
//            InformasjonskravNavn.SAMORDNING_YTELSE, 
//            InformasjonskravNavn.SAMORDNING_TJENESTEPENSJON,
//            InformasjonskravNavn.UFØRE,
//            InformasjonskravNavn.INSTITUSJONSOPPHOLD,
//            InformasjonskravNavn.PERSONOPPLYSNING
        )

        val behandlingId = BehandlingId(input.behandlingId())

        val vurderingsbehov = relevanteInformasjonskrav.flatMap { it.behovForRevurdering(behandlingId) }

        if (vurderingsbehov.isNotEmpty()) {
            val sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider)
            val revurdering = sakOgBehandlingService.finnEllerOpprettBehandling(
                SakId(input.sakId()),
                vurderingsbehov,
                ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA
            )
            ProsesserBehandlingService(repositoryProvider, gatewayProvider).triggProsesserBehandling(
                revurdering,
                emptyList() // TODO: Se om vi bør legge ved triggere
            )
        }
    }


    companion object : ProvidersJobbSpesifikasjon {
        override val type = "flyt.informasjonskrav"
        override val navn = "Oppdag endringer i informasjonskrav"
        override val beskrivelse = "Oppdag endringer i informasjonskrav og opprett revurdering ved behov"

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OppdagEndretInformasjonskravJobbUtfører(repositoryProvider, gatewayProvider)
        }
    }

}