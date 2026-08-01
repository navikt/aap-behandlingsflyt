package no.nav.aap.behandlingsflyt.steg.samordning.arbeidsgiver

import no.nav.aap.komponenter.type.Periode


data class SamordningArbeidsgiverVurderingerDTO(
    val begrunnelse: String,
    val perioder: List<Periode>,
)

