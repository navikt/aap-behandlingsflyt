package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSykepengerErstatningLøser(
    private val behandlingRepository: BehandlingRepository,
    private val sykepengerErstatningRepository: SykepengerErstatningRepository,
) : AvklaringsbehovsLøser<AvklarSykepengerErstatningLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sykepengerErstatningRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSykepengerErstatningLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        sykepengerErstatningRepository.lagre(
            behandlingId = behandling.id,
            vurdering = løsning.sykepengeerstatningVurdering
        )

        return LøsningsResultat(
            begrunnelse = løsning.sykepengeerstatningVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKEPENGEERSTATNING
    }
}
