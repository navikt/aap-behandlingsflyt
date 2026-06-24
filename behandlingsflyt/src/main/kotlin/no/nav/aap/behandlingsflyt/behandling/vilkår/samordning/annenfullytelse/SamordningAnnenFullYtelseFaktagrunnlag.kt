package no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse

import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreGrunnlag
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SamordningAvslagGrunnlag
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent

class SamordningAnnenFullYtelseFaktagrunnlag(
    val rettighetsperiode: Periode,
    val samordningTidslinje: Tidslinje<SamordningGradering>,
    val uføreTidslinje: Tidslinje<Prosent>,
    val samordningAvslagGrunnlag: SamordningAvslagGrunnlag,
) : Faktagrunnlag