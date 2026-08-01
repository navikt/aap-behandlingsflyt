package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.ArbeidsopptrappingLøsning
import no.nav.aap.behandlingsflyt.steg.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class ArbeidsopptrappingLøser(
    private val arbeidsopptrappingRepositiory: ArbeidsopptrappingRepository,
    private val behandlingRepository: BehandlingRepository
) : AvklaringsbehovsLøser<ArbeidsopptrappingLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        arbeidsopptrappingRepositiory = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: ArbeidsopptrappingLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val nyeVurderinger = løsning.løsningerForPerioder.map { it.toArbeidsopptrappingVurdering(kontekst) }
        val gamleVurderinger =
            behandling.forrigeBehandlingId?.let { arbeidsopptrappingRepositiory.hentHvisEksisterer(it) }?.vurderinger.orEmpty()

        arbeidsopptrappingRepositiory.lagre(
            behandlingId = behandling.id,
            arbeidsopptrappingVurderinger = gamleVurderinger + nyeVurderinger
        )
        return LøsningsResultat(begrunnelse = "Vurdert arbeidsopptrapping")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.ARBEIDSOPPTRAPPING
    }
}