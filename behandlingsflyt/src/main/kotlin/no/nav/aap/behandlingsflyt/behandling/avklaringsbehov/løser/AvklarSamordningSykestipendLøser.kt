package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningSykestipendLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSamordningSykestipendLøser(
    repositoryProvider: RepositoryProvider
): AvklaringsbehovsLøser<AvklarSamordningSykestipendLøsning> {

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSamordningSykestipendLøsning
    ): LøsningsResultat {
        return LøsningsResultat(
            begrunnelse = løsning.løsning.begrunnelse,
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAMORDNING_SYKESTIPEND
    }
}