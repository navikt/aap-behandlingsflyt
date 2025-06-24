package no.nav.aap.behandlingsflyt.behandling.underveis

import com.papsign.ktor.openapigen.annotations.properties.description.Description
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
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
    val meldepliktStatus: MeldepliktStatus?,
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
        meldepliktStatus = underveisperiode.meldepliktStatus,
        avslagsårsak = underveisperiode.avslagsårsak,
        gradering = GraderingDto(underveisperiode.arbeidsgradering, underveisperiode.grenseverdi),
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
    constructor(arbeidsGradering: ArbeidsGradering, grenseverdi: Prosent) : this(
        gradering = arbeidsGradering.gradering.prosentverdi(),
        andelArbeid = arbeidsGradering.andelArbeid.prosentverdi(),
        fastsattArbeidsevne = arbeidsGradering.fastsattArbeidsevne.prosentverdi(),
        grenseverdi = grenseverdi.prosentverdi()
    )
}