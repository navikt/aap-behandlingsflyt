package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettArbeidsevneLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevnePerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class FastsettArbeidsevneLøser(
    private val arbeidsevneRepository: ArbeidsevneRepository,
) : AvklaringsbehovsLøser<FastsettArbeidsevneLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        arbeidsevneRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: FastsettArbeidsevneLøsning
    ): LøsningsResultat {
        val arbeidsevneVurderinger =
            løsning.arbeidsevneVurderinger.map { it.toArbeidsevnevurdering() }
        val eksisterendeArbeidsevnePerioder = ArbeidsevnePerioder(
            arbeidsevneRepository.hentHvisEksisterer(kontekst.behandlingId())?.vurderinger.orEmpty()
        )
        val nyeArbeidsevnePerioder =
            eksisterendeArbeidsevnePerioder.leggTil(ArbeidsevnePerioder(arbeidsevneVurderinger))

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
