package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OvergangArbeidFlytTest: AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    @Test
    fun `Vurdering av 11-17`() {
        if (gatewayProvider.provide<UnleashGateway>().isDisabled(BehandlingsflytFeature.OvergangArbeid)) {
            return
        }

        val sak = happyCaseFørstegangsbehandling(LocalDate.now())
        val endringsdato = sak.rettighetsperiode.fom.plusDays(7)
        val sluttdato = endringsdato.plusMonths(6).minusDays(1)

        /* Gir AAP som arbeidssøker. */
        sak.opprettManuellRevurdering(
            no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
        )
            .løsSykdom(vurderingGjelderFra = endringsdato, erOppfylt = false)
            .løsBistand(endringsdato, erOppfylt = false)
            .løsOvergangArbeid(Utfall.OPPFYLT, fom = endringsdato)
            .løsSykdomsvurderingBrev()
            .fattVedtak()
            .also {
                assertThat(it.status()).isEqualTo(Status.IVERKSETTES)
            }
            .assertRettighetstype(
                Periode(sak.rettighetsperiode.fom, endringsdato.minusDays(1)) to RettighetsType.BISTANDSBEHOV,
                Periode(endringsdato, sluttdato) to RettighetsType.ARBEIDSSØKER,
            )

        /* Revurdering som ombestemmer seg, og ikke gir AAP som arbeidssøker. */
        sak.opprettManuellRevurdering(
            no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
        )
            .løsSykdom(vurderingGjelderFra = endringsdato, erOppfylt = false)
            .løsBistand(endringsdato, erOppfylt = false)
            .løsOvergangArbeid(Utfall.IKKE_OPPFYLT, fom = endringsdato)
            .løsSykdomsvurderingBrev()
            .fattVedtak()
            .also {
                assertThat(it.status()).isEqualTo(Status.IVERKSETTES)
                it.assertRettighetstype(
                    Periode(sak.rettighetsperiode.fom, endringsdato.minusDays(1)) to RettighetsType.BISTANDSBEHOV,
                )
            }
    }

    @Test
    fun `Endrer sykdomsvurdering slik at 11-17-vurdering ikke lenger er nødvendig`() {
        if (gatewayProvider.provide<UnleashGateway>().isDisabled(BehandlingsflytFeature.OvergangArbeid)) {
            return
        }

        val sak = happyCaseFørstegangsbehandling(LocalDate.now())
        val periodeEttAar = Periode(fom = sak.rettighetsperiode.fom, tom = sak.rettighetsperiode.fom.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR))

        /* Gir AAP som arbeidssøker. */
        val endringsdato = sak.rettighetsperiode.fom.plusDays(7)
        sak.opprettManuellRevurdering(
            no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
        )
            .løsSykdom(vurderingGjelderFra = endringsdato, erOppfylt = false)
            .løsBistand(endringsdato, erOppfylt = true)
            .løsOvergangArbeid(Utfall.OPPFYLT, fom = endringsdato)
            /* Her hopper vi "tilbake" i flyten og endrer sykdom til oppfylt. */
            .løsSykdom(vurderingGjelderFra = endringsdato, erOppfylt = true)
            .løsSykdomsvurderingBrev()
            .fattVedtak()
            .also {
                assertThat(it.status()).isEqualTo(Status.IVERKSETTES)
            }
            .assertRettighetstype(
                periodeEttAar to RettighetsType.BISTANDSBEHOV,
            )
            .assertVilkårsutfall(
                Vilkårtype.OVERGANGARBEIDVILKÅRET,
                sak.rettighetsperiode to Utfall.IKKE_VURDERT
            )
    }


}