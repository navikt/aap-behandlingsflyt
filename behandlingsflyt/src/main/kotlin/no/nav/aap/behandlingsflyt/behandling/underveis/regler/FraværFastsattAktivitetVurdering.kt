package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Utfall.REDUKSJON
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Utfall.UNNTAK
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.FRAVÆR_FØRSTE_DAG_I_MELDEPERIODE
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.FRAVÆR_STERK_VELFERDSGRUNN
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.FRAVÆR_SYKDOM_ELLER_SKADE
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.MER_ENN_EN_DAGS_FRAVÆR_I_MELDEPERIODE
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FraværFastsattAktivitetVurdering.Vilkårsvurdering.MER_ENN_TI_DAGERS_FRAVÆR_I_KALENDERÅR
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.FraværForPeriode

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
    val fravær: FraværForPeriode,
    /** Er vilkårene for reduksjon etter § 11-8 oppfylt? */
    val vilkårsvurdering: Vilkårsvurdering,
) {
    enum class Utfall {
        REDUKSJON,
        UNNTAK
    }

    val utfall = when (vilkårsvurdering) {
        MER_ENN_EN_DAGS_FRAVÆR_I_MELDEPERIODE, MER_ENN_TI_DAGERS_FRAVÆR_I_KALENDERÅR -> REDUKSJON
        FRAVÆR_FØRSTE_DAG_I_MELDEPERIODE, FRAVÆR_STERK_VELFERDSGRUNN, FRAVÆR_SYKDOM_ELLER_SKADE -> UNNTAK
    }

    enum class Vilkårsvurdering {
        MER_ENN_EN_DAGS_FRAVÆR_I_MELDEPERIODE,
        MER_ENN_TI_DAGERS_FRAVÆR_I_KALENDERÅR,
        FRAVÆR_FØRSTE_DAG_I_MELDEPERIODE,
        FRAVÆR_STERK_VELFERDSGRUNN,
        FRAVÆR_SYKDOM_ELLER_SKADE,
    }
}
