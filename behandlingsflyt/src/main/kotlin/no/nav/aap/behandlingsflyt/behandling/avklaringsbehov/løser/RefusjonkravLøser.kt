package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class RefusjonkravLøser(
    private val refusjonkravRepository: RefusjonkravRepository,
    ) : AvklaringsbehovsLøser<RefusjonkravLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        refusjonkravRepository = repositoryProvider.provide()
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: RefusjonkravLøsning): LøsningsResultat {
        val vurderinger =  løsning.refusjonkravVurderinger.map { refusjonkrav ->
            RefusjonkravVurdering(
                harKrav = refusjonkrav.harKrav,
                navKontor = refusjonkrav.navKontor,
                vurdertAv = kontekst.bruker.ident
            )
        }

        refusjonkravRepository.lagre(kontekst.kontekst.sakId, kontekst.behandlingId(), vurderinger)
        return LøsningsResultat("Vurdert refusjonskrav")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.REFUSJON_KRAV
    }

}
