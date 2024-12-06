package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection

class SkrivBrevLøser (val connection: DBConnection) : AvklaringsbehovsLøser<SkrivBrevLøsning> {
    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SkrivBrevLøsning
    ): LøsningsResultat {
        val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

        brevbestillingRepository.oppdaterStatus(
            behandlingId = kontekst.behandlingId(),
            referanse = løsning.brevbestillingReferanse,
            status = Status.FULLFØRT
        )

        return LøsningsResultat("Brev ferdig")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SKRIV_BREV
    }
}