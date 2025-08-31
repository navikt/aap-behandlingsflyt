package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KansellerRevurderingLøsning
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingRepository
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingVurdering
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingÅrsak
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class KansellerRevurderingLøser(
    private val behandlingRepository: BehandlingRepository,
    private val kansellerRevurderingRepository: KansellerRevurderingRepository
) : AvklaringsbehovsLøser<KansellerRevurderingLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        kansellerRevurderingRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: KansellerRevurderingLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId())

        require(behandling.typeBehandling() == TypeBehandling.Revurdering) {
            "kan kun kansellere revurdering i en revurdering behandling"
        }
        require(behandling.status() in listOf(Status.OPPRETTET, Status.UTREDES)) {
            "kan kun kansellere revurdering som utredes"
        }

        kansellerRevurderingRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            vurdering = KansellerRevurderingVurdering(
                årsak = løsning.vurdering.årsak?.name?.let { KansellerRevurderingÅrsak.valueOf(it) },
                begrunnelse = løsning.vurdering.begrunnelse,
                vurdertAv = kontekst.bruker,
            ),
        )

        return LøsningsResultat(løsning.vurdering.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.KANSELLER_REVURDERING
    }
}