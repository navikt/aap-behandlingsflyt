package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarPeriodisertOverstyrtForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.validerGyldigVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.ForutgåendeMedlemskapRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarOverstyrtForutgåendeMedlemskapLøser(
    private val forutgåendeMedlemskapRepository: ForutgåendeMedlemskapRepository,
    private val behandlingRepository: BehandlingRepository,
) : AvklaringsbehovsLøser<AvklarPeriodisertOverstyrtForutgåendeMedlemskapLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this(
        forutgåendeMedlemskapRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarPeriodisertOverstyrtForutgåendeMedlemskapLøsning): LøsningsResultat {
        løsning.løsningerForPerioder.validerGyldigVurderinger()
            .throwOnInvalid { UgyldigForespørselException(it.errorMessage) }

        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val nyeVurderinger = løsning.løsningerForPerioder.map { it.toManuellVurderingForForutgåendeMedlemskap(kontekst, overstyrt = true) }
        val tidligereVurderinger = kontekst.kontekst.forrigeBehandlingId?.let {
            forutgåendeMedlemskapRepository.hentHvisEksisterer(it)
        }?.vurderinger.orEmpty()
        val vurderinger = tidligereVurderinger + nyeVurderinger

        forutgåendeMedlemskapRepository.lagreVurderinger(behandling.id, vurderinger)
        return LøsningsResultat("OVERSTYRT: Vurdert forutgående medlemskap manuelt.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.MANUELL_OVERSTYRING_MEDLEMSKAP
    }
}