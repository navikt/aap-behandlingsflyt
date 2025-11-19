package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

/**
 * Hvilken hjemmel gis AAP etter? Brukes til kvoter og til statistikk-formål.
 *
 * @param hjemmel Hvilken paragraf
 */
enum class RettighetsType(val hjemmel: String) {
    BISTANDSBEHOV(hjemmel = "§ 11-6"),
    SYKEPENGEERSTATNING(hjemmel = "§ 11-13"),
    STUDENT(hjemmel = "§ 11-14"),
    ARBEIDSSØKER(hjemmel = "§ 11-17"),
    VURDERES_FOR_UFØRETRYGD(hjemmel = "§ 11-18"),
}