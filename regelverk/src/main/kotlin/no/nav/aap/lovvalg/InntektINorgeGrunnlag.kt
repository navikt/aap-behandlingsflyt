package no.nav.aap.lovvalg

import java.time.LocalDate
import no.nav.aap.komponenter.type.Periode

data class InntektINorgeGrunnlag(
    val identifikator: String,
    val beloep: Double,
    val skattemessigBosattLand: String?,
    val opptjeningsLand: String?,
    val inntektType: String?,
    val periode: Periode,
    val organisasjonsNavn: String?
)

data class ArbeidINorgeGrunnlag(
    val identifikator: String,
    val arbeidsforholdKode: Arbeidsforholdtype,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val organisasjonsNavn: String? = null,
    val ansettelsesdetaljer: List<ArbeidAnsettelsesdetaljGrunnlag> = emptyList()
)

data class ArbeidAnsettelsesdetaljGrunnlag(
    val skipsregister: Skipsregister? = null,
    val skipstype: Skipstype? = null,
    val fartsomraade: Fartsomraade? = null,
    val yrke: Yrke? = null,
)

enum class Skipsregister(val kode: String) {
    NIS("nis"),
    NOR("nor"),
    UTL("utl");

    companion object {
        fun fraKode(kode: String): Skipsregister =
            entries.first { it.kode == kode }
    }
}

enum class Skipstype(val kode: String) {
    ANNET("annet"),
    BOREPLATTFORM("boreplattform"),
    TURIST("turist");

    companion object {
        fun fraKode(kode: String): Skipstype =
            entries.first { it.kode == kode }
    }
}

enum class Fartsomraade(val kode: String) {
    INNENRIKS("innenriks"),
    UTENRIKS("utenriks");

    companion object {
        fun fraKode(kode: String): Fartsomraade =
            entries.first { it.kode == kode }
    }
}

data class Yrke(val kode: String, val beskrivelse: String? = null)

enum class Arbeidsforholdtype(val kode: String) {
    ORDINAERT_ARBEIDSFORHOLD("ordinaertArbeidsforhold"),
    MARITIMT_ARBEIDSFORHOLD("maritimtArbeidsforhold");

    companion object {
        fun fraKode(kode: String): Arbeidsforholdtype =
            entries.first { it.kode == kode }
    }
}

data class EnhetGrunnlag(
    val orgnummer: String,
    val orgNavn: String
)

enum class InntektTyper {
    SYKEPENGER,
    SYKEPENGERTILFISKERSOMBAREHARHYRE,
    SYKEPENGERTILDAGMAMMA,
    SYKEPENGERTILFISKER,
    SYKEPENGERTILJORDOGSKOGBRUKERE,
    FERIEPENGERSYKEPENGERTILFISKERSOMBAREHARHYRE,
}