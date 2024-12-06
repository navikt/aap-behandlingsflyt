package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import java.time.LocalDate

data class Venteinformasjon(val frist: LocalDate, val begrunnelse: String, val grunn: ÅrsakTilSettPåVent)