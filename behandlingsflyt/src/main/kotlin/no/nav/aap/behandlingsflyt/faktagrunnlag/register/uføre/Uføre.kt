package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate

data class Uføre(
    val virkningstidspunkt: LocalDate,
    val uføregrad: Prosent
)
