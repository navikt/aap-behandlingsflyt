package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.OverstyrIkkeOppfyltMeldepliktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class OverstyrIkkeOppfyltMeldepliktLøser(
    private val behandlingRepository: BehandlingRepository,
    private val overstyringMeldepliktRepository: OverstyringMeldepliktRepository,
) : AvklaringsbehovsLøser<OverstyrIkkeOppfyltMeldepliktLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        overstyringMeldepliktRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: OverstyrIkkeOppfyltMeldepliktLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val overstyringMeldepliktVurderinger = OverstyringMeldepliktVurdering(
            vurdertAv = kontekst.bruker.ident,
            opprettetTid = LocalDateTime.now(),
            vurdertIBehandling = behandling.referanse,
            perioder = løsning.meldepliktOverstyringVurdering.perioder
        )

        overstyringMeldepliktRepository.lagre(
            behandlingId = behandling.id,
            vurdering = overstyringMeldepliktVurderinger
        )

        return LøsningsResultat(
            begrunnelse = "Har overstyrt eller endret en vurdering av overholdt meldeplikt",
            kreverToTrinn = overstyringMeldepliktVurderinger.perioder.isNotEmpty()
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.OVERSTYR_IKKE_OPPFYLT_MELDEPLIKT
    }
}
