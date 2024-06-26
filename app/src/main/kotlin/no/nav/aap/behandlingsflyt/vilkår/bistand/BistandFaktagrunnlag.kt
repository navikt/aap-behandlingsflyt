package no.nav.aap.behandlingsflyt.vilkår.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.faktasaksbehandler.student.StudentVurdering
import java.time.LocalDate

class BistandFaktagrunnlag(
    val vurderingsdato: LocalDate,
    val sisteDagMedMuligYtelse: LocalDate,
    val vurdering: BistandVurdering?,
    val studentvurdering: StudentVurdering?,
) : Faktagrunnlag
