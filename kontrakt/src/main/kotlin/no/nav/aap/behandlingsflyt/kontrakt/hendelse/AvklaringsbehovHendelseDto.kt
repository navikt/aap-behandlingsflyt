package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @param definisjon Hva slags avklaringsbehov denne hendelsen gjelder.
 * @param status Status for avklaringsbehovet.
 * @param endringer Alle endringer som har skjedd på en gitt behandling.
 */
public data class AvklaringsbehovHendelseDto(
    val definisjon: DefinisjonDTO,
    val status: Status,
    val endringer: List<EndringDTO>
)

public data class DefinisjonDTO(
    val type: AvklaringsbehovKode,
    val behovType: Definisjon.BehovType,
    val løsesISteg: StegType
)

public data class EndringDTO(
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val frist: LocalDate? = null,
    val endretAv: String,
    val årsakTilSattPåVent: ÅrsakTilSettPåVent? = null
)

public enum class ÅrsakTilSettPåVent {
    VENTER_PÅ_OPPLYSNINGER,
    VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER,
    VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
    VENTER_PÅ_VURDERING_AV_ROL,
    VENTER_PÅ_SVAR_FRA_BRUKER,
    VENTER_PÅ_MASKINELL_AVKLARING
}