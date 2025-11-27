package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarPeriodisertForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.validerGyldigVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarForutgåendeMedlemskapLøser(
    private val forutgåendeMedlemskapRepository: MedlemskapArbeidInntektForutgåendeRepository,
    private val behandlingRepository: BehandlingRepository,
) : AvklaringsbehovsLøser<AvklarPeriodisertForutgåendeMedlemskapLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        forutgåendeMedlemskapRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarPeriodisertForutgåendeMedlemskapLøsning): LøsningsResultat {
        løsning.løsningerForPerioder.validerGyldigVurderinger()
            .throwOnInvalid { UgyldigForespørselException(it.errorMessage) }

        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val nyeVurderinger = løsning.løsningerForPerioder.map { it.toManuellVurderingForForutgåendeMedlemskap(kontekst, overstyrt = false) }
        val tidligereVurderinger = kontekst.kontekst.forrigeBehandlingId?.let {
            forutgåendeMedlemskapRepository.hentHvisEksisterer(it)
        }?.vurderinger.orEmpty()
        val vurderinger = tidligereVurderinger + nyeVurderinger

        forutgåendeMedlemskapRepository.lagreVurderinger(behandling.id, vurderinger)
        return LøsningsResultat("Vurdert forutgående medlemskap manuelt.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP
    }
}