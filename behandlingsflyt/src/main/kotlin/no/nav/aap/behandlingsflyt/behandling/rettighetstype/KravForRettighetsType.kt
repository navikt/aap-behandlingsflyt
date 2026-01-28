package no.nav.aap.behandlingsflyt.behandling.rettighetstype

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KravspesifikasjonForRettighetsType.IngenKrav
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KravspesifikasjonForRettighetsType.IngenKravOmForutgåendeAAP
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KravspesifikasjonForRettighetsType.KravOmForutgåendeAAP
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KravspesifikasjonForRettighetsType.MåVæreOppfylt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType

internal val kravprioritet =
    /**
     * Rekkefølgen på disse er av betydning: første match blir valgt. Ideelt sett burde de nok være i samme rekkefølge
     * som vi vurdere vilkår i flyten. Samtidig er nok en del (nesten alle?) av kravene gjensidig utelukkende.
     * */
    listOf(
        KravForStudent,
        KravForOrdinærAap,
        KravForYrkesskade,
        KravForOvergangUføretrygd,
        KravForSykepengeerstatning,
        KravForSykepengeerstatningGammeltFormat,
        KravForOvergangArbeid,
    )



object KravForSykepengeerstatningGammeltFormat : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.SYKEPENGEERSTATNING

    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = MåVæreOppfylt(Innvilgelsesårsak.SYKEPENGEERSTATNING)

    override val kravSykepengeerstatning = IngenKrav
    override val kravBistand = IngenKrav
    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
    override val kravStudent = IngenKrav
}

data object KravForStudent : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.STUDENT
    override val kravStudent = MåVæreOppfylt()
    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravBistand = IngenKrav

    override val kravSykdom = IngenKrav
    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val kravSykepengeerstatning = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
}

data object KravForOrdinærAap : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.BISTANDSBEHOV

    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = MåVæreOppfylt()
    override val kravBistand = MåVæreOppfylt()

    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val kravSykepengeerstatning = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
    override val kravStudent = IngenKrav
}

data object KravForYrkesskade : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.BISTANDSBEHOV

    override val kravSykdom = MåVæreOppfylt(Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG)
    override val kravBistand = MåVæreOppfylt()

    override val kravForutgåendeMedlemskap = IngenKrav
    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val kravSykepengeerstatning = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
    override val kravStudent = IngenKrav
}

data object KravForSykepengeerstatning : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.SYKEPENGEERSTATNING

    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykepengeerstatning = MåVæreOppfylt()

    override val kravSykdom = IngenKrav
    override val kravBistand = IngenKrav
    override val kravOvergangUfør = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
    override val kravStudent = IngenKrav
}

data object KravForOvergangUføretrygd : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.VURDERES_FOR_UFØRETRYGD

    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravSykdom = IngenKrav
    override val kravOvergangUfør = MåVæreOppfylt(null, Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD)

    override val kravBistand = IngenKrav
    override val kravOvergangArbeid = IngenKrav
    override val kravSykepengeerstatning = IngenKrav
    override val forutgåendeAap = IngenKravOmForutgåendeAAP
    override val kravStudent = IngenKrav
}

data object KravForOvergangArbeid : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.ARBEIDSSØKER

    override val kravForutgåendeMedlemskap = MåVæreOppfylt()
    override val kravOvergangArbeid = MåVæreOppfylt()
    override val forutgåendeAap = KravOmForutgåendeAAP(RettighetsType.BISTANDSBEHOV)

    override val kravBistand = IngenKrav
    override val kravSykdom = IngenKrav
    override val kravOvergangUfør = IngenKrav
    override val kravSykepengeerstatning = IngenKrav
    override val kravStudent = IngenKrav
}

