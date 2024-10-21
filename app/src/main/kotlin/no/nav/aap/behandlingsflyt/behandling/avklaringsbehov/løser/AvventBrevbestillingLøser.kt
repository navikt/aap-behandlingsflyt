package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvventBrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection

class AvventBrevbestillingLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvventBrevbestillingLøsning> {
    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvventBrevbestillingLøsning
    ): LøsningsResultat {
        val brevbestillingService = BrevbestillingService.konstruer(connection = connection)

        brevbestillingService.oppdaterStatus(
            referanse = løsning.brevbestillingStatus.referanse,
            status = løsning.brevbestillingStatus.status
        )

        return LøsningsResultat("Oppdatert brevbestilling")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.UTFØR_BREV_BESTILLING
    }
}