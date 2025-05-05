package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.BrevbestillingLøsningStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository

val BREV_SYSTEMBRUKER = Bruker("Brevløsning")

class BrevbestillingLøser(val connection: DBConnection) :
    AvklaringsbehovsLøser<BrevbestillingLøsning> {

    private val repositoryProvider = RepositoryRegistry.provider(connection)
    private val sakRepository = repositoryProvider.provide<SakRepository>()
    private val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(
        connection, BehandlingHendelseServiceImpl(
            repositoryProvider.provide<FlytJobbRepository>(),
            repositoryProvider.provide<BrevbestillingRepository>(),
            SakService(sakRepository),
        )
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: BrevbestillingLøsning
    ): LøsningsResultat {

        require(kontekst.bruker == BREV_SYSTEMBRUKER) { "Bruker kan ikke løse brevbestilling. Ble forsøkt løst av ${kontekst.bruker}." }

        val brevbestillingRepository = repositoryProvider.provide<BrevbestillingRepository>()

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
            val brevbestilling = brevbestillingRepository
                .hent(BrevbestillingReferanse(løsning.oppdatertStatusForBestilling.bestillingReferanse))

            avklaringsbehovOrkestrator.opprettSkrivBrevAvklaringsbehov(
                behandlingId = kontekst.behandlingId(),
                typeBrev = brevbestilling.typeBrev
            )
        }

        return LøsningsResultat("Oppdatert brevbestilling")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.BESTILL_BREV
    }
}
