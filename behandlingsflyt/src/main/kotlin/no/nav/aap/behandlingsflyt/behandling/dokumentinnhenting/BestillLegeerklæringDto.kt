package no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumentasjonType
import java.util.*

data class BestillLegeerklæringDto (
    val behandlerRef: String,
    val behandlerNavn: String,
    val behandlerHprNr: String,
    val veilederNavn: String,
    val fritekst: String,
    val saksnummer: String,
    val dokumentasjonType: DokumentasjonType,
    val behandlingsReferanse: UUID
)

data class ForhåndsvisBrevRequest (
    val saksnummer: String,
    val fritekst: String,
    val veilederNavn: String,
    val dokumentasjonType: DokumentasjonType
)

data class HentStatusLegeerklæring(@PathParam("saksnummer") val saksnummer: String)

data class PurringLegeerklæring(
    @PathParam("dialogmeldinguuid") val dialogmeldingPurringUUID: UUID
)