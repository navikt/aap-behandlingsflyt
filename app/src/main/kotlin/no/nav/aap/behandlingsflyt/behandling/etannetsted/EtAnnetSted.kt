package no.nav.aap.behandlingsflyt.behandling.etannetsted

import no.nav.aap.komponenter.type.Periode

data class EtAnnetSted (
    val periode: Periode,
    val soning: Soning,
    val institusjon: Institusjon,
    val begrunnelse: String
)

data class Soning(
    val soner: Boolean,
    val sonerFritt: Boolean
)

data class Institusjon(
    val erPåInstitusjon: Boolean,
    val forsørgerEktefelle: Boolean
)