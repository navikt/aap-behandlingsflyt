package no.nav.aap.behandlingsflyt.behandling.rettighetstype

import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.help.genererVilkårsresultat
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtledRettighetsTypeTest {
    
    @Test
    fun `Rettighetstyper og kvoter`() {
        val rettighetsperiode = Periode(1 januar 2020, Tid.MAKS)

        val vilkårsresulatUtenRett = genererVilkårsresultat(
            rettighetsperiode,
            bistandVilkåret = Vilkår(
                Vilkårtype.BISTANDSVILKÅRET, setOf(
                    Vilkårsperiode(
                        rettighetsperiode,
                        Utfall.IKKE_OPPFYLT,
                        false,
                        null,
                        faktagrunnlag = null,
                        avslagsårsak = Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING
                    )
                )
            )
        )

        val rettighetsTidslinje = vilkårsresulatUtenRett.rettighetstypeTidslinje()
        assertThat(rettighetsTidslinje.segmenter()).isEmpty()
        val kvotevurdering = vurderRettighetstypeOgKvoter(
            vilkårsresulatUtenRett,
            KvoteService().beregn()
        )

        assertThat(kvotevurdering.segmenter().any { it.verdi is KvoteOk }).isFalse()
    }
}