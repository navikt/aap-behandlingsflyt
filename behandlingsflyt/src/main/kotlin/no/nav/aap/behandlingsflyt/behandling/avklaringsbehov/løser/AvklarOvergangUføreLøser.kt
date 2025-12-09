package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOvergangUføreEnkelLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.collections.orEmpty

class AvklarOvergangUføreLøser(
    private val behandlingRepository: BehandlingRepository,
    private val overgangUforeRepository: OvergangUføreRepository,
    private val sakRepository: SakRepository,
) : AvklaringsbehovsLøser<AvklarOvergangUføreEnkelLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        overgangUforeRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
    )


    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarOvergangUføreEnkelLøsning
    ): LøsningsResultat {
        val løsninger = løsning.løsningerForPerioder ?: listOf(requireNotNull(løsning.overgangUføreVurdering))
        
        val (behandlingId, sakId, forrigeBehandlingId) = kontekst.kontekst.let {
            Triple(
                it.behandlingId,
                it.sakId,
                it.forrigeBehandlingId
            )
        }

        val rettighetsperiode = sakRepository.hent(sakId).rettighetsperiode

        val vedtatteVurderinger = forrigeBehandlingId
            ?.let { overgangUforeRepository.hentHvisEksisterer(it) }
            ?.vurderinger
            .orEmpty()

        val nyeVurderinger = løsninger.map {
            it.tilOvergangUføreVurdering(
                kontekst.bruker,
                rettighetsperiode.fom,
                behandlingId
            )
        }

        overgangUforeRepository.lagre(
            behandlingId = behandlingId,
            overgangUføreVurderinger = nyeVurderinger + vedtatteVurderinger
        )

        return LøsningsResultat(
            begrunnelse = nyeVurderinger.joinToString("\n") { it.begrunnelse }
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_OVERGANG_UFORE
    }
}
