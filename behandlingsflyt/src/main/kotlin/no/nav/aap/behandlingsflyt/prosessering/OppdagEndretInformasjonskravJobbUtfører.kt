package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.KanTriggeRevurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningInformasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreInformasjonskrav
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate

class OppdagEndretInformasjonskravJobbUtfører(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider,
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val låsRepository: TaSkriveLåsRepository,
    private val rettighetstypeService: RettighetstypeService,
    private val klokke: Clock = Clock.systemDefaultZone(),
) : JobbUtfører {
    private val log = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("team-logs")

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        utfør(sakId)
    }

    fun utfør(sakId: SakId) {
        val sakSkrivelås = låsRepository.låsSak(sakId)
        val relevanteInformasjonskrav: List<KanTriggeRevurdering> = listOf(
            //BarnService.konstruer(repositoryProvider, gatewayProvider), Vente på avklaring fra departementet
            SamordningYtelseVurderingInformasjonskrav.konstruer(repositoryProvider, gatewayProvider),
            UføreInformasjonskrav.konstruer(repositoryProvider, gatewayProvider),
            InstitusjonsoppholdInformasjonskrav.konstruer(repositoryProvider, gatewayProvider),
            PersonopplysningInformasjonskrav.konstruer(repositoryProvider, gatewayProvider),
        )
        val sisteBehandling = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sakId)
            ?: error("Fant ikke ytelsesbehandling for sak $sakId")

        if (sisteBehandling.status().erÅpen()) {
            log.info("Siste behandling for sak $sakId er åpen. Hopper over sjekk for endret informasjonskrav.")
        } else {
            val harNyligEllerFremtidigRett = rettighetstypeService.harRettInnenforPeriode(
                sisteBehandling.id, Periode(
                    LocalDate.now(klokke).minusWeeks(2),
                    Tid.MAKS
                )
            )
            if (!harNyligEllerFremtidigRett) {
                log.info("Sak $sakId har ikke nylig eller fremtidig rettighet. Hopper over sjekk for endret informasjonskrav.")
            } else {
                val vurderingsbehov = relevanteInformasjonskrav
                    .flatMap {
                        it.behovForRevurdering(sisteBehandling.id)
                            .also { behov -> if (behov.isNotEmpty()) log.info("Fant endringer i ${it.javaClass.simpleName}") }
                    }
                    .toSet().toList() // Fjern duplikater

                if (vurderingsbehov.isNotEmpty()) {
                    val revurdering = this.sakOgBehandlingService.finnEllerOpprettOrdinærBehandling(
                        sakId,
                        VurderingsbehovOgÅrsak(vurderingsbehov, ÅrsakTilOpprettelse.ENDRING_I_REGISTERDATA)
                    )
                    log.info("Fant endringer i $sakId med behov $vurderingsbehov. Behandling: ${revurdering.referanse}.")
                    secureLogger.info("" + vurderingsbehov)
                    prosesserBehandlingService.triggProsesserBehandling(
                        revurdering,
                        vurderingsbehov = vurderingsbehov.map { it.type },
                    )
                } else {
                    log.info("Lar være å opprette revurdering for sak $sakId med behov $vurderingsbehov da opplysningene er registrert fra før. ")
                }
            }
        }

        låsRepository.verifiserSkrivelås(sakSkrivelås)
    }


    companion object : ProvidersJobbSpesifikasjon {
        override val type = "flyt.informasjonskrav"
        override val navn = "Oppdag endringer i informasjonskrav"
        override val beskrivelse = "Oppdag endringer i informasjonskrav og opprett revurdering ved behov"

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider,
        ): OppdagEndretInformasjonskravJobbUtfører {
            return OppdagEndretInformasjonskravJobbUtfører(
                repositoryProvider = repositoryProvider,
                gatewayProvider = gatewayProvider,
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                låsRepository = repositoryProvider.provide(),
                rettighetstypeService = RettighetstypeService(repositoryProvider, gatewayProvider)
            )
        }

        fun konstruerMedKlokke(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider,
            klokke: Clock,
        ): OppdagEndretInformasjonskravJobbUtfører {
            return OppdagEndretInformasjonskravJobbUtfører(
                repositoryProvider = repositoryProvider,
                gatewayProvider = gatewayProvider,
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                låsRepository = repositoryProvider.provide(),
                rettighetstypeService = RettighetstypeService(repositoryProvider, gatewayProvider),
                klokke = klokke,
            )
        }
    }
}