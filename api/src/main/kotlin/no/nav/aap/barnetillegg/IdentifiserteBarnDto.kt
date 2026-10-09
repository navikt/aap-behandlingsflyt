package no.nav.aap.barnetillegg

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class IdentifiserteBarnDto(
    val ident: Ident?,
    val fodselsDato: LocalDate?,
    val dodsDato: LocalDate?,
    val navn: String?,
    val forsorgerPeriode: Periode?,
    val oppgittForeldreRelasjon: Relasjon? = null
)