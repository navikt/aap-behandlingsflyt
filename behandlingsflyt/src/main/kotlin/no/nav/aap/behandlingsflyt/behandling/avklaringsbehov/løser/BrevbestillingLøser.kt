package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.BrevbestillingLøsningStatus
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider

val BREV_SYSTEMBRUKER = Bruker("Brevløsning")

class BrevbestillingLøser(
    private val avklaringsbehovOrkestrator: AvklaringsbehovOrkestrator,
    private val brevbestillingRepository: BrevbestillingRepository,
) : AvklaringsbehovsLøser<BrevbestillingLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(repositoryProvider),
        brevbestillingRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: BrevbestillingLøsning
    ): LøsningsResultat {

        require(kontekst.bruker == BREV_SYSTEMBRUKER) { "Bruker kan ikke løse brevbestilling. Ble forsøkt løst av ${kontekst.bruker}." }

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
