package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.type.Periode

data class VurderingsbehovMedPeriode(val type: Vurderingsbehov, val periode: Periode? = null)
