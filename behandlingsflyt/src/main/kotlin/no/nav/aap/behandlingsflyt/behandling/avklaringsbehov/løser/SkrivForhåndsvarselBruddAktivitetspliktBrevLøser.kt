package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivForhåndsvarselBruddAktivitetspliktBrevLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class SkrivForhåndsvarselBruddAktivitetspliktBrevLøser(
    private val skrivBrevAvklaringsbehovLøser: SkrivBrevAvklaringsbehovLøser,
    private val aktivitetsplikt11_7Repository: Aktivitetsplikt11_7Repository
) :
    AvklaringsbehovsLøser<SkrivForhåndsvarselBruddAktivitetspliktBrevLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        skrivBrevAvklaringsbehovLøser = SkrivBrevAvklaringsbehovLøser(repositoryProvider, gatewayProvider),
        aktivitetsplikt11_7Repository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SkrivForhåndsvarselBruddAktivitetspliktBrevLøsning
    ): LøsningsResultat {
        val generellLøsning = skrivBrevAvklaringsbehovLøser.løs(
            kontekst,
            SkrivBrevAvklaringsbehovLøsning(løsning.brevbestillingReferanse, løsning.handling, løsning.mottakere)
        )
        when (løsning.handling) {
            SkrivBrevAvklaringsbehovLøsning.Handling.FERDIGSTILL -> aktivitetsplikt11_7Repository.lagreFrist(
                kontekst.kontekst.behandlingId,
                LocalDate.now(),
                LocalDate.now().plusWeeks(3)
            )

            else -> {}
        }

        return generellLøsning
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV
    }
}
