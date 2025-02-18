package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.BrevbestillingLøsningStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository

val BREV_SYSTEMBRUKER = Bruker("Brevløsning")

class BrevbestillingLøser(val connection: DBConnection) :
    AvklaringsbehovsLøser<BrevbestillingLøsning> {

    private val repositoryProvider = RepositoryProvider(connection)
    private val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
    private val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
    private val sakRepository = repositoryProvider.provide<SakRepository>()
    private val behandlingHendelseService = BehandlingHendelseServiceImpl(
        FlytJobbRepository(connection),
        SakService(sakRepository)
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: BrevbestillingLøsning
    ): LøsningsResultat {

        require(kontekst.bruker == BREV_SYSTEMBRUKER) { "Bruker kan ikke løse brevbestilling." }

        val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

        val status = when (løsning.oppdatertStatusForBestilling.status) {
            BrevbestillingLøsningStatus.KLAR_FOR_EDITERING -> Status.FORHÅNDSVISNING_KLAR
            BrevbestillingLøsningStatus.AUTOMATISK_FERDIGSTILT -> Status.FULLFØRT
        }

        brevbestillingRepository.oppdaterStatus(
            behandlingId = kontekst.behandlingId(),
            referanse = BrevbestillingReferanse(løsning.oppdatertStatusForBestilling.bestillingReferanse),
            status = status
        )

        if (status == Status.FORHÅNDSVISNING_KLAR) {
            val behandling = behandlingRepository.hent(kontekst.behandlingId())
            val brevbestilling =
                brevbestillingRepository.hent(BrevbestillingReferanse(løsning.oppdatertStatusForBestilling.bestillingReferanse))
            val avklaringsbehov = when (brevbestilling.typeBrev) {
                TypeBrev.VEDTAK_AVSLAG -> Definisjon.SKRIV_VEDTAK_AVSLAG_BREV
                TypeBrev.VEDTAK_INNVILGELSE -> Definisjon.SKRIV_VEDTAK_INNVILGELSE_BREV
                TypeBrev.VARSEL_OM_BESTILLING -> Definisjon.SKRIV_VARSEL_OM_BESTILLING_BREV
                TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT -> Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV
            }
            val avklaringsbehovene =
                avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId = kontekst.behandlingId())
            avklaringsbehovene.leggTil(listOf(avklaringsbehov), behandling.aktivtSteg())
            behandlingHendelseService.stoppet(behandling, avklaringsbehovene)
        }

        return LøsningsResultat("Oppdatert brevbestilling")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.BESTILL_BREV
    }
}
