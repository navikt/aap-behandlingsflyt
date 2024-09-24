package no.nav.aap.behandlingsflyt.behandling.etannetsted

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.tidslinje.Segment

internal class EtAnnetStedInput(
    val institusjonsOpphold: List<Segment<Institusjon>>,
    val barnetillegg: List<BarnetilleggPeriode>
) {

    fun harUavklartSoningsforhold(): Boolean {
        return institusjonsOpphold.any { segment -> segment.verdi.type == Institusjonstype.FO }
    }



}