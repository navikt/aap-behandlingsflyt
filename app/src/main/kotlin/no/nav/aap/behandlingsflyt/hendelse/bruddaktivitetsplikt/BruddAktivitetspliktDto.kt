package no.nav.aap.behandlingsflyt.hendelse.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Paragraf
import no.nav.aap.komponenter.type.Periode

data class BruddAktivitetspliktRequest(
    val saksnummer: String,
    val brudd: AktivitetType,
    val paragraf: Paragraf,
    val begrunnelse: String,
    val perioder: List<Periode>
)

data class BruddAktivitetspliktResponse(
    val hendelser: List<BruddAktivitetspliktHendelseDto>
)

data class HentHendelseDto(@PathParam("saksnummer") val saksnummer: String)

data class BruddAktivitetspliktHendelseDto(
    val brudd: AktivitetType,
    val paragraf: Paragraf,
    val periode: Periode,
    val begrunnelse: String,
    val hendelseId: String,
)
