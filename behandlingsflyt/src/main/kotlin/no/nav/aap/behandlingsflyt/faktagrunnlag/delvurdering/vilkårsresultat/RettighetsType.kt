package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

/**
 * Hvilken hjemmel gis AAP etter? Brukes til kvoter og til statistikk-formål.
 *
 * @param hjemmel Hvilken paragraf
 */
enum class RettighetsType(val hjemmel: String) {
    BISTANDSBEHOV(hjemmel = "§ 11-6"),
    // TODO: Utkommenter disse når vi får implementert disse paragrafene.
    //       Oppdater også tabell her https://confluence.adeo.no/pages/viewpage.action?pageId=566090100
    //    BISTANDSBEHOV_ARBEIDSUTPROVING(hjemmel = "§ 11-6 + 11-23 sjette ledd"),
    //    BISTANDSBEHOV_ETABLERING(hjemmel = "§ 11-15"),
    //    BISTANDSBEHOV_UTVIKLINGFASE(hjemmel = "§ 11-15"),
    SYKEPENGEERSTATNING(hjemmel = "§ 11-13"),
    STUDENT(hjemmel = "§ 11-14"),
    ARBEIDSSØKER(hjemmel = "§ 11-17"),
    VURDERES_FOR_UFØRETRYGD(hjemmel = "§ 11-18"),
}