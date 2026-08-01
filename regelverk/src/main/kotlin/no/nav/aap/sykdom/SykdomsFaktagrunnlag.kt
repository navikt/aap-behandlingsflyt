package no.nav.aap.sykdom

import java.time.LocalDate
import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.bistand.BistandGrunnlag
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.sykepengererstatning.SykepengerErstatningGrunnlag

class SykdomsFaktagrunnlag(
    val kravDato: LocalDate,
    val sisteDagMedMuligYtelse: LocalDate,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykepengerErstatningFaktagrunnlag: SykepengerErstatningGrunnlag?,
    val sykdomsvurderinger: List<Sykdomsvurdering>,
    val bistandvurderingFaktagrunnlag: BistandGrunnlag?,
    val sykepengeerstatningVilkår: Tidslinje<Vilkårsvurdering>,
) : Faktagrunnlag