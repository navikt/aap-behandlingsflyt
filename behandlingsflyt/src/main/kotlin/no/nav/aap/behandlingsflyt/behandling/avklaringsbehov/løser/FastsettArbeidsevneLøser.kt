package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertFastsettArbeidsevneLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.collections.orEmpty

class FastsettArbeidsevneLøser(
    private val arbeidsevneRepository: ArbeidsevneRepository,
) : AvklaringsbehovsLøser<PeriodisertFastsettArbeidsevneLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        arbeidsevneRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: PeriodisertFastsettArbeidsevneLøsning
    ): LøsningsResultat {
        val nyeVurderinger = løsning.løsningerForPerioder.map { it.toArbeidsevnevurdering(kontekst) }

        val vedtatteVurderinger =
            kontekst.kontekst.forrigeBehandlingId?.let { arbeidsevneRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()

        arbeidsevneRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            vurderinger = vedtatteVurderinger + nyeVurderinger
        )

        return LøsningsResultat(begrunnelse = "Vurdert fastsetting av arbeidsevne")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_ARBEIDSEVNE
    }
}
