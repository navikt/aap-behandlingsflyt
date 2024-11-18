package no.nav.aap.behandlingsflyt.behandling.etannetsted

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.tidslinje.Segment

internal class EtAnnetStedInput(
    val rettighetsperiode: Periode,
    val institusjonsOpphold: List<Segment<Institusjon>>,
    val soningsvurderinger: List<Soningsvurdering>,
    val barnetillegg: List<BarnetilleggPeriode>,
    val helsevurderinger: List<HelseinstitusjonVurdering>
)