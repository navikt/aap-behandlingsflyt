package no.nav.aap.behandlingsflyt.vilkår.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.vilkår.Faktagrunnlag
import java.time.LocalDate

class SykepengerErstatningFaktagrunnlag(
    val vurderingsdato: LocalDate,
    val sisteDagMedMuligYtelse: LocalDate,
    val vurdering: SykepengerVurdering
) : Faktagrunnlag
