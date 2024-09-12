package no.nav.aap.behandlingsflyt.faktagrunnlag.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.komponenter.type.Periode

data class BruddAktivitetspliktRequest(
    val saksnummer: String,
    val brudd: AktivitetDtoType,
    val paragraf: Paragraf,
    val begrunnelse: String,
    val periode: Periode
)

data class BruddAktivitetspliktResponse(
    val hendelser: List<BruddAktivitetspliktHendelse>
)

data class HentHendelseDto(@PathParam("saksnummer") val saksnummer: String)

data class BruddAktivitetspliktHendelse(
    val brudd: AktivitetDtoType,
    val paragraf: Paragraf,
    val periode: Periode
)

enum class Paragraf {
    PARAGRAF_11_7,
    PARAGRAF_11_8,
    PARAGRAF_11_9
}

enum class AktivitetDtoType {
    IKKE_MØTT_TIL_MØTE,
    IKKE_MØTT_TIL_BEHANDLING,
    IKKE_MØTT_TIL_TILTAK,
    IKKE_MØTT_TIL_ANNEN_AKTIVITET,
    IKKE_SENDT_INN_DOKUMENTASJON,
    IKKE_AKTIVT_BIDRAG
}