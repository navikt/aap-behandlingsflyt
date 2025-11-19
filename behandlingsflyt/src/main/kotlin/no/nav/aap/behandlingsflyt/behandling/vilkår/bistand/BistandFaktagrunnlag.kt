package no.nav.aap.behandlingsflyt.behandling.vilkår.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import java.time.LocalDate
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag

class BistandFaktagrunnlag(
    val sisteDagMedMuligYtelse: LocalDate,
    val bistandGrunnlag: BistandGrunnlag?,
) : Faktagrunnlag
