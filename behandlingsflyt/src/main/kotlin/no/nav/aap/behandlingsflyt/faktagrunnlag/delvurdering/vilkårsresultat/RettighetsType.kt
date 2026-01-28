package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote

/**
 * Hvilken hjemmel gis AAP etter? Brukes til kvoter og til statistikk-formål.
 *
 * @param hjemmel Hvilken paragraf
 */
enum class RettighetsType(val hjemmel: String, val kvote: Kvote?) {
    BISTANDSBEHOV(hjemmel = "§ 11-6", kvote = Kvote.ORDINÆR),
    SYKEPENGEERSTATNING(hjemmel = "§ 11-13", kvote = Kvote.SYKEPENGEERSTATNING),
    STUDENT(hjemmel = "§ 11-14", kvote = Kvote.ORDINÆR),
    ARBEIDSSØKER(hjemmel = "§ 11-17", kvote = null),
    VURDERES_FOR_UFØRETRYGD(hjemmel = "§ 11-18", kvote = null),
}