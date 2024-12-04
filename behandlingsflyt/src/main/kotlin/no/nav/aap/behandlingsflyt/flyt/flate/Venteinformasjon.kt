package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import java.time.LocalDate

class Venteinformasjon(val frist: LocalDate, val begrunnelse: String, val grunn: ÅrsakTilSettPåVent)