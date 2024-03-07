package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.FORESLÅ_VEDTAK_KODE

@JsonTypeName(value = FORESLÅ_VEDTAK_KODE)
class ForeslåVedtakLøsning(val begrunnelse: String) :
    AvklaringsbehovLøsning
