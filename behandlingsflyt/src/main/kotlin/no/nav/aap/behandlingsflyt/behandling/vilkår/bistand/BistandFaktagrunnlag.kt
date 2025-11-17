package no.nav.aap.behandlingsflyt.behandling.vilkår.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering
import java.time.LocalDate

class BistandFaktagrunnlag(
    val sisteDagMedMuligYtelse: LocalDate,
    val vurderinger: List<BistandVurdering>,
) : Faktagrunnlag
