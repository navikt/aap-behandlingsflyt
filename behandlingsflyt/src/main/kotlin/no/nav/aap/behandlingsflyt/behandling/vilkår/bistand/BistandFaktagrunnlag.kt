package no.nav.aap.behandlingsflyt.behandling.vilkår.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import java.time.LocalDate

class BistandFaktagrunnlag(
    val vurderingsdato: LocalDate,
    val sisteDagMedMuligYtelse: LocalDate,
    val bistandGrunnlag: BistandGrunnlag?,
    val studentvurdering: StudentVurdering?,
) : Faktagrunnlag
