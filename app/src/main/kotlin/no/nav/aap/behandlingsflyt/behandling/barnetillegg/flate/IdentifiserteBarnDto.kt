package no.nav.aap.behandlingsflyt.behandling.barnetillegg.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import java.time.LocalDate

class IdentifiserteBarnDto(val ident: Ident, val forsorgerPeriode: Periode, val fødselsdato: LocalDate)