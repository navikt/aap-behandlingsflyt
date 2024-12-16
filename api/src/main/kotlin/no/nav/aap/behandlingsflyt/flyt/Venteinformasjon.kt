package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import java.time.LocalDate

data class Venteinformasjon(
    val definisjon: Definisjon,
    val frist: LocalDate,
    val begrunnelse: String,
    val grunn: ÅrsakTilSettPåVent
)