package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivForhåndsvarselKlageFormkravBrevLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class SkrivForhåndsvarselKlageFormkravBrevLøser(
    private val skrivBrevAvklaringsbehovLøser: SkrivBrevAvklaringsbehovLøser,
    private val formkravRepository: FormkravRepository
) :
    AvklaringsbehovsLøser<SkrivForhåndsvarselKlageFormkravBrevLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        skrivBrevAvklaringsbehovLøser = SkrivBrevAvklaringsbehovLøser(repositoryProvider),
        formkravRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SkrivForhåndsvarselKlageFormkravBrevLøsning
    ): LøsningsResultat {
        val generellLøsning = skrivBrevAvklaringsbehovLøser.løs(
            kontekst,
            SkrivBrevAvklaringsbehovLøsning(løsning.brevbestillingReferanse, løsning.handling)
        )
        when (løsning.handling) {
            SkrivBrevAvklaringsbehovLøsning.Handling.FERDIGSTILL -> {
                formkravRepository.lagreFrist(
                    kontekst.kontekst.behandlingId,
                    LocalDate.now(),
                    LocalDate.now().plusWeeks(3)
                )
            }

            else -> {} // Ferdig
        }
        return generellLøsning
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV
    }
}
