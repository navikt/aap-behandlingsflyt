package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarForutgåendeMedlemskapLøser(
    private val forutgåendeMedlemskapRepository: MedlemskapArbeidInntektForutgåendeRepository,
) : AvklaringsbehovsLøser<AvklarForutgåendeMedlemskapLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        forutgåendeMedlemskapRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarForutgåendeMedlemskapLøsning): LøsningsResultat {
        forutgåendeMedlemskapRepository.lagreManuellVurdering(
            kontekst.behandlingId(),
            ManuellVurderingForForutgåendeMedlemskap(
                begrunnelse = løsning.manuellVurderingForForutgåendeMedlemskap.begrunnelse,
                harForutgåendeMedlemskap = løsning.manuellVurderingForForutgåendeMedlemskap.harForutgåendeMedlemskap,
                varMedlemMedNedsattArbeidsevne = løsning.manuellVurderingForForutgåendeMedlemskap.varMedlemMedNedsattArbeidsevne,
                medlemMedUnntakAvMaksFemAar = løsning.manuellVurderingForForutgåendeMedlemskap.medlemMedUnntakAvMaksFemAar,
                vurdertAv = kontekst.bruker.ident,
                overstyrt = false
            )
        )
        return LøsningsResultat("Vurdert forutgående medlemskap manuelt.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP
    }
}