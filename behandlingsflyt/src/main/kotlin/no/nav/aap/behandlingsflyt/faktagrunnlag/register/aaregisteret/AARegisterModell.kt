package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret

import java.time.LocalDate

data class ArbeidsforholdRequest(
    val arbeidstakerId: String,
    val arbeidsforholdtyper: List<String> = listOf("ordinaertArbeidsforhold"),
    val arbeidsforholdstatuser: List<String> = listOf(ARBEIDSFORHOLDSTATUSER.AKTIV.toString(), ARBEIDSFORHOLDSTATUSER.AVSLUTTET.toString()),
)

data class ArbeidsforholdoversiktResponse(
    val arbeidsforholdoversikter: List<ArbeidsforholdOversikt> = listOf()
)

data class ArbeidsforholdOversikt(
    val type: Kodeverksentitet,
    val arbeidssted: Arbeidssted,
    val startdato: LocalDate,
    val sluttdato: LocalDate? = null,
)

data class Kodeverksentitet(
    val kode: String
)


data class Arbeidssted(
    val type: String,
    val identer: List<Ident>,
)

data class Ident(
    val type: String,
    val ident: String
)

enum class ARBEIDSFORHOLDSTATUSER{
    AKTIV,
    AVSLUTTET
}