package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.BrevbestillingLøsningStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider

val BREV_SYSTEMBRUKER = Bruker("Brevløsning")

class BrevbestillingLøser(val connection: DBConnection) : AvklaringsbehovsLøser<BrevbestillingLøsning> {

    private val repositoryProvider = RepositoryProvider(connection)
    private val avklaringsbehovRepository = repositoryProvider.provide(AvklaringsbehovRepository::class)
    private val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)

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
            val avklaringsbehovene =
                avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId = kontekst.behandlingId())
            avklaringsbehovene.leggTil(listOf(Definisjon.SKRIV_BREV), behandling.aktivtSteg())
        }

        return LøsningsResultat("Oppdatert brevbestilling")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.BESTILL_BREV
    }
}
