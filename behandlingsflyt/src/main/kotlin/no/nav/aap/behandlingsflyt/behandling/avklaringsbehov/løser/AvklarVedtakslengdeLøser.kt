package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarVedtakslengdeLøsning
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class AvklarVedtakslengdeLøser(
    private val vedtakslengdeRepository: VedtakslengdeRepository
) : AvklaringsbehovsLøser<AvklarVedtakslengdeLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        vedtakslengdeRepository = repositoryProvider.provide()
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarVedtakslengdeLøsning): LøsningsResultat {
        val vurdering = løsning.vedtakslengdeVurdering
        val vedtattGrunnlag = kontekst.kontekst.forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(it) }
        val grunnlag = vedtakslengdeRepository.hentHvisEksisterer(kontekst.behandlingId())

        val gjeldendeVedtatteVurderinger = vedtattGrunnlag?.vurderinger.orEmpty()
        val nyeVurderingerFraBehandlingen = grunnlag?.vurderinger?.filter { it.vurdertIBehandling == kontekst.behandlingId() }.orEmpty()

        // Kun en ny automatisk vurdering per behandling
        val automatiskVurderingFraBehandlingen = nyeVurderingerFraBehandlingen.filter { it.vurdertAutomatisk }.take(1)

        // Kun en manuell vurdering per behandling
        val nyManuellVurdering = VedtakslengdeVurdering(
            sluttdato = vurdering.sluttdato,
            utvidetMed = vedtattGrunnlag?.gjeldendeVurdering()?.utvidetMed ?: ÅrMedHverdager.FØRSTE_ÅR,
            vurdertAv = kontekst.bruker,
            vurdertIBehandling = kontekst.behandlingId(),
            opprettet = Instant.now()
        )

        vedtakslengdeRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            vurderinger = gjeldendeVedtatteVurderinger + automatiskVurderingFraBehandlingen + nyManuellVurdering
        )

        return LøsningsResultat(vurdering.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_VEDTAKSLENGDE
    }
}

