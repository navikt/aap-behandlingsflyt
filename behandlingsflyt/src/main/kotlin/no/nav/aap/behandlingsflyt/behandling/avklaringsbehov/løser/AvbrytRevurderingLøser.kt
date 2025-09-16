package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvbrytRevurderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingRepository
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingVurdering
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingÅrsak
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class AvbrytRevurderingLøser(
    private val behandlingRepository: BehandlingRepository,
    private val avbrytRevurderingRepository: AvbrytRevurderingRepository
) : AvklaringsbehovsLøser<AvbrytRevurderingLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        avbrytRevurderingRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvbrytRevurderingLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId())

        if (behandling.typeBehandling() != TypeBehandling.Revurdering) {
            throw UgyldigForespørselException("kan kun avbryte revurdering i en revurdering behandling")
        }
        if (behandling.status() !in listOf(Status.OPPRETTET, Status.UTREDES)) {
            throw UgyldigForespørselException("kan kun avbryte revurdering som utredes")
        }

        avbrytRevurderingRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            vurdering = AvbrytRevurderingVurdering(
                årsak = løsning.vurdering.årsak?.name?.let { AvbrytRevurderingÅrsak.valueOf(it) },
                begrunnelse = løsning.vurdering.begrunnelse,
                vurdertAv = kontekst.bruker,
            ),
        )

        // Oppdaterer behandling årsak med begrunnelse for hvorfor revurdering ble avbrutt
        val avbrytVurderingsbehovOgÅrsak = behandlingRepository.hentVurderingsbehovOgÅrsaker(behandling.id)
            .filter { it.vurderingsbehov.any { behov -> behov.type == Vurderingsbehov.REVURDERING_AVBRUTT } }
            .maxByOrNull { it.opprettet }

        if (avbrytVurderingsbehovOgÅrsak != null) {
            val oppdatertVurderingsbehogOgÅrsak = VurderingsbehovOgÅrsak(
                avbrytVurderingsbehovOgÅrsak.vurderingsbehov,
                avbrytVurderingsbehovOgÅrsak.årsak,
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
        return Definisjon.AVBRYT_REVURDERING
    }
}