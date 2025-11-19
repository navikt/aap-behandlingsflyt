package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOverstyrtForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarOverstyrtForutgåendeMedlemskapLøser(
    private val forutgåendeMedlemskapArbeidInntektRepository: MedlemskapArbeidInntektForutgåendeRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
) : AvklaringsbehovsLøser<AvklarOverstyrtForutgåendeMedlemskapLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this(
        forutgåendeMedlemskapArbeidInntektRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarOverstyrtForutgåendeMedlemskapLøsning
    ): LøsningsResultat {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId())
        if (vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET).vilkårsperioder()
                .any { it.innvilgelsesårsak == Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG }
        ) {
            return LøsningsResultat("OVERSTYRT: Fant yrkesskade, overstyring ikke tillat.")
        }

        forutgåendeMedlemskapArbeidInntektRepository.lagreManuellVurdering(
            kontekst.behandlingId(),
            ManuellVurderingForForutgåendeMedlemskap(
                begrunnelse = løsning.manuellVurderingForForutgåendeMedlemskap.begrunnelse,
                harForutgåendeMedlemskap = løsning.manuellVurderingForForutgåendeMedlemskap.harForutgåendeMedlemskap,
                varMedlemMedNedsattArbeidsevne = løsning.manuellVurderingForForutgåendeMedlemskap.varMedlemMedNedsattArbeidsevne,
                medlemMedUnntakAvMaksFemAar = løsning.manuellVurderingForForutgåendeMedlemskap.medlemMedUnntakAvMaksFemAar,
                vurdertAv = kontekst.bruker.ident,
                overstyrt = true
            )
        )

        return LøsningsResultat("OVERSTYRT: Vurdert forutgående medlemskap manuelt.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.MANUELL_OVERSTYRING_MEDLEMSKAP
    }
}