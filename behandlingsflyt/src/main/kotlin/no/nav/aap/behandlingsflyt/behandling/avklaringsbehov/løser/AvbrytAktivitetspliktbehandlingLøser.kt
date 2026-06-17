package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling.AvbrytAktivitetspliktbehandlingÅrsak
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvbrytAktivitetspliktbehandlingLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class AvbrytAktivitetspliktbehandlingLøser(
    private val behandlingRepository: BehandlingRepository,
    private val avbrytAktivitetspliktbehandlingRepository: AvbrytAktivitetspliktbehandlingRepository
) : AvklaringsbehovsLøser<AvbrytAktivitetspliktbehandlingLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        avbrytAktivitetspliktbehandlingRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvbrytAktivitetspliktbehandlingLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId())

        if (behandling.typeBehandling() !in listOf(
                TypeBehandling.Aktivitetsplikt,
                TypeBehandling.Aktivitetsplikt11_9
            )
        ) {
            throw UgyldigForespørselException("kan kun avbryte aktivitetspliktbehandlinger")
        }
        if (behandling.status() !in listOf(Status.OPPRETTET, Status.UTREDES)) {
            throw UgyldigForespørselException("kan kun avbryte aktivitetspliktbehandling som utredes")
        }

        avbrytAktivitetspliktbehandlingRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            vurdering = AvbrytAktivitetspliktbehandlingVurdering(
                årsak = løsning.vurdering.årsak.name.let { AvbrytAktivitetspliktbehandlingÅrsak.valueOf(it) },
                begrunnelse = løsning.vurdering.begrunnelse,
                vurdertAv = kontekst.bruker,
                opprettetTidspunkt = LocalDateTime.now(),
            ),
        )

        return LøsningsResultat(løsning.vurdering.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVBRYT_AKTIVITETSPLIKTBEHANDING
    }
}