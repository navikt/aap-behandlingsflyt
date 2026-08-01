package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret

import no.nav.aap.lovvalg.Arbeidsforholdtype

data class ArbeidsforholdRequest(
    val arbeidstakerId: String,
    val arbeidsforholdstatuser: List<String> = listOf(
        ARBEIDSFORHOLDSTATUSER.AKTIV.toString(),
        ARBEIDSFORHOLDSTATUSER.AVSLUTTET.toString()
    ),
    val arbeidsforholdtyper: List<String> = listOf(
        Arbeidsforholdtype.ORDINAERT_ARBEIDSFORHOLD.kode,
        Arbeidsforholdtype.MARITIMT_ARBEIDSFORHOLD.kode
    ),
    val historikk: Boolean = false
)

enum class ARBEIDSFORHOLDSTATUSER {
    AKTIV,
    AVSLUTTET
}