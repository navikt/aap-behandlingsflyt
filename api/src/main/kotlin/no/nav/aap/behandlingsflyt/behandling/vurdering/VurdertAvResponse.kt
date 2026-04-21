package no.nav.aap.behandlingsflyt.behandling.vurdering

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class VurdertAvResponse(
    val ident: String,
    val dato: LocalDate,
    val ansattnavn: String? = null,
    val enhetsnavn: String? = null,
    val erRetur: Boolean? = null,
) {
    companion object {
        fun fraIdent(
            ident: String?,
            dato: LocalDate?,
            ansattInfoService: AnsattInfoService,
            erRetur: Boolean? = null
        ): VurdertAvResponse? {
            if (ident == null || dato == null) return null

            val navnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(ident)
            return VurdertAvResponse(
                ident = ident,
                dato = dato,
                ansattnavn = navnOgEnhet?.navn,
                enhetsnavn = navnOgEnhet?.enhet,
                erRetur = erRetur
            )
        }

        fun fraIdent(
            ident: String?,
            dato: Instant?,
            ansattInfoService: AnsattInfoService,
            erRetur: Boolean? = null
        ): VurdertAvResponse? {
            if (ident == null || dato == null) return null
            return fraIdent(
                ident = ident,
                dato = dato.atZone(ZoneId.of("Europe/Oslo")).toLocalDate(),
                ansattInfoService = ansattInfoService,
                erRetur = erRetur
            )
        }
    }
}
