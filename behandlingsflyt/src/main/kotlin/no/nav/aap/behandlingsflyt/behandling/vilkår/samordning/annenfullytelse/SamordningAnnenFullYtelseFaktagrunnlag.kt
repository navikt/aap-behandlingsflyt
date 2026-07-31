package no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse

import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.komponenter.type.Periode

data class SamordningAnnenFullYtelseFaktagrunnlag(
    val rettighetsperiode: Periode,
    val samordningGrunnlag: SamordningYtelseVurderingGrunnlag?,
    val uføreRegisterGrunnlag: UføreGrunnlag?,
    val uføreVurderingGrunnlag: SamordningUføreGrunnlag?,
    val avslag1127grunnlag: Avslag11_27Grunnlag?,
    val kravGrunnlag: KravGrunnlag?,
    val strekkAvslagOverHelger: Boolean
) : Faktagrunnlag