package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOppfølgingLøsning
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsBehandlingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.repository.RepositoryProvider

class AvklarOppfølgingLøser(private val repositoryProvider: RepositoryProvider) :
    AvklaringsbehovsLøser<AvklarOppfølgingLøsning> {
    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarOppfølgingLøsning
    ): LøsningsResultat {
        val repo = repositoryProvider.provide<OppfølgingsBehandlingRepository>()

        val behandlingId = kontekst.behandlingId()
        val vurdertAv = kontekst.bruker.ident

        repo.lagre(behandlingId, løsning.avklarOppfølgingsbehovVurdering.tilOppfølgingsoppgaveGrunnlag(vurdertAv))

        return LøsningsResultat("Lagret oppfølgingsbehov for $behandlingId med vurdert av $vurdertAv")
    }

    override fun forBehov(): Definisjon = Definisjon.AVKLAR_OPPFØLGINGSBEHOV
}