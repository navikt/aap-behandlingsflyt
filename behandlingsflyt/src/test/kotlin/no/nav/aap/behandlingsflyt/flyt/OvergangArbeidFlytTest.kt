package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.underveis.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.vedtakslengde.ÅrMedHverdager
import no.nav.aap.vilkårsresultat.RettighetsType
import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.vilkårsresultat.Vilkårtype
import no.nav.aap.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.steg.sykdom.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.BeforeParameterizedClassInvocation
import org.junit.jupiter.params.Parameter
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import kotlin.reflect.KClass

@ParameterizedClass
@MethodSource("unleashTestDataSource")
class OvergangArbeidFlytTest : AbstraktFlytOrkestratorSnapshotTest() {

    @field:Parameter
    lateinit var unleashGateway: KClass<out UnleashGateway>

    override fun unleashGateway() = unleashGateway

    lateinit var sak: Sak

    @BeforeParameterizedClassInvocation(injectArguments = false)
    fun settOppFGB() = snapshotEtterSetup {
        sak = happyCaseFørstegangsbehandling(LocalDate.now(), sendMeldekort = false)
    }

    @Test
    fun `Vurdering av 11-17`() {
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
            .bekreftVurderinger()
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
            .bekreftVurderinger()
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
            .bekreftVurderinger()
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

    @Test
    fun `Legge til revurdering av 11-17`() {
        /*
        Legg til "§ 11-17 AAP i perioden som arbeidssøker" som revurderingsårsak
        1. Hvis det finnes en eksisterende 11-17 vurdering, så skal det trigge avklaringsbehov på 11-17
        2. Hvis det ikke finnes vurdert 11-17, så skal det trigge en vurdering av § 11-5 + 11-17.
         */
        val startDato = LocalDate.now()
        val endringsdato = sak.rettighetsperiode.fom.plusDays(7)

        /* Gir AAP som arbeidssøker. */
        sak.opprettManuellRevurdering(
            no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.OVERGANG_ARBEID
        )
            .medKontekst {
                assertThat( åpneAvklaringsbehov.map { it.definisjon } ).containsExactly(Definisjon.AVKLAR_SYKDOM)
            }
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    løsningerForPerioder = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123128")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                            yrkesskadeBegrunnelse = null,
                            fom = startDato,
                            tom = startDato.plusDays(7)
                        ),
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Ikke syk",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123128")),
                            harSkadeSykdomEllerLyte = false,
                            erSkadeSykdomEllerLyteVesentligdel = false,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = false,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            harNedsattArbeidsevne = ArbeidsevneNedsattValg.NEI,
                            yrkesskadeBegrunnelse = null,
                            fom = startDato.plusDays(8),
                            tom = null
                        ),
                    )
                )
            )
            // 2.
            .medKontekst {
                assertThat( åpneAvklaringsbehov.map { it.definisjon } ).containsExactly(Definisjon.AVKLAR_OVERGANG_ARBEID)
            }

            // 1.
            .løsOvergangArbeid(Utfall.OPPFYLT, fom = endringsdato)
            .medKontekst {
                assertThat( åpneAvklaringsbehov.map { it.definisjon } ).doesNotContain(Definisjon.AVKLAR_OVERGANG_ARBEID)
            }
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .fattVedtak()

        sak.opprettManuellRevurdering(
            no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.OVERGANG_ARBEID
        )
            .medKontekst {
                assertThat( behandling.aktivtSteg() ).isEqualTo(StegType.OVERGANG_ARBEID)
            }
    }
}
