package no.nav.aap.behandlingsflyt.steg.institusjon

import no.nav.aap.barnetillegg.BarnetilleggPeriode
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.misc.institusjonsopphold.Helseoppholdvurderinger
import no.nav.aap.misc.institusjonsopphold.Institusjon
import no.nav.aap.misc.institusjonsopphold.Soningsvurderinger

internal class InstitusjonsoppholdInput(
    val rettighetsperiode: Periode,
    val institusjonsOpphold: List<Segment<Institusjon>>,
    val soningsvurderinger: Soningsvurderinger?,
    val barnetillegg: List<BarnetilleggPeriode>,
    val helsevurderinger: Helseoppholdvurderinger?
)