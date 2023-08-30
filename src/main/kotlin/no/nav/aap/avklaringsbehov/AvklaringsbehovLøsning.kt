package no.nav.aap.avklaringsbehov

import no.nav.aap.flate.behandling.avklaringsbehov.LøsAvklaringsbehovDTO

abstract class AvklaringsbehovLøsning(val begrunnelse: String, val endretAv: String) :
    LøsAvklaringsbehovDTO
