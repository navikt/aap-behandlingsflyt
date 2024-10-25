package no.nav.aap.behandlingsflyt.hendelse.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt
import no.nav.aap.komponenter.type.Periode

data class BruddAktivitetspliktRequest(
    val saksnummer: String,
    val brudd: BruddAktivitetsplikt.Brudd,
    val paragraf: Paragraf?,
    val begrunnelse: String,
    val grunn: BruddAktivitetsplikt.Grunn?,
    val perioder: List<Periode>,
    val dokumenttype: BruddAktivitetsplikt.Dokumenttype,
)

enum class Paragraf(val somDomene: BruddAktivitetsplikt.Paragraf) {
    PARAGRAF_11_8(BruddAktivitetsplikt.Paragraf.PARAGRAF_11_8),
    PARAGRAF_11_9(BruddAktivitetsplikt.Paragraf.PARAGRAF_11_9),
}

data class BruddAktivitetspliktResponse(
    val hendelser: List<BruddAktivitetspliktHendelseDto>
)

data class HentHendelseDto(@PathParam("saksnummer") val saksnummer: String)

data class BruddAktivitetspliktHendelseDto(
    val brudd: BruddAktivitetsplikt.Brudd,
    val paragraf: BruddAktivitetsplikt.Paragraf,
    val grunn: BruddAktivitetsplikt.Grunn,
    val periode: Periode,
)
