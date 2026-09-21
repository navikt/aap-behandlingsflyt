package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettPåklagetBehandlingLøsning
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingRepository
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingsbehandling
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.erAvsluttet
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetVedtakType
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider

class FastsettPåklagetBehandlingLøser(
    private val påklagetBehandlingRepository: PåklagetBehandlingRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
) :
    AvklaringsbehovsLøser<FastsettPåklagetBehandlingLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        påklagetBehandlingRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        tilbakekrevingRepository = repositoryProvider.provide()
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FastsettPåklagetBehandlingLøsning): LøsningsResultat {
        val referanse = løsning.påklagetBehandlingVurdering.påklagetBehandling

        val vurdering = if (løsning.påklagetBehandlingVurdering.påklagetVedtakType == PåklagetVedtakType.TILBAKEKREVING) {
            requireNotNull(referanse) { "Påklaget tilbakekrevingsbehandling må være utfylt" }
            tilbakekrevingRepository.hent(referanse).valider()
            løsning.påklagetBehandlingVurdering.tilVurdering(
                bruker = kontekst.bruker,
                behandlingId = null,
                tilbakekrevingsbehandling = referanse
            )
        } else {
            val påklagetBehandling = referanse?.let {
                behandlingRepository.hent(BehandlingReferanse(it)).valider()
            }
            løsning.påklagetBehandlingVurdering.tilVurdering(
                bruker = kontekst.bruker,
                behandlingId = påklagetBehandling?.id
            )
        }

        påklagetBehandlingRepository.lagre(
            behandlingId = kontekst.kontekst.behandlingId,
            påklagetBehandlingVurdering = vurdering
        )

        return LøsningsResultat(begrunnelse = "Vurdert påklaget behandling")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_PÅKLAGET_BEHANDLING
    }

    private fun Behandling.valider(): Behandling {
        if (!this.typeBehandling().erYtelsesbehandling() && this.typeBehandling() != TypeBehandling.Klage) {
            throw UgyldigForespørselException("Kan ikke klage på type ${this.typeBehandling()}")
        }


        if (!this.status().erAvsluttet()) {
            throw UgyldigForespørselException("Kan ikke klage på åpen behandling ${this.status()}")
        }
        return this
    }

    private fun Tilbakekrevingsbehandling.valider(): Tilbakekrevingsbehandling {
        if (!this.behandlingsstatus.erAvsluttet()) {
            throw UgyldigForespørselException("Kan ikke klage på åpen behandling ${this.behandlingsstatus}")
        }
        return this
    }
}
