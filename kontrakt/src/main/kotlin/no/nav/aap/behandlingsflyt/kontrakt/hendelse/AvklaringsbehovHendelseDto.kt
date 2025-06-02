package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param avklaringsbehovDefinisjon Hva slags avklaringsbehov denne hendelsen gjelder.
 * @param status Status for avklaringsbehovet.
 * @param endringer Alle endringer som har skjedd på en gitt behandling.
 * @param typeBrev Type brev relatert til et avklaringsbehov. Relevant for `Definisjon.SKRIV_BREV`
 */
public data class AvklaringsbehovHendelseDto(
    val avklaringsbehovDefinisjon: Definisjon,
    val status: Status,
    val endringer: List<EndringDTO>,
    val typeBrev: TypeBrev? = null,
)

public enum class TypeBrev {
    VEDTAK_AVSLAG,
    VEDTAK_INNVILGELSE,
    VEDTAK_ENDRING,
    VARSEL_OM_BESTILLING,
    FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
    KLAGE_AVVIST,
    KLAGE_OPPRETTHOLDELSE,
    KLAGE_TRUKKET,
    FORHÅNDSVARSEL_KLAGE_FORMKRAV;
}

public data class EndringDTO(
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val frist: LocalDate? = null,
    val endretAv: String,
    val årsakTilSattPåVent: ÅrsakTilSettPåVent? = null,
    val årsakTilRetur: List<ÅrsakTilRetur> = emptyList(),
    val begrunnelse: String? = "",
)

public data class ÅrsakTilRetur(val årsak: ÅrsakTilReturKode)

public enum class ÅrsakTilReturKode {
    MANGELFULL_BEGRUNNELSE,
    MANGLENDE_UTREDNING,
    FEIL_LOVANVENDELSE,
    ANNET
}


public enum class ÅrsakTilSettPåVent {
    VENTER_PÅ_OPPLYSNINGER,
    VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER,
    VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
    VENTER_PÅ_VURDERING_AV_ROL,
    VENTER_PÅ_SVAR_FRA_BRUKER,
    VENTER_PÅ_MASKINELL_AVKLARING,
    VENTER_PÅ_UTENLANDSK_VIDEREFORING_AVKLARING,
    VENTER_PÅ_KLAGE_IMPLEMENTASJON,
    VENTER_PÅ_SVAR_PÅ_FORHÅNDSVARSEL,
    VENTER_PÅ_FUNKSJONALITET
}