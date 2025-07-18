package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import java.time.LocalDate

data class Ventebehov(val definisjon: Definisjon, val grunn: ÅrsakTilSettPåVent, val frist: LocalDate? = null) {
    init {
        if (frist != null) {
            require(frist.isAfter(LocalDate.now())) { "Ventefrist må være i framtiden. Fikk $frist." }
        }
    }
}