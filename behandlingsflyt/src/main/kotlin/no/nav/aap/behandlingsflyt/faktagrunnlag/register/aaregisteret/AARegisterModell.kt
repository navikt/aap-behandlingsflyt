package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret

import java.time.LocalDate

data class ArbeidsforholdRequest(
    val arbeidstakerId: String,
    val arbeidsforholdtyper: List<String> = listOf("ordinaertArbeidsforhold"),
    val arbeidsforholdstatuser: List<String> = listOf(ARBEIDSFORHOLDSTATUSER.AKTIV.toString(), ARBEIDSFORHOLDSTATUSER.AVSLUTTET.toString()),
)

data class ArbeidsforholdoversiktResponse(
    val arbeidsforholdoversikter: List<ArbeidsforholdOversikt>,
)

data class ArbeidsforholdOversikt(
    val type: Kodeverksentitet,
    val arbeidstaker: Arbeidstaker,
    val arbeidssted: Arbeidssted,
    val opplysningspliktig: Opplysningspliktig,
    val startdato: LocalDate,
    val sluttdato: LocalDate? = null,
    val yrke: Kodeverksentitet,
    val avtaltStillingsprosent: Int,
    val permisjonsprosent: Int? = null,
    val permitteringsprosent: Int? = null,
)

data class Kodeverksentitet(
    val kode: String,
    val beskrivelse: String,
)

data class Arbeidstaker(
    val identer: List<Ident>,
)

data class Arbeidssted(
    val type: String,
    val identer: List<Ident>,
)

data class Opplysningspliktig(
    val type: String,
    val identer: List<Ident>,
)

data class Ident(
    val type: String,
    val ident: String,
    val gjeldende: Boolean? = null,
)

enum class ARBEIDSFORHOLDSTATUSER{
    AKTIV,
    AVSLUTTET
}