package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.VirksomhetEtableringResultat
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

sealed interface TilOgMedBeregning {
    data class Beregnet(val tom: LocalDate) : TilOgMedBeregning
    data class Feil(val feilmelding: String, val feilObjekt: VirksomhetEtableringResultat.Ugyldig) :
        TilOgMedBeregning
}

fun beregnTomForSistePeriode(
    vurderinger: List<EtableringEgenVirksomhetVurdering>,
    sisteVurdering: EtableringEgenVirksomhetVurdering
): TilOgMedBeregning {
    val fase = requireNotNull(sisteVurdering.fase)

    val maksHverdager = fase.maksHverdager
    val brukteHverdager = vurderinger
        .gjeldendeVurderinger()
        .segmenter()
        .filter {
            it.verdi.fase == fase &&
                    it.periode.fom.isBefore(sisteVurdering.fom)
        }
        .sumOf {
            Periode(
                it.periode.fom,
                minOf(it.periode.tom, sisteVurdering.fom.minusDays(1))
            ).antallHverdager().asInt
        }

    val gjenståendeHverdager = maksHverdager - brukteHverdager
    if (gjenståendeHverdager <= 0) {
        return TilOgMedBeregning.Feil(
            "Kvoten for $fase er brukt opp. Brukt: $brukteHverdager, maks: $maksHverdager",
            VirksomhetEtableringResultat.Ugyldig.OverstigerKvoter(
                fase = fase,
                brukteDager = brukteHverdager
            )
        )
    }

    return TilOgMedBeregning.Beregnet(Hverdager(gjenståendeHverdager).fraOgMed(sisteVurdering.fom))
}
