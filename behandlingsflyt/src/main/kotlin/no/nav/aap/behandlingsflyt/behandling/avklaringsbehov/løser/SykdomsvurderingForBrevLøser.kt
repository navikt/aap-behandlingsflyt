package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SykdomsvurderingForBrevLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class SykdomsvurderingForBrevLøser(
    private val sykdomsvurderingForBrevRepository: SykdomsvurderingForBrevRepository,
    private val behandlingRepository: BehandlingRepository,
) : AvklaringsbehovsLøser<SykdomsvurderingForBrevLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        sykdomsvurderingForBrevRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SykdomsvurderingForBrevLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId())
        val sykdomsvurderingForBrev = SykdomsvurderingForBrev(kontekst.behandlingId(), løsning.vurdering, kontekst.bruker.ident)
        sykdomsvurderingForBrevRepository.lagre(behandling.id, sykdomsvurderingForBrev)

        return LøsningsResultat("Skrevet sykdomsvurdering for brev")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SKRIV_SYKDOMSVURDERING_BREV
    }
}
