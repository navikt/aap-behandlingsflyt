package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import java.time.LocalDate

data class VedtakslengdeVurderingDto(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val begrunnelse: String,
    val sluttdato: LocalDate,
) : LøsningForPeriode

