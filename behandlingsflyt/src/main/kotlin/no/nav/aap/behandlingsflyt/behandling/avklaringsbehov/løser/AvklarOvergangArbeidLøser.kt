package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOvergangArbeidLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarOvergangArbeidLøser(
    private val behandlingRepository: BehandlingRepository,
    private val overgangArbeidRepository: OvergangArbeidRepository,
) : AvklaringsbehovsLøser<AvklarOvergangArbeidLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        overgangArbeidRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarOvergangArbeidLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val nyeVurderinger = løsning.løsningerForPerioder
            .map { it.tilOvergangArbeidVurdering(kontekst) }

        val gamleVurderinger = behandling.forrigeBehandlingId
            ?.let { overgangArbeidRepository.hentHvisEksisterer(it) }
            ?.vurderinger
            ?: emptyList()

        overgangArbeidRepository.lagre(
            behandlingId = behandling.id,
            overgangArbeidVurderinger = nyeVurderinger + gamleVurderinger
        )

        return LøsningsResultat(
            begrunnelse = nyeVurderinger.joinToString(separator = "\n\n") { it.begrunnelse }
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_OVERGANG_ARBEID
    }
}