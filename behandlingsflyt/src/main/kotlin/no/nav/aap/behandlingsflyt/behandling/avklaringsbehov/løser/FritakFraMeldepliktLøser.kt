package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertFritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.PeriodisertFritaksvurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class FritakFraMeldepliktLøser(
    private val behandlingRepository: BehandlingRepository,
    private val meldepliktRepository: MeldepliktRepository,
) : AvklaringsbehovsLøser<PeriodisertFritakMeldepliktLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        meldepliktRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: PeriodisertFritakMeldepliktLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val nyeVurderinger =
            løsning.løsningerForPerioder.map { toFritaksvurdering(it, kontekst) }

        val vedtatteVurderinger = behandling.forrigeBehandlingId?.let { meldepliktRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()

        meldepliktRepository.lagre(
            behandlingId = behandling.id,
            vurderinger = vedtatteVurderinger + nyeVurderinger,
        )

        return LøsningsResultat(
            begrunnelse = "Vurdert fritak meldeplikt",
            kreverToTrinn = nyeVurderinger.minstEttFritak()
        )
    }

    private fun toFritaksvurdering(
        dto: PeriodisertFritaksvurderingDto,
        kontekst: AvklaringsbehovKontekst
    ): Fritaksvurdering = Fritaksvurdering(
        harFritak = dto.harFritak,
        fraDato = dto.fom,
        tilDato = dto.tom,
        begrunnelse = dto.begrunnelse,
        vurdertAv = kontekst.bruker.ident,
        vurdertIBehandling = kontekst.behandlingId(),
        opprettetTid = LocalDateTime.now()
    )

    override fun forBehov(): Definisjon {
        return Definisjon.FRITAK_MELDEPLIKT
    }

    private fun List<Fritaksvurdering>.minstEttFritak(): Boolean {
        return this.any { it.harFritak }
    }
}
