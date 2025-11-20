package no.nav.aap.behandlingsflyt.behandling.etannetsted

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.OppholdVurdering


data class InstitusjonsOpphold(val helse: HelseOpphold? = null, val soning: SoningOpphold? = null) {
    fun harNoeUavklart(): Boolean {
        if (soning?.vurdering == OppholdVurdering.AVSLÅTT) {
            // Avslått på soning, ingenting å ta stilling til uavhengig av om det er helse
            return false
        }
        if (soning?.vurdering == OppholdVurdering.UAVKLART) {
            return true
        }
        return helse?.vurdering == OppholdVurdering.UAVKLART
    }

    fun harUavklartSoningsopphold(): Boolean {
        return soning?.vurdering == OppholdVurdering.UAVKLART
    }

    fun harUavklartHelseopphold(): Boolean {
        return helse?.vurdering == OppholdVurdering.UAVKLART
    }
}