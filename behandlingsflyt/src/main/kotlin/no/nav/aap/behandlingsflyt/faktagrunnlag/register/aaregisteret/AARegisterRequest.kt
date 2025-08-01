package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret

data class ArbeidsforholdRequest(
    val arbeidstakerId: String,
    val arbeidsforholdstatuser: List<String> = listOf(ARBEIDSFORHOLDSTATUSER.AKTIV.toString(), ARBEIDSFORHOLDSTATUSER.AVSLUTTET.toString()),
    val arbeidsforholdtyper: List<String> = listOf("ordinaertArbeidsforhold"),
    val historikk: Boolean = false
)

enum class ARBEIDSFORHOLDSTATUSER{
    AKTIV,
    AVSLUTTET
}