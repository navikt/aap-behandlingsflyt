package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.BREV_SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection

class BrevbestillingLøser(val connection: DBConnection) : AvklaringsbehovsLøser<BrevbestillingLøsning> {
    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: BrevbestillingLøsning
    ): LøsningsResultat {

        require(kontekst.bruker == BREV_SYSTEMBRUKER){ "${kontekst.bruker} kan ikke løse brevbestilling." }

        val brevbestillingService = BrevbestillingService.konstruer(connection = connection)

        brevbestillingService.oppdaterStatus(
            behandlingId = kontekst.kontekst.behandlingId,
            referanse = løsning.brevbestillingStatus.referanse,
            status = løsning.brevbestillingStatus.status
        )

        return LøsningsResultat("Oppdatert brevbestilling")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.BESTILL_BREV
    }
}
