package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettArbeidsevneLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevnePerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.collections.orEmpty

class FastsettArbeidsevneLøser(
    private val arbeidsevneRepository: ArbeidsevneRepository,
    private val behandlingRepository: BehandlingRepository,
) : AvklaringsbehovsLøser<FastsettArbeidsevneLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        arbeidsevneRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: FastsettArbeidsevneLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val arbeidsevneVurderinger =
            løsning.arbeidsevneVurderinger.map { it.toArbeidsevnevurdering(kontekst.bruker.ident) }
        val gamleVurderinger =
            ArbeidsevnePerioder(behandling.forrigeBehandlingId?.let { arbeidsevneRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty())
        val nyeArbeidsevnePerioder =
            gamleVurderinger.leggTil(ArbeidsevnePerioder(arbeidsevneVurderinger))

        arbeidsevneRepository.lagre(
            kontekst.behandlingId(),
            nyeArbeidsevnePerioder.gjeldendeArbeidsevner()
        )

        return LøsningsResultat(begrunnelse = "Vurdert fastsetting av arbeidsevne")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_ARBEIDSEVNE
    }
}
