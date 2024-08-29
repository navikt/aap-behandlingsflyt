package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.verdityper.Prosent

data class SamordningGradering(val gradering: Prosent, val ytelsesGraderinger: List<YtelseGradering>)