package no.nav.aap.behandlingsflyt.behandling.etannetsted

import no.nav.aap.komponenter.type.Periode

data class EtAnnetSted (
    val periode: Periode,
    val soning: Soning = Soning(),
    val institusjon: Institusjon = Institusjon(),
    val begrunnelse: String
)

data class Soning(
    val soner: Boolean = false,
    val formueUnderForvaltning: Boolean = false,
    val soningUtenforFengsel: Boolean = false,
    val arbeidUtenforAnstalt: Boolean = false,
)

data class Institusjon(
    val erPåInstitusjon: Boolean = false,
    val forsørgerEktefelle: Boolean = false,
    val harFasteKostnader: Boolean = false
)