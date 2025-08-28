package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktFritaksperioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class FritakFraMeldepliktLøser(
    private val behandlingRepository: BehandlingRepository,
    private val meldepliktRepository: MeldepliktRepository,
) : AvklaringsbehovsLøser<FritakMeldepliktLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        meldepliktRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: FritakMeldepliktLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val fritaksvurderinger =
            løsning.fritaksvurderinger.map { Fritaksvurdering(
                harFritak = it.harFritak,
                fraDato = it.fraDato,
                begrunnelse = it.begrunnelse,
                vurdertAv = kontekst.bruker.ident,
                opprettetTid = null,
            ) }
        val eksisterendeFritaksperioder = MeldepliktFritaksperioder(
            behandling.forrigeBehandlingId?.let { meldepliktRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()
        )

        val nyeFritaksperioder =
            eksisterendeFritaksperioder.leggTil(MeldepliktFritaksperioder(fritaksvurderinger))

        meldepliktRepository.lagre(
            behandlingId = behandling.id,
            vurderinger = nyeFritaksperioder.gjeldendeFritaksvurderinger()
        )

        return LøsningsResultat(
            begrunnelse = "Vurdert fritak meldeplikt",
            kreverToTrinn = fritaksvurderinger.minstEttFritak()
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FRITAK_MELDEPLIKT
    }

    private fun List<Fritaksvurdering>.minstEttFritak(): Boolean {
        return this.any { it.harFritak }
    }
}
