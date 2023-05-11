package no.nav.aap.domene.behandling.avklaringsbehov.løsning

import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon

data class AvklaringsbehovLøsning(val definisjon: Definisjon, val begrunnelse: String, val endretAv: String)
