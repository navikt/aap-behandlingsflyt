package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktDokument

/**
 * Den ferdige vurderingen av om medlemmet kan sanksjoneres etter ftrl § 11-9 "Reduksjon av
 * arbeidsavklaringspenger ved brudd på nærmere bestemte aktivitetsplikter".
 *
 * Vurderingen tar kun hensyn til § 11-9, så om det f.eks. også er stans etter en annen paragraf,
 * så får ikke vurderingen nødvendigvis noen effekt.
 *
 * Datoen/perioden som vurderingen gjelder for er implisitt, da typen brukes
 * inne i konteksten av en [Tidslinje][no.nav.aap.komponenter.tidslinje.Tidslinje].
 *
 * - [Folketrygdloven § 11-9](]https://lovdata.no/lov/1997-02-28-19/§11-9)
 * - [Forkskriftens § 4](https://lovdata.no/forskrift/2017-12-13-2100/§4)
*/
data class ReduksjonAktivitetspliktVurdering(
    val dokument: AktivitetspliktDokument,

    /** Er vilkårene for å reduksjon etter § 11-9 oppfylt? */
    val vilkårsvurdering: Vilkårsvurdering,
) {
    enum class Vilkårsvurdering {
        UNNTAK_RIMELIG_GRUNN,
        FORELDET,
        VILKÅR_FOR_REDUKSJON_OPPFYLT,
    }
}