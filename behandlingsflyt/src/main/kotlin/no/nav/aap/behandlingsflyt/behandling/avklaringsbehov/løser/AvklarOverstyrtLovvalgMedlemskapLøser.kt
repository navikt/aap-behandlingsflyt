package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOverstyrtLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarOverstyrtLovvalgMedlemskapLøser(
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository
) : AvklaringsbehovsLøser<AvklarOverstyrtLovvalgMedlemskapLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        medlemskapArbeidInntektRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarOverstyrtLovvalgMedlemskapLøsning
    ): LøsningsResultat {
        medlemskapArbeidInntektRepository.lagreManuellVurdering(
            kontekst.behandlingId(),
            ManuellVurderingForLovvalgMedlemskap(
                lovvalgVedSøknadsTidspunkt = løsning.manuellVurderingForLovvalgMedlemskap.lovvalgVedSøknadsTidspunkt,
                medlemskapVedSøknadsTidspunkt = løsning.manuellVurderingForLovvalgMedlemskap.medlemskapVedSøknadsTidspunkt,
                vurdertAv = kontekst.bruker.ident,
                overstyrt = true
            )
        )
        return LøsningsResultat("OVERSTYRT: Vurdert lovvalg & medlemskap manuelt.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.MANUELL_OVERSTYRING_LOVVALG
    }
}