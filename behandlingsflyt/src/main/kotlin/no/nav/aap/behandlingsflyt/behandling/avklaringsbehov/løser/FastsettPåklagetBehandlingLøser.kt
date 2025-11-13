package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettPåklagetBehandlingLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider

class FastsettPåklagetBehandlingLøser(
    private val påklagetBehandlingRepository: PåklagetBehandlingRepository,
    private val behandlingRepository: BehandlingRepository,
) :
    AvklaringsbehovsLøser<FastsettPåklagetBehandlingLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        påklagetBehandlingRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide()
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FastsettPåklagetBehandlingLøsning): LøsningsResultat {
        val påklagetBehandling = løsning.påklagetBehandlingVurdering.påklagetBehandling?.let {
            behandlingRepository.hent(BehandlingReferanse(løsning.påklagetBehandlingVurdering.påklagetBehandling)).valider()
        }

        påklagetBehandlingRepository.lagre(
            behandlingId = kontekst.kontekst.behandlingId,
            påklagetBehandlingVurdering = løsning.påklagetBehandlingVurdering.tilVurdering(
                kontekst.bruker,
                påklagetBehandling?.id
            )
        )
        return LøsningsResultat(begrunnelse = "Vurdert påklaget behandling")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_PÅKLAGET_BEHANDLING
    }

    private fun Behandling.valider(): Behandling {
        if (!this.typeBehandling().erYtelsesbehandling()) {
            throw UgyldigForespørselException("Kan ikke klage på type ${this.typeBehandling()}")
        }


        if (!this.status().erAvsluttet()) {
            throw UgyldigForespørselException("Kan ikke klage på åpen behandling ${this.status()}")
        }
        return this
    }
}
