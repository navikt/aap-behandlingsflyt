package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOppfølgingLokalkontorLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOppfølgingNAYLøsning
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsBehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsoppgaveGrunnlagDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.repository.RepositoryProvider

class AvklarOppfølgingLokalkontorLøser(private val repositoryProvider: RepositoryProvider) :
    AvklaringsbehovsLøser<AvklarOppfølgingLokalkontorLøsning> {
    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarOppfølgingLokalkontorLøsning
    ): LøsningsResultat {
        val repo = repositoryProvider.provide<OppfølgingsBehandlingRepository>()

        return resultat(kontekst, repo, løsning.avklarOppfølgingsbehovVurdering)
    }

    override fun forBehov(): Definisjon = Definisjon.AVKLAR_OPPFØLGINGSBEHOV_LOKALKONTOR
}

class AvklarOppfølgingNAYLøser(private val repositoryProvider: RepositoryProvider) :
    AvklaringsbehovsLøser<AvklarOppfølgingNAYLøsning> {
    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarOppfølgingNAYLøsning
    ): LøsningsResultat {
        val repo = repositoryProvider.provide<OppfølgingsBehandlingRepository>()

        return resultat(kontekst, repo, løsning.avklarOppfølgingsbehovVurdering)
    }

    override fun forBehov(): Definisjon = Definisjon.AVKLAR_OPPFØLGINGSBEHOV_NAY
}

private fun resultat(
    kontekst: AvklaringsbehovKontekst,
    repo: OppfølgingsBehandlingRepository,
    løsning: OppfølgingsoppgaveGrunnlagDto
): LøsningsResultat {
    val behandlingId = kontekst.behandlingId()
    val vurdertAv = kontekst.bruker.ident

    repo.lagre(behandlingId, løsning.tilOppfølgingsoppgaveGrunnlag(vurdertAv))

    return LøsningsResultat("Lagret oppfølgingsbehov for $behandlingId med vurdert av $vurdertAv")
}