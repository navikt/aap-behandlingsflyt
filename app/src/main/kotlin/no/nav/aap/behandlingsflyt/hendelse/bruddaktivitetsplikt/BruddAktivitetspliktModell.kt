package no.nav.aap.behandlingsflyt.hendelse.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.komponenter.type.Periode
import java.util.*

data class BruddAktivitetspliktRequest(
    val saksnummer: String,
    val brudd: AktivitetTypeDto,
    val paragraf: ParagrafDto,
    val begrunnelse: String,
    val perioder: List<Periode>
)

data class BruddAktivitetspliktResponse(
    val hendelser: List<BruddAktivitetspliktHendelseDto>
)

data class HentHendelseDto(@PathParam("saksnummer") val saksnummer: String)

data class BruddAktivitetspliktHendelseDto(
    val brudd: AktivitetTypeDto,
    val paragraf: ParagrafDto,
    val periode: Periode,
    val begrunnelse: String,
    val hendelseId: UUID
)

enum class ParagrafDto {
    PARAGRAF_11_7,
    PARAGRAF_11_8,
    PARAGRAF_11_9
}

enum class AktivitetTypeDto {
    IKKE_MØTT_TIL_MØTE,
    IKKE_MØTT_TIL_BEHANDLING,
    IKKE_MØTT_TIL_TILTAK,
    IKKE_MØTT_TIL_ANNEN_AKTIVITET,
    IKKE_SENDT_INN_DOKUMENTASJON,
    IKKE_AKTIVT_BIDRAG
}