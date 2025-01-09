package no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumentasjonType
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.tilgang.plugin.kontrakt.Behandlingsreferanse
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
) : Behandlingsreferanse {
    override fun hentAvklaringsbehovKode(): String? {
        return Definisjon.BESTILL_LEGEERKLÆRING.kode.toString()
    }

    override fun hentBehandlingsreferanse(): String {
        return behandlingsReferanse.toString()
    }
}

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