package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

enum class Avslagsårsak(
    /**
     * Er `kode` fra felles kodeverk? Eller noe vi har funnet på selv? Dokumenter plz.
     */
    val kode: String,
    val hjemmel: String,
    val avslagstype: Avslagstype,
) {
    BRUKER_UNDER_18(kode = "11-4-1-1", hjemmel = "§ 11-4 1. ledd", Avslagstype.KUN_INNGANGSVILKÅR),
    BRUKER_OVER_67(kode = "11-4-1-2", hjemmel = "§ 11-4 1. ledd", Avslagstype.OPPHØR),
    MANGLENDE_DOKUMENTASJON(kode = "21-3", hjemmel = "§ 21-3, § 11-1", Avslagstype.UKJENT), // FIXME: Dette er neppe rett med 11-1,
    IKKE_RETT_PA_SYKEPENGEERSTATNING(kode = "11-13", hjemmel = "§ 11-13", Avslagstype.OPPHØR),
    IKKE_SYKDOM_AV_VISS_VARIGHET(kode = "11-5-1", "§ 11-5", Avslagstype.OPPHØR),
    IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL(kode = "11-5-1", "§ 11-5", Avslagstype.OPPHØR),
    IKKE_NOK_REDUSERT_ARBEIDSEVNE(kode = "11-5-1", "§ 11-5", Avslagstype.OPPHØR),
    IKKE_BEHOV_FOR_OPPFOLGING(kode = "11-6-1", "§ 11-6", Avslagstype.OPPHØR),
    IKKE_MEDLEM_FORUTGÅENDE(kode= "11-2", hjemmel = "§ 11-2", Avslagstype.KUN_INNGANGSVILKÅR),
    IKKE_MEDLEM(kode= "2-1", hjemmel = "§ 2-1", Avslagstype.UKJENT),
    IKKE_OPPFYLT_OPPHOLDSKRAV_EØS(kode = "11-3-1", hjemmel = "§ 11-3 1.ledd", Avslagstype.STANS),
    NORGE_IKKE_KOMPETENT_STAT(kode= "EØS-forordning 883. Art 11-3-E", hjemmel = "EØS-forordning 883. Art 11-3-E", Avslagstype.UKJENT),
    ANNEN_FULL_YTELSE(kode = "11-27", hjemmel = "§ 11-27", Avslagstype.OPPHØR),
    IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE(kode = "11-18", hjemmel = "§ 11-18", Avslagstype.OPPHØR),
    VARIGHET_OVERSKREDET_OVERGANG_UFORE(kode = "11-18", hjemmel = "§ 11-18 1. ledd", Avslagstype.OPPHØR),
    VARIGHET_OVERSKREDET_ARBEIDSSØKER(kode = "11-17", hjemmel = "§ 11-17", Avslagstype.OPPHØR),
    IKKE_RETT_PA_AAP_I_PERIODE_SOM_ARBEIDSSOKER(kode = "11-17", hjemmel = "§ 11-17", Avslagstype.STANS)
}

/** [Avslagstype] sier hvordan en [Avslagsårsak] skal tolkes når
 * avslagsårsaken er den utslagsgivende årsaken til at medlemmet *mister* retten
 * til AAP. Merk at avslagstypen ikke sier noe om hvordan avslagsårsaken
 * skal forstås hvis den er medvirkende til at medlemmet ikke får innvilget AAP.
 */
enum class Avslagstype {
    STANS,
    OPPHØR,
    KUN_INNGANGSVILKÅR,
    UKJENT,
}