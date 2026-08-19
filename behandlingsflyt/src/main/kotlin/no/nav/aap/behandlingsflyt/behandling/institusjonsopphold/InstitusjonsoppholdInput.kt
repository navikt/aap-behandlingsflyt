package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Helseoppholdvurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Soningsvurderinger
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode

internal class InstitusjonsoppholdInput(
    val rettighetsperiode: Periode,
    val institusjonsOpphold: List<Segment<Institusjon>>,
    val soningsvurderinger: Soningsvurderinger?,
    val barnetillegg: List<BarnetilleggPeriode>,
    val helsevurderinger: Helseoppholdvurderinger?
)