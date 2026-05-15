package no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold

import java.time.LocalDate

data class ArbeidsforholdoversiktResponse(
    val arbeidsforholdoversikter: List<ArbeidsforholdOversikt> = emptyList()
)

data class ArbeidsforholdOversikt(
    val type: ArbeidsforholdTypeResponse,
    val arbeidssted: Arbeidssted,
    val startdato: LocalDate,
    val sluttdato: LocalDate? = null,
    val ansettelsesdetaljer: List<Ansettelsesdetalj> = emptyList(),
    val yrke: Kodeverksentitet? = null
)

data class ArbeidsforholdTypeResponse(
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

data class Ansettelsesdetalj(
    val fartsomraade: Kodeverksentitet? = null,
    val skipsregister: Kodeverksentitet? = null,
    val fartoeystype: Kodeverksentitet? = null,
    val yrke: Kodeverksentitet? = null
)

data class Kodeverksentitet(
    val kode: String? = null,
    val beskrivelse: String? = null
)