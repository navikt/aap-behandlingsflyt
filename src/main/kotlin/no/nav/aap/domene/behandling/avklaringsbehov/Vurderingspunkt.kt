package no.nav.aap.domene.behandling.avklaringsbehov

import no.nav.aap.flyt.StegStatus

enum class Vurderingspunkt(val stegStatus: StegStatus) {
    INN(StegStatus.INNGANG),
    UT(StegStatus.UTGANG)
}
