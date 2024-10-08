package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt

/* TODO: Er det avklart om § 11-8 kan føre til opphør? Første versjon av koden
 * er skrevet med antagelse om at det kun er stans. Så sjekk om noe må endres.
 **/

/** Den ferdige vurderingen av § 11-8 "Fravær fra fastsatt aktivitet".
 *
 * Vurderingen kan være at det blir stans av aap (og muligens opphør, avhengig av tolkning av lovverket).
 *
 * Datoen/perioden som vurderingen gjelder for er implisitt, da typen brukes
 * inne i konteksten av en [Tidslinje][no.nav.aap.tidslinje.Tidslinje].
 *
 * - [Folketrygdloven § 11-8](https://lovdata.no/lov/1997-02-28-19/§11-8)
 * - [Forskriftens § 3](https://lovdata.no/forskrift/2017-12-13-2100/§3)
 */
data class FraværFastsattAktivitetVurdering(
    /* TODO: Det kan være flere brudd som ligger til grunn for hvorfor vi kan stanse etter § 11-8. */
    val brudd: BruddAktivitetsplikt,

    /** Er vilkårene for stans etter § 11-8 oppfylt? */
    val utfall: StansUtfall,

    /** Er både maskinen og saksbehandler enig om at aap skal stanses? */
    val skalStanses: Boolean,
) {
    enum class StansUtfall {
        STANS_ANDRE_DAG,
        STANS_TI_DAGER_BRUKT_OPP,
        UNNTAK_INNTIL_EN_DAG,
        UNNTAK_STERKE_VELFERDSGRUNNER,
        UNNTAK_SYKDOM_ELLER_SKADE,
        IKKE_RELEVANT,
    }
}
