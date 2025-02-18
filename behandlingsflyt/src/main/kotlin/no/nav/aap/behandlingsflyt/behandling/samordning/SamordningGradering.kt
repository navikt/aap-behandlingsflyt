package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.komponenter.verdityper.Prosent

data class SamordningGradering(val gradering: Prosent, val ytelsesGraderinger: List<YtelseGradering>)

data class YtelseGradering(val ytelse: Ytelse, val gradering: Prosent)