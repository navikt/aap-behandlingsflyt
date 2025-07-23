package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class IdentifiserteBarnDto(
    val ident: Ident?,
    val fodselsDato: LocalDate?,
    val navn: String?,
    val forsorgerPeriode: Periode?
)