package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Endring
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon

class DefinisjonEndring(val definisjon: Definisjon, val endring: Endring) : Comparable<DefinisjonEndring> {
    override fun compareTo(other: DefinisjonEndring): Int {
        return endring.compareTo(other.endring)
    }
}