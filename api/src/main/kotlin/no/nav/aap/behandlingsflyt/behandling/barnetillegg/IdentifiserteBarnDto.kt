package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.barnetillegg.Relasjon
import no.nav.aap.misc.Ident
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