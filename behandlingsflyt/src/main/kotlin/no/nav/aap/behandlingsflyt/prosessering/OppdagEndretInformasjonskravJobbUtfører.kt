package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.KanTriggeRevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreInformasjonskrav
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

class OppdagEndretInformasjonskravJobbUtfører(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider,
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val sakOgBehandlingService: SakOgBehandlingService
) : JobbUtfører {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val behandlingId = BehandlingId(input.behandlingId())
        utfør(sakId, behandlingId)
    }

    fun utfør(sakId: SakId, behandlingId: BehandlingId) {
        val relevanteInformasjonskrav: List<KanTriggeRevurdering> = listOf(
            //BarnService.konstruer(repositoryProvider, gatewayProvider), Vente på avklaring fra departementet
            SamordningYtelseVurderingInformasjonskrav.konstruer(repositoryProvider, gatewayProvider),
            TjenestePensjonInformasjonskrav.konstruer(repositoryProvider, gatewayProvider),
            UføreInformasjonskrav.konstruer(repositoryProvider, gatewayProvider),
            InstitusjonsoppholdInformasjonskrav.konstruer(repositoryProvider, gatewayProvider),
            PersonopplysningInformasjonskrav.konstruer(repositoryProvider, gatewayProvider),
        )


        val vurderingsbehov = relevanteInformasjonskrav
            .flatMap {
                it.behovForRevurdering(behandlingId)
                    .also { behov -> if (behov.isNotEmpty()) log.info("Fant endringer i ${it.javaClass.simpleName}") }
            }
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

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): OppdagEndretInformasjonskravJobbUtfører {
            return OppdagEndretInformasjonskravJobbUtfører(
                repositoryProvider,
                gatewayProvider,
                ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                SakOgBehandlingService(repositoryProvider, gatewayProvider)
            )
        }
    }

}