package no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumentasjonType

data class BestillLegeerklæringDto (
    val behandlerRef: String,
    val fritekst: String,
    val sakId: String,
    val dokumentasjonType: DokumentasjonType,
    val dialogmeldingVedlegg: ByteArray?
)

data class HentStatusLegeerklæring(@PathParam("saksnummer") val saksnummer: String)