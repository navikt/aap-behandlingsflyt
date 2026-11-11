package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import java.time.format.DateTimeFormatter

object SykdomsvurderingForBrevFormatter {
    private val datoformat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun formater(grunnlag: SykdomsvurderingForBrev): String? {
        val vurdering = grunnlag.vurdering
        if (vurdering.isNullOrBlank() || grunnlag.kilder.isEmpty()) {
            return vurdering
        }

        val kildeliste = grunnlag.kilder.joinToString("\n") {
            "- ${it.dokumentnavn} (${it.journalføringsdato.format(datoformat)})"
        }
        return "$vurdering\n\nVi har lagt vekt på følgende dokumenter i vurderingen:\n$kildeliste"
    }
}
