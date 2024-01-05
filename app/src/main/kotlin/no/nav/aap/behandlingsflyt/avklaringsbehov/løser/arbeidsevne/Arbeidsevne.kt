package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.arbeidsevne

import no.nav.aap.behandlingsflyt.verdityper.Prosent

data class Arbeidsevne(
    val begrunnelse: String,
    val andelNedsattArbeidsevne: Prosent
)
