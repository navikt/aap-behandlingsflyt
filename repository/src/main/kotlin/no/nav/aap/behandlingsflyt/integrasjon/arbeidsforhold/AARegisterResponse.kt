package no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold

import java.time.LocalDate

data class ArbeidsforholdoversiktResponse(
    val arbeidsforholdoversikter: List<ArbeidsforholdOversikt> = emptyList()
)

data class ArbeidsforholdOversikt(
    val type: Kodeverksentitet,
    val arbeidssted: Arbeidssted,
    val startdato: LocalDate,
    val sluttdato: LocalDate?
)

data class Kodeverksentitet(
    val kode: String
)

data class Arbeidssted(
    val type: String,
    val identer: List<Ident>
)

data class Ident(
    val type: String,
    val ident: String
)