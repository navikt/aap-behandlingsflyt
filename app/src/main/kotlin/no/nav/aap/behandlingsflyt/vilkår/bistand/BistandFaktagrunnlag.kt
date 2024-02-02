package no.nav.aap.behandlingsflyt.vilkår.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.vilkårsresultat.Faktagrunnlag
import java.time.LocalDate

class BistandFaktagrunnlag(
    val vurderingsdato: LocalDate,
    val sisteDagMedMuligYtelse: LocalDate,
    val vurdering: BistandVurdering?,
    val studentvurdering: StudentVurdering?,
) : Faktagrunnlag
