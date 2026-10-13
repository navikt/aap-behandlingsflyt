package no.nav.aap.behandlingsflyt.datadeling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import java.time.LocalDateTime

enum class BehandlingHendelseNavn{
    SØKNAD_MOTTATT,
    UNDER_BEHANDLING,
    FERDIG_BEHANDLET,
    SØKNAD_TRUKKET,
    VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
    VENTER_PÅ_SVAR_FRA_BRUKER,
    VENTER_PÅ_OPPLYSNINGER,
    VENTER_PÅ_SVAR_PÅ_FORHÅNDSVARSEL,
    VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER,
}
enum class BehandlingHendelseType{
    SØKNAD,
    KLAGE
}
enum class BehandlingHendelseStatus{
    AKTIV,
    FULLFØRT,
    IKKE_PÅBEGYNT,
}
data class BehandlingHendelse(
    val navn: BehandlingHendelseNavn,
    val tidsstempel: LocalDateTime,
    val status: BehandlingHendelseStatus,
)

data class BehandlingVisning(
    val behandlingReferanse: BehandlingReferanse,
    val avsluttet: Boolean,
    val behandlingType: BehandlingHendelseType,
    val hendelser: List<BehandlingHendelse>
)

fun ventepunktTilehandlingHendelseNavn(ventepunkt: Avklaringsbehov): BehandlingHendelseNavn?{
    return when(ventepunkt.venteårsak()) {
        ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER -> BehandlingHendelseNavn.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER
        ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER -> BehandlingHendelseNavn.VENTER_PÅ_SVAR_FRA_BRUKER
        ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_PÅ_FORHÅNDSVARSEL -> BehandlingHendelseNavn.VENTER_PÅ_SVAR_PÅ_FORHÅNDSVARSEL
        ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER -> BehandlingHendelseNavn.VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER
        ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER -> BehandlingHendelseNavn.VENTER_PÅ_OPPLYSNINGER
        else -> null
    }
}