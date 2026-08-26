package no.nav.aap.behandlingsflyt.behandling.dialogmelding

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class HentDialogmeldingerForSakParams(
    @param:PathParam(description = "Saksnummer") val saksnummer: String,
)
