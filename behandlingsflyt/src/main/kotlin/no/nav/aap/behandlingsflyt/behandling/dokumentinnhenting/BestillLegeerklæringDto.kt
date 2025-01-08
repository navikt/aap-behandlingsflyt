package no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumentasjonType
import no.nav.aap.tilgang.plugin.kontrakt.Saksreferanse
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
) : Saksreferanse {
    override fun hentSaksreferanse(): String {
        return saksnummer
    }
}

data class ForhåndsvisBrevRequest (
    val saksnummer: String,
    val fritekst: String,
    val veilederNavn: String,
    val dokumentasjonType: DokumentasjonType
) : Saksreferanse {
    override fun hentSaksreferanse(): String {
        return saksnummer
    }
}

data class HentStatusLegeerklæring(@PathParam("saksnummer") val saksnummer: String) : Saksreferanse {
    override fun hentSaksreferanse(): String {
        return saksnummer
    }
}

data class PurringLegeerklæring(
    @PathParam("dialogmeldinguuid") val dialogmeldingPurringUUID: UUID
)