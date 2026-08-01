package no.nav.aap.samordning.annenfullytelse

import no.nav.aap.samordning.avslag11_27.Avslag11_27Grunnlag
import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.samordning.SamordningYtelseVurderingGrunnlag
import no.nav.aap.samordning.SamordningUføreGrunnlag
import no.nav.aap.beregning.UføreGrunnlag
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.krav.KravGrunnlag

data class SamordningAnnenFullYtelseFaktagrunnlag(
    val rettighetsperiode: Periode,
    val samordningGrunnlag: SamordningYtelseVurderingGrunnlag?,
    val uføreRegisterGrunnlag: UføreGrunnlag?,
    val uføreVurderingGrunnlag: SamordningUføreGrunnlag?,
    val avslag1127grunnlag: Avslag11_27Grunnlag?,
    val kravGrunnlag: KravGrunnlag?,
    val strekkAvslagOverHelger: Boolean
) : Faktagrunnlag