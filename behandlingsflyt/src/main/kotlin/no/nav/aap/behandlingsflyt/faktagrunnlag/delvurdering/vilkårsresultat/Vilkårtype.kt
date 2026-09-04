package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

/**
 * Er `kode` fra felles kodeverk? Eller noe vi har funnet på selv? Dokumenter plz.
 *
 * @param obligatorisk Sier om vilkåret må være tilstede for at flyten skal gå bra. Ikke koblet til lovverk eller valg
 *        av rettighetstype.
 */
enum class Vilkårtype(
    val kode: String,
    val spesielleInnvilgelsesÅrsaker: List<Innvilgelsesårsak>,
    val avslagsårsaker: List<Avslagsårsak>,
    val hjemmel: String,
    val obligatorisk: Boolean = true,
    val kontraktversjon: no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype,
) {
    ALDERSVILKÅRET(
        kode = "AAP-4",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.BRUKER_OVER_67,
            Avslagsårsak.BRUKER_UNDER_18,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-4",
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.ALDERSVILKÅRET,
    ),
    AVSLAG_11_27(
        kode = "AAP-27",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.ANNEN_FULL_YTELSE_AVSLAG,
        ),
        hjemmel = "§ 11-27",
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.AVSLAG_11_27,
    ),
    LOVVALG(
        kode = "AAP-3",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_MEDLEM,
            Avslagsårsak.NORGE_IKKE_KOMPETENT_STAT
        ),
        hjemmel = "§ 2",
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.LOVVALG,
    ),
    SYKDOMSVILKÅRET(
        kode = "AAP-5",
        spesielleInnvilgelsesÅrsaker = listOf(
            Innvilgelsesårsak.STUDENT, // Denne er kun for bakoverkompatibilitet,
            Innvilgelsesårsak.SYKEPENGEERSTATNING,
            Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG,
        ),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL,
            Avslagsårsak.IKKE_NOK_REDUSERT_ARBEIDSEVNE,
            Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET,
            Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE,
        ),
        hjemmel = "§ 11-5",
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.SYKDOMSVILKÅRET,
    ),
    BISTANDSVILKÅRET(
        kode = "AAP-6",
        spesielleInnvilgelsesÅrsaker = listOf(
            Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD,
        ),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING
        ),
        hjemmel = "§ 11-6",
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.BISTANDSVILKÅRET,
    ),
    OVERGANGARBEIDVILKÅRET(
        kode = "AAP-17",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_RETT_PA_AAP_I_PERIODE_SOM_ARBEIDSSOKER,
            Avslagsårsak.VARIGHET_OVERSKREDET_ARBEIDSSØKER,
        ),
        hjemmel = "§ 11-17",
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.OVERGANGARBEIDVILKÅRET,
    ),
    OVERGANGUFØREVILKÅRET(
        kode = "AAP-18",
        spesielleInnvilgelsesÅrsaker = listOf(
            Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD,
        ),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE,
            Avslagsårsak.VARIGHET_OVERSKREDET_OVERGANG_UFORE,
        ),
        hjemmel = "§ 11-18",
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.OVERGANGUFØREVILKÅRET,
    ),
    MEDLEMSKAP(
        kode = "AAP-2",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_MEDLEM,
            Avslagsårsak.IKKE_MEDLEM_FORUTGÅENDE,
        ),
        hjemmel = "§ 11-2",
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.MEDLEMSKAP,
    ),
    GRUNNLAGET(
        kode = "AAP-19",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-19",
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.GRUNNLAGET,
    ),
    SAMORDNING(
        kode = "AAP-27",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.ANNEN_FULL_YTELSE,
            Avslagsårsak.ANNEN_FULL_YTELSE_AVSLAG
        ),
        hjemmel = "§ 11-27",
        obligatorisk = false,
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.SAMORDNING,
    ),
    SAMORDNING_ANNEN_LOVGIVNING(
        kode = "AAP-29",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.INNTEKTSTAP_DEKKES_ETTER_ANNEN_LOVGIVNING
        ),
        hjemmel = "§ 11-29",
        obligatorisk = false,
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING,
    ),
    SYKEPENGEERSTATNING(
        kode = "AAP-13",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_RETT_PA_SYKEPENGEERSTATNING
        ),
        hjemmel = "§ 11-13",
        obligatorisk = false,
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.SYKEPENGEERSTATNING,
    ),
    STUDENT(
        kode = "AAP-14",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_RETT_PA_STUDENT,
            Avslagsårsak.VARIGHET_OVERSKREDET_STUDENT
        ),
        hjemmel = "§ 11-14",
        obligatorisk = false,
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.STUDENT,
    ),
    STRAFFEGJENNOMFØRING(
        kode = "AAP-26",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING
        ),
        hjemmel = "§ 11-26",
        obligatorisk = false,
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.STRAFFEGJENNOMFØRING,
    ),
    AKTIVITETSPLIKT(
        kode = "AAP-7",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS,
            Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR,
        ),
        hjemmel = "§ 11-7",
        obligatorisk = false,
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.AKTIVITETSPLIKT,
    ),
    OPPHOLDSKRAV(
        kode = "AAP-3",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_STANS,
            Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_OPPHØR,
        ),
        hjemmel = "§ 11-3",
        obligatorisk = false,
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.OPPHOLDSKRAV,
    ),
    INNTEKTSBORTFALL(
        kode = "AAP-4-2",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.HAR_RETT_TIL_FULLT_UTTAK_ALDERSPENSJON
        ),
        hjemmel = "§ 11-4 2. ledd",
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.INNTEKTSBORTFALL,
    ),
    ORDINÆR_KVOTE(
        kode = "AAP-12",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP
        ),
        hjemmel = "§ 11-12 1. ledd",
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.ORDINÆR_KVOTE,
    ),
    SYKEPENGEERSTATNING_KVOTE(
        kode = "AAP-13-1",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP
        ),
        hjemmel = "§ 11-13 1. ledd",
        kontraktversjon = no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype.SYKEPENGEERSTATNING_KVOTE,
    );
}