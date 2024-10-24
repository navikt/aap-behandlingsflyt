package no.nav.aap.behandlingsflyt.hendelse.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt
import no.nav.aap.komponenter.type.Periode

data class BruddAktivitetspliktRequest(
    val saksnummer: String,
    val brudd: BruddAktivitetsplikt.Brudd,
    val paragraf: BruddAktivitetsplikt.Paragraf,
    val begrunnelse: String,
    val grunn: BruddAktivitetsplikt.Grunn?,
    val perioder: List<Periode>
)

data class BruddAktivitetspliktResponse(
    val hendelser: List<BruddAktivitetspliktHendelseDto>
)

data class HentHendelseDto(@PathParam("saksnummer") val saksnummer: String)

data class BruddAktivitetspliktHendelseDto(
    val brudd: BruddAktivitetsplikt.Brudd,
    val paragraf: BruddAktivitetsplikt.Paragraf,
    val periode: Periode,
    val begrunnelse: String,
    val hendelseId: String,
)
