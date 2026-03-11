package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Utfall.REDUKSJON
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Utfall.UNNTAK
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.REDUKSJON_ANDRE_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.REDUKSJON_TI_DAGER_BRUKT_OPP
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_INNTIL_EN_DAG
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_STERKE_VELFERDSGRUNNER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.UNNTAK_SYKDOM_ELLER_SKADE
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.FraværForDag

/** Den ferdige vurderingen av § 11-8 "Fravær fra fastsatt aktivitet".
 *
 * Vurderingen kan være at det blir reduksjon av aap.
 *
 * Datoen/perioden som vurderingen gjelder for er implisitt, da typen brukes
 * inne i konteksten av en [Tidslinje][no.nav.aap.komponenter.tidslinje.Tidslinje].
 *
 * - [Folketrygdloven § 11-8](https://lovdata.no/lov/1997-02-28-19/§11-8)
 * - [Forskriftens § 3](https://lovdata.no/forskrift/2017-12-13-2100/§3)
 */
data class FraværFastsattAktivitetVurdering(
    val fravær: FraværForDag,
    /** Er vilkårene for reduksjon etter § 11-8 oppfylt? */
    val vilkårsvurdering: Vilkårsvurdering,
) {
    enum class Utfall {
        REDUKSJON,
        UNNTAK
    }

    val utfall: Utfall
        get() = when (vilkårsvurdering) {
            REDUKSJON_ANDRE_DAG, REDUKSJON_TI_DAGER_BRUKT_OPP -> REDUKSJON
            UNNTAK_INNTIL_EN_DAG, UNNTAK_STERKE_VELFERDSGRUNNER, UNNTAK_SYKDOM_ELLER_SKADE -> UNNTAK
        }

    enum class Vilkårsvurdering {
        REDUKSJON_ANDRE_DAG,
        REDUKSJON_TI_DAGER_BRUKT_OPP,
        UNNTAK_INNTIL_EN_DAG,
        UNNTAK_STERKE_VELFERDSGRUNNER,
        UNNTAK_SYKDOM_ELLER_SKADE,
    }
}
