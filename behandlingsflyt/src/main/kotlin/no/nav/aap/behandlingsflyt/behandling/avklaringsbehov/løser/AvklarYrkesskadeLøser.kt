package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarYrkesskadeLøser(
    private val behandlingRepository: BehandlingRepository,
    private val sykdomRepository: SykdomRepository,
) : AvklaringsbehovsLøser<AvklarYrkesskadeLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarYrkesskadeLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        sykdomRepository.lagre(
            behandlingId = behandling.id,
            yrkesskadevurdering = løsning.yrkesskadesvurdering.toYrkesskadevurdering(),
        )

        return LøsningsResultat(
            begrunnelse = løsning.yrkesskadesvurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_YRKESSKADE
    }
}
