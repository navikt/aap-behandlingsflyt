package no.nav.aap.behandlingsflyt.behandling.etannetsted

import no.nav.aap.komponenter.type.Periode

data class EtAnnetSted (
    val periode: Periode,
    val soning: Soning? = null,
    val institusjon: Institusjon? = null,
)

data class Soning(
    val soner: Boolean,
    val girOpphør: Boolean,
)

data class Institusjon(
    val erPåInstitusjon: Boolean,
    val skalGiReduksjon: Boolean,
    val skalGiUmiddelbarReduksjon: Boolean
)