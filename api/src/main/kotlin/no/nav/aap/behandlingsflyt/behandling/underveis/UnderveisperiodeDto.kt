package no.nav.aap.behandlingsflyt.behandling.underveis

import com.papsign.ktor.openapigen.annotations.properties.description.Description
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Gradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent

data class UnderveisperiodeDto(
    val periode: Periode,
    val meldePeriode: Periode,
    val utfall: Utfall,
    val rettighetsType: RettighetsType?,
    val avslagsårsak: UnderveisÅrsak?,
    val gradering: GraderingDto,
    val trekk: Dagsatser,
    val brukerAvKvoter: List<Kvote>,
) {
    constructor(underveisperiode: Underveisperiode) : this(
        periode = underveisperiode.periode,
        meldePeriode = underveisperiode.meldePeriode,
        utfall = underveisperiode.utfall,
        rettighetsType = underveisperiode.rettighetsType,
        avslagsårsak = underveisperiode.avslagsårsak,
        gradering = GraderingDto(underveisperiode.gradering, underveisperiode.grenseverdi),
        trekk = Dagsatser(underveisperiode.trekk.antall * underveisperiode.periode.antallDager()),
        brukerAvKvoter = underveisperiode.brukerAvKvoter.toList(),
    )
}

data class GraderingDto(
    @property:Description("Gradering (i prosent) før vurdering av grenseverdi")
    val gradering: Int,
    @property:Description("Hvor mye man har jobbet i en meldeperiode i prosent")
    val andelArbeid: Int,
    @property:Description("Vurdert arbeidsevne (i prosent) av veileder")
    val fastsattArbeidsevne: Int,
    @property:Description("Maksverdi for endelig gradering (i prosent)")
    val grenseverdi: Int
) {
    constructor(gradering: Gradering, grenseverdi: Prosent) : this(
        gradering = gradering.gradering.prosentverdi(),
        andelArbeid = gradering.andelArbeid.prosentverdi(),
        fastsattArbeidsevne = gradering.fastsattArbeidsevne.prosentverdi(),
        grenseverdi = grenseverdi.prosentverdi()
    )
}

/*
class Underveisperiode(
    val periode: Periode,
    val meldePeriode: Periode?,
    val utfall: Utfall,
    val avslagsårsak: UnderveisÅrsak?,
    val grenseverdi: Prosent,
    val gradering: Gradering?,
    val trekk: Dagsatser
)
 */