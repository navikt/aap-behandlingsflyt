package no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.flyt.vilkår.Faktagrunnlag
import java.time.LocalDate

class SykepengerErstatningFaktagrunnlag(
    val vurderingsdato: LocalDate,
    val sisteDagMedMuligYtelse: LocalDate,
    val vurdering: SykepengerVurdering
) : Faktagrunnlag
