package no.nav.aap.behandlingsflyt.behandling.mellomlagring

import no.nav.aap.tilgang.plugin.kontrakt.Behandlingsreferanse
import java.util.UUID

data class MellomlagretVurderingRequest(
    val avklaringsbehovkode: String,
    val behandlingsReferanse: UUID,
    val data: String,
): Behandlingsreferanse {
    override fun behandlingsreferanseResolverInput(): String {
        return this.behandlingsReferanse.toString()
    }

    override fun hentAvklaringsbehovKode(): String? {
        return this.avklaringsbehovkode
    }
}