package no.nav.aap.rettighetstype

import no.nav.aap.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.vilkårsresultat.RettighetsType

val kravprioritet =
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

    override val kravForutgåendeMedlemskap = KravspesifikasjonForRettighetsType.MåVæreOppfylt()
    override val kravSykdom = KravspesifikasjonForRettighetsType.MåVæreOppfylt(Innvilgelsesårsak.SYKEPENGEERSTATNING)

    override val kravSykepengeerstatning = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravBistand = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravOvergangUfør = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravOvergangArbeid = KravspesifikasjonForRettighetsType.IngenKrav
    override val forutgåendeAap = KravspesifikasjonForRettighetsType.IngenKravOmForutgåendeAAP
    override val kravStudent = KravspesifikasjonForRettighetsType.IngenKrav
}

data object KravForStudent : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.STUDENT
    override val kravStudent = KravspesifikasjonForRettighetsType.MåVæreOppfylt()
    override val kravForutgåendeMedlemskap = KravspesifikasjonForRettighetsType.MåVæreOppfylt()

    override val kravBistand = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravSykdom = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravOvergangUfør = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravOvergangArbeid = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravSykepengeerstatning = KravspesifikasjonForRettighetsType.IngenKrav
    override val forutgåendeAap = KravspesifikasjonForRettighetsType.IngenKravOmForutgåendeAAP
}

data object KravForOrdinærAap : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.BISTANDSBEHOV

    override val kravForutgåendeMedlemskap = KravspesifikasjonForRettighetsType.MåVæreOppfylt()
    override val kravSykdom = KravspesifikasjonForRettighetsType.MåVæreOppfylt()
    override val kravBistand = KravspesifikasjonForRettighetsType.MåVæreOppfylt()

    override val kravOvergangUfør = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravOvergangArbeid = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravSykepengeerstatning = KravspesifikasjonForRettighetsType.IngenKrav
    override val forutgåendeAap = KravspesifikasjonForRettighetsType.IngenKravOmForutgåendeAAP
    override val kravStudent = KravspesifikasjonForRettighetsType.IngenKrav
}

data object KravForYrkesskade : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.BISTANDSBEHOV

    override val kravSykdom =
        KravspesifikasjonForRettighetsType.MåVæreOppfylt(Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG)
    override val kravBistand = KravspesifikasjonForRettighetsType.MåVæreOppfylt()

    override val kravForutgåendeMedlemskap = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravOvergangUfør = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravOvergangArbeid = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravSykepengeerstatning = KravspesifikasjonForRettighetsType.IngenKrav
    override val forutgåendeAap = KravspesifikasjonForRettighetsType.IngenKravOmForutgåendeAAP
    override val kravStudent = KravspesifikasjonForRettighetsType.IngenKrav
}

data object KravForSykepengeerstatning : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.SYKEPENGEERSTATNING

    override val kravForutgåendeMedlemskap = KravspesifikasjonForRettighetsType.MåVæreOppfylt()
    override val kravSykepengeerstatning = KravspesifikasjonForRettighetsType.MåVæreOppfylt()

    override val kravSykdom = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravBistand = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravOvergangUfør = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravOvergangArbeid = KravspesifikasjonForRettighetsType.IngenKrav
    override val forutgåendeAap = KravspesifikasjonForRettighetsType.IngenKravOmForutgåendeAAP
    override val kravStudent = KravspesifikasjonForRettighetsType.IngenKrav
}

data object KravForOvergangUføretrygd : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.VURDERES_FOR_UFØRETRYGD

    override val kravForutgåendeMedlemskap = KravspesifikasjonForRettighetsType.MåVæreOppfylt()
    override val kravOvergangUfør =
        KravspesifikasjonForRettighetsType.MåVæreOppfylt(null, Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD)

    override val kravSykdom = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravBistand = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravOvergangArbeid = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravSykepengeerstatning = KravspesifikasjonForRettighetsType.IngenKrav
    override val forutgåendeAap = KravspesifikasjonForRettighetsType.IngenKravOmForutgåendeAAP
    override val kravStudent = KravspesifikasjonForRettighetsType.IngenKrav
}

data object KravForOvergangArbeid : KravspesifikasjonForRettighetsType {
    override val rettighetstype = RettighetsType.ARBEIDSSØKER

    override val kravForutgåendeMedlemskap = KravspesifikasjonForRettighetsType.MåVæreOppfylt()
    override val kravOvergangArbeid = KravspesifikasjonForRettighetsType.MåVæreOppfylt()
    override val forutgåendeAap = KravspesifikasjonForRettighetsType.KravOmForutgåendeAAP(RettighetsType.BISTANDSBEHOV)

    override val kravBistand = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravSykdom = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravOvergangUfør = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravSykepengeerstatning = KravspesifikasjonForRettighetsType.IngenKrav
    override val kravStudent = KravspesifikasjonForRettighetsType.IngenKrav
}