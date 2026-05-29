package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.dokumentinnhenting.kontrakt.DokumentasjonType
import no.nav.aap.tilgang.Rolle
import no.nav.aap.tilgang.plugin.kontrakt.Behandlingsreferanse
import no.nav.aap.tilgang.plugin.kontrakt.Saksreferanse
import java.util.*

data class BestillLegeerklæringDto(
    val behandlerRef: String,
    val behandlerNavn: String,
    val behandlerHprNr: String,
    val fritekst: String,
    val saksnummer: String,
    val dokumentasjonType: DokumentasjonType,
    val behandlingsReferanse: UUID
) : Behandlingsreferanse {
    override fun hentPåkrevdRolle(): List<Rolle> {
        return Definisjon.BESTILL_LEGEERKLÆRING.løsesAv
    }

    override fun behandlingsreferanseResolverInput(): String {
        return behandlingsReferanse.toString()
    }
}

data class ForhåndsvisBrevRequest(
    val saksnummer: String,
    val fritekst: String,
    val dokumentasjonType: DokumentasjonType
) : Saksreferanse {
    override fun hentSaksreferanse(): String {
        return saksnummer
    }
}

data class HentStatusLegeerklæring(@param:PathParam("saksnummer") val saksnummer: String)

data class PurringLegeerklæringRequest(
    val dialogmeldingPurringUUID: UUID,
    val behandlingsReferanse: UUID
) : Behandlingsreferanse {
    override fun hentPåkrevdRolle(): List<Rolle> {
        return Definisjon.BESTILL_LEGEERKLÆRING.løsesAv
    }

    override fun behandlingsreferanseResolverInput(): String {
        return behandlingsReferanse.toString()
    }
}

