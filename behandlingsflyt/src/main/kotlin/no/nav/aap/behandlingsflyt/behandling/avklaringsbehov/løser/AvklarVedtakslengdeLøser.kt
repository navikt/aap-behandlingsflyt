package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarVedtakslengdeLøsning
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class AvklarVedtakslengdeLøser(
    private val vedtakslengdeRepository: VedtakslengdeRepository
) : AvklaringsbehovsLøser<AvklarVedtakslengdeLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        vedtakslengdeRepository = repositoryProvider.provide()
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarVedtakslengdeLøsning): LøsningsResultat {
        val vedtattGrunnlag = kontekst.kontekst.forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(it) }
        val grunnlag = vedtakslengdeRepository.hentHvisEksisterer(kontekst.behandlingId())

        val gjeldendeVedtatteVurderinger = vedtattGrunnlag?.vurderinger.orEmpty()
        val nyeVurderingerFraBehandlingen = grunnlag?.vurderinger?.filter { it.vurdertIBehandling == kontekst.behandlingId() }.orEmpty()

        // Kun en ny automatisk vurdering per behandling
        val automatiskVurderingFraBehandlingen = nyeVurderingerFraBehandlingen.filter { it.vurdertAutomatisk }.also {
            require(it.size <= 1) { "Det skal kun være opp til én automatisk vurdering per behandling, fant ${it.size} for behandling ${kontekst.behandlingId()}" }
        }

        val nyManuellVurdering = løsning.løsningerForPerioder.also {
            require(it.size <= 1) { "Det skal kun være opp til én manuell vurdering, fant ${it.size} for behandling ${kontekst.behandlingId()}" }
        }.singleOrNull()?.let { vurdering ->
            // Inntil videre er det ikke mulig å innskrenke en vedtatt vedtakslengde
            if (vedtattGrunnlag?.gjeldendeVurdering() != null && vurdering.sluttdato < vedtattGrunnlag.gjeldendeVurdering()?.sluttdato) {
                throw UgyldigForespørselException("Sluttdato for vedtakslengde kan ikke være før en tidligere vedtatt sluttdato")
            }

            VedtakslengdeVurdering(
                sluttdato = vurdering.sluttdato,
                utvidetMed = vedtattGrunnlag?.gjeldendeVurdering()?.utvidetMed ?: ÅrMedHverdager.FØRSTE_ÅR,
                vurdertAv = kontekst.bruker,
                vurdertIBehandling = kontekst.behandlingId(),
                opprettet = Instant.now(),
                begrunnelse = vurdering.begrunnelse
            )
        }

        vedtakslengdeRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            vurderinger = gjeldendeVedtatteVurderinger + automatiskVurderingFraBehandlingen + listOfNotNull(nyManuellVurdering)
        )

        return LøsningsResultat(nyManuellVurdering?.begrunnelse ?: "")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_VEDTAKSLENGDE
    }
}
