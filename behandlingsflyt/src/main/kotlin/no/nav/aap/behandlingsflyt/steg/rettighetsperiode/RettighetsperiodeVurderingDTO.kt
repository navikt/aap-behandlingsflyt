package no.nav.aap.behandlingsflyt.steg.rettighetsperiode

import java.time.LocalDate
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.krav.RettighetsperiodeHarRett

data class RettighetsperiodeVurderingDTO(
    val startDato: LocalDate?,
    val begrunnelse: String,
    val harRett: RettighetsperiodeHarRett
) {
    init {
        if (harRett.harOverstyrt() && startDato == null) {
            throw UgyldigForespørselException("Må sette startdato når bruker har rett utover søknadsdatoen")
        }
        if (!harRett.harOverstyrt() && startDato != null) {
            throw UgyldigForespørselException("Kan ikke sette startdato når bruker ikke har rett utover søknadsdatoen")
        }
    }
}