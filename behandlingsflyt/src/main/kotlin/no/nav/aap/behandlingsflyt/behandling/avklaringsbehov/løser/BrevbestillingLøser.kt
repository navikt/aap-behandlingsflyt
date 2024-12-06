package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.BrevbestillingLøsningStatus
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.auth.Bruker

val BREV_SYSTEMBRUKER = Bruker("Brevløsning")

class BrevbestillingLøser(val connection: DBConnection) : AvklaringsbehovsLøser<BrevbestillingLøsning> {
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

        return LøsningsResultat("Oppdatert brevbestilling")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.BESTILL_BREV
    }
}
