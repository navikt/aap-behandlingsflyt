package no.nav.aap.misc

import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.samordning.Ytelse

data class SamordningGradering(val gradering: Prosent, val ytelsesGraderinger: List<YtelseGradering>)

data class YtelseGradering(val ytelse: Ytelse, val gradering: Prosent)