package no.nav.aap.aktivitetsplikt

import java.time.LocalDate
import no.nav.aap.misc.Faktagrunnlag

data class AktivitetspliktvilkåretGrunnlag(
    val aktivitetsplikt117grunnlag: Aktivitetsplikt11_7Grunnlag,
    val vurderFra: LocalDate,
) : Faktagrunnlag