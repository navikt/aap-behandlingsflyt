package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

class IdentifiserteBarnDto(val ident: Ident, val forsorgerPeriode: Periode, val fødselsdato: LocalDate)