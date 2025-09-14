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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

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

        if (behandling.typeBehandling() != TypeBehandling.Revurdering) {
            throw UgyldigForespørselException("kan kun kansellere revurdering i en revurdering behandling")
        }
        if (behandling.status() !in listOf(Status.OPPRETTET, Status.UTREDES)) {
            throw UgyldigForespørselException("kan kun kansellere revurdering som utredes")
        }

        kansellerRevurderingRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            vurdering = KansellerRevurderingVurdering(
                årsak = løsning.vurdering.årsak?.name?.let { KansellerRevurderingÅrsak.valueOf(it) },
                begrunnelse = løsning.vurdering.begrunnelse,
                vurdertAv = kontekst.bruker,
            ),
        )

        // Oppdaterer behandling årsak med begrunnelse for kansellering av revurdering
        val kansellertVurderingsbehovOgÅrsak = behandlingRepository.hentVurderingsbehovOgÅrsaker(behandling.id)
            .filter { it.vurderingsbehov.any { behov -> behov.type == Vurderingsbehov.REVURDERING_KANSELLERT } }
            .maxByOrNull { it.opprettet }

        if (kansellertVurderingsbehovOgÅrsak != null) {
            val oppdatertVurderingsbehogOgÅrsak = VurderingsbehovOgÅrsak(
                kansellertVurderingsbehovOgÅrsak.vurderingsbehov,
                kansellertVurderingsbehovOgÅrsak.årsak,
                LocalDateTime.now(),
                løsning.vurdering.begrunnelse
            )
            behandlingRepository.oppdaterVurderingsbehovOgÅrsak(behandling, oppdatertVurderingsbehogOgÅrsak)

            val nyesteBehandlingÅrsakId = behandlingRepository.hentBehandlingAarsakId(behandling.id).firstOrNull()

            if (nyesteBehandlingÅrsakId != null) {
                behandlingRepository.oppdaterVurderingsbehovMedNyesteBehandlingAarsakId(behandling.id, nyesteBehandlingÅrsakId)
            }

        }

        return LøsningsResultat(løsning.vurdering.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.KANSELLER_REVURDERING
    }
}