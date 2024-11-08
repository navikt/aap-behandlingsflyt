package no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumentasjonType

data class BestillLegeerklæringDto (
    val behandlerRef: String,
    val behandlerNavn: String,
    val veilederNavn: String,
    val fritekst: String,
    val saksnummer: String,
    val dokumentasjonType: DokumentasjonType,
    val dialogmeldingVedlegg: ByteArray?
)

data class ForhåndsvisBrevRequest (
    val saksnummer: String,
    val personIdent: String,
    val fritekst: String,
    val veilederNavn: String,
    val dokumentasjonType: DokumentasjonType
)

data class HentStatusLegeerklæring(@PathParam("saksnummer") val saksnummer: String)