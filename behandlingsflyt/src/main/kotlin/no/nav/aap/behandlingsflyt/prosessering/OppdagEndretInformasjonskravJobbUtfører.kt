package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.KanTriggeRevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon

class OppdagEndretInformasjonskravJobbUtfører(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider,
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val sakOgBehandlingService: SakOgBehandlingService
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val behandlingId = BehandlingId(input.behandlingId())
        utfør(sakId, behandlingId)
    }

    fun utfør(sakId: SakId, behandlingId: BehandlingId) {
        val relevanteInformasjonskrav: List<KanTriggeRevurdering> = listOf(
            //BarnService.konstruer(repositoryProvider, gatewayProvider), Vente på avklaring fra departementet
            SamordningYtelseVurderingService.konstruer(repositoryProvider, gatewayProvider),
            TjenestePensjonService.konstruer(repositoryProvider, gatewayProvider),
            UføreService.konstruer(repositoryProvider, gatewayProvider),
            InstitusjonsoppholdService.konstruer(repositoryProvider, gatewayProvider),
            PersonopplysningService.konstruer(repositoryProvider, gatewayProvider),
        )


        val vurderingsbehov = relevanteInformasjonskrav
            .flatMap { it.behovForRevurdering(behandlingId) }
            .toSet().toList() // Fjern duplikater

        if (vurderingsbehov.isNotEmpty()) {
            val revurdering = this.sakOgBehandlingService.finnEllerOpprettOrdinærBehandling(
                sakId,
                VurderingsbehovOgÅrsak(vurderingsbehov, ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA)
            )
            prosesserBehandlingService.triggProsesserBehandling(
                revurdering,
                emptyList() // TODO: Se om vi bør legge ved triggere
            )
        }
    }


    companion object : ProvidersJobbSpesifikasjon {
        override val type = "flyt.informasjonskrav"
        override val navn = "Oppdag endringer i informasjonskrav"
        override val beskrivelse = "Oppdag endringer i informasjonskrav og opprett revurdering ved behov"

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): OppdagEndretInformasjonskravJobbUtfører {
            return OppdagEndretInformasjonskravJobbUtfører(
                repositoryProvider,
                gatewayProvider,
                ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                SakOgBehandlingService(repositoryProvider, gatewayProvider)
            )
        }
    }

}