package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class Avslag11_27FlytTest : AbstraktFlytOrkestratorTest(Avslag11_27FlytTestUnleash::class) {

    @Test
    fun `innvilgelse - skalAvslås1127 false gir oppfylt vilkår og avsluttet behandling`() {
        val fom = LocalDate.now()

        val (_, behandling) = sendInnFørsteSøknad(mottattTidspunkt = fom.atStartOfDay())

        behandling
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .leggTilVurderingsbehov(Vurderingsbehov.VURDER_AVSLAG_11_27)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon })
                    .describedAs("Skal ha åpent avklaringsbehov for avslag § 11-27")
                    .contains(Definisjon.VURDER_AVSLAG_11_27)
            }
            .løsAvslag11_27(skalAvslås1127 = false)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon })
                    .doesNotContain(Definisjon.VURDER_AVSLAG_11_27)

                val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
                val samordningVilkår = vilkårsresultat.finnVilkår(Vilkårtype.SAMORDNING)
                assertThat(samordningVilkår.vilkårsperioder())
                    .anySatisfy { periode ->
                        assertThat(periode.erOppfylt()).isTrue()
                    }
            }
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
            }
            .løsVedtaksbrev()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }
    }

    @Test
    fun `avslag - skalAvslås1127 true gir ikke oppfylt vilkår med ANNEN_FULL_YTELSE_AVSLAG`() {
        val fom = LocalDate.now()

        val (_, behandling) = sendInnFørsteSøknad(mottattTidspunkt = fom.atStartOfDay())

        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .leggTilVurderingsbehov(Vurderingsbehov.VURDER_AVSLAG_11_27)
            .løsAvslag11_27(
                skalAvslås1127 = true,
                brukersYtelse = Ytelse.SYKEPENGER,
                brukersYtelseTom = fom.plusMonths(6),
                harSykepengegrunnlagOver2G = true,
                harArbeidsgiverSykepengerUtbetaling = false,
            )
            .medKontekst {
                val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
                val samordningVilkår = vilkårsresultat.finnVilkår(Vilkårtype.SAMORDNING)

                assertThat(samordningVilkår.type.hjemmel)
                    .describedAs("Vilkåret for samordning skal ha hjemmel § 11-27")
                    .isEqualTo("§ 11-27")

                assertThat(samordningVilkår.vilkårsperioder())
                    .anySatisfy { periode ->
                        assertThat(periode.erOppfylt()).isFalse()
                        assertThat(periode.avslagsårsak)
                            .isEqualTo(Avslagsårsak.ANNEN_FULL_YTELSE_AVSLAG)
                        assertThat(periode.avslagsårsak?.hjemmel)
                            .describedAs("Avslagsårsaken skal peke på riktig hjemmel § 11-27")
                            .isEqualTo("§ 11-27")
                    }
            }
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)

                val vedtak = hentVedtak(behandlingId = behandling.id)
                assertThat(vedtak).isNotNull()
            }
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_AVSLAG)
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)

                val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
                val samordningVilkår = vilkårsresultat.finnVilkår(Vilkårtype.SAMORDNING)
                assertThat(samordningVilkår.harPerioderMedIkkeOppfylt())
                    .describedAs("Vedtaket skal fortsatt reflektere avslag på § 11-27 etter avslutning")
                    .isTrue()
            }
    }

    @Test
    fun `steget trigges ikke uten at vurderingsbehov er lagt til manuelt`() {
        val fom = LocalDate.now()

        val (_, behandling) = sendInnFørsteSøknad(mottattTidspunkt = fom.atStartOfDay())

        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .medKontekst {
                assertThat(hentAlleAvklaringsbehov(behandling).map { it.definisjon })
                    .describedAs("§ 11-27 skal aldri trigges automatisk uten at saksbehandler legger til vurderingsbehovet")
                    .doesNotContain(Definisjon.VURDER_AVSLAG_11_27)
            }
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }
    }

    @Test
    fun `harAnnenFullYtelse true men skalAvslås1127 false - gir likevel oppfylt vilkår`() {
        val fom = LocalDate.now()

        val (_, behandling) = sendInnFørsteSøknad(mottattTidspunkt = fom.atStartOfDay())

        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .leggTilVurderingsbehov(Vurderingsbehov.VURDER_AVSLAG_11_27)
            .løsAvslag11_27(
                skalAvslås1127 = false,
                brukersYtelse = Ytelse.SYKEPENGER,
                brukersYtelseTom = fom.plusMonths(3),
                harSykepengegrunnlagOver2G = false,
                harArbeidsgiverSykepengerUtbetaling = false,
            )
            .medKontekst {
                val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
                val samordningVilkår = vilkårsresultat.finnVilkår(Vilkårtype.SAMORDNING)

                assertThat(samordningVilkår.vilkårsperioder())
                    .describedAs("skalAvslås1127 = false skal gi oppfylt vilkår selv om harAnnenFullYtelse er true")
                    .anySatisfy { periode ->
                        assertThat(periode.erOppfylt()).isTrue()
                        assertThat(periode.avslagsårsak).isNull()
                    }
            }
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }
    }

    @Test
    fun `flere krav i samme behandling - hvert krav får uavhengig utfall`() {
        val fom = LocalDate.now()

        val (_, behandling) = sendInnFørsteSøknad(mottattTidspunkt = fom.atStartOfDay())

        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .leggTilVurderingsbehov(Vurderingsbehov.VURDER_AVSLAG_11_27)
            .medKontekst {
                val kravreferanser = dataSource.transaction(readOnly = true) { connection ->
                    postgresRepositoryRegistry.provider(connection)
                        .provide<KravRepository>()
                        .hentHvisEksisterer(behandling.id)
                        ?.gjeldendeRelevanteKrav()
                        .orEmpty()
                }
                assertThat(kravreferanser)
                    .describedAs("Denne testen forutsetter minst ett relevant krav for behandlingen")
                    .isNotEmpty()
            }
            .løsAvslag11_27(skalAvslås1127 = true)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon })
                    .doesNotContain(Definisjon.VURDER_AVSLAG_11_27)

                val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
                val samordningVilkår = vilkårsresultat.finnVilkår(Vilkårtype.SAMORDNING)

                assertThat(samordningVilkår.vilkårsperioder())
                    .allSatisfy { periode ->
                        assertThat(periode.erOppfylt()).isFalse()
                        assertThat(periode.avslagsårsak).isEqualTo(Avslagsårsak.ANNEN_FULL_YTELSE_AVSLAG)
                    }
            }
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_AVSLAG)
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }
    }

    @Test
    fun `revurdering - tidligere avslag 11-27 videreføres eller kan overstyres`() {
        val fom = LocalDate.now()

        val (sak, behandling) = sendInnFørsteSøknad(mottattTidspunkt = fom.atStartOfDay())

        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .leggTilVurderingsbehov(Vurderingsbehov.VURDER_AVSLAG_11_27)
            .løsAvslag11_27(skalAvslås1127 = true)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_AVSLAG)
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }

        val revurdering = sak.opprettManuellRevurdering(
            vurderingsbehov = listOf(Vurderingsbehov.VURDER_AVSLAG_11_27),
        )

        revurdering
            .medKontekst {
                val vilkårsresultat = hentVilkårsresultat(behandlingId = revurdering.id)
                val samordningVilkår = vilkårsresultat.finnVilkår(Vilkårtype.SAMORDNING)

                assertThat(samordningVilkår.vilkårsperioder())
                    .describedAs("Kopiert grunnlag fra forrige behandling skal videreføre forrige avslag inntil ny vurdering gjøres")
                    .anySatisfy { periode ->
                        assertThat(periode.avslagsårsak).isEqualTo(Avslagsårsak.ANNEN_FULL_YTELSE_AVSLAG)
                    }
            }
            .løsAvslag11_27(skalAvslås1127 = false)
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon })
                    .doesNotContain(Definisjon.VURDER_AVSLAG_11_27)

                val vilkårsresultat = hentVilkårsresultat(behandlingId = revurdering.id)
                val samordningVilkår = vilkårsresultat.finnVilkår(Vilkårtype.SAMORDNING)

                assertThat(samordningVilkår.vilkårsperioder())
                    .describedAs("Ny vurdering i revurdering skal overstyre forrige avslag")
                    .anySatisfy { periode ->
                        assertThat(periode.erOppfylt()).isTrue()
                    }
            }
    }

    @Test
    fun `avslag 11-27 overstyrer samordning når begge sier ikke oppfylt`() {
        val fom = LocalDate.now()

        val (_, behandling) = sendInnFørsteSøknad(mottattTidspunkt = fom.atStartOfDay())

        behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .leggTilVurderingsbehov(Vurderingsbehov.VURDER_AVSLAG_11_27)
            // 11-27 sier ogsa IKKE_OPPFYLT
            .løsAvslag11_27(skalAvslås1127 = true)
            // Samordning sier IKKE_OPPFYLT (100% annen ytelse)
            .løsSamordningMedGradering(periode = Periode(fom.minusMonths(2), fom.plusMonths(2)))
            .medKontekst {
                val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
                val samordningVilkår = vilkårsresultat.finnVilkår(Vilkårtype.SAMORDNING)

                assertThat(samordningVilkår.vilkårsperioder())
                    .describedAs("Når begge sier IKKE_OPPFYLT skal avslagsårsak være fra 11-27, ikke generell samordning")
                    .anySatisfy { periode ->
                        assertThat(periode.erOppfylt()).isFalse()
                        assertThat(periode.avslagsårsak)
                            .describedAs("11-27 skal vinne over ANNEN_FULL_YTELSE når begge er IKKE_OPPFYLT")
                            .isEqualTo(Avslagsårsak.ANNEN_FULL_YTELSE_AVSLAG)
                    }
            }
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_AVSLAG)
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }
    }

    @Test
    fun `11-27 oppfylt overstyrer ikke samordning når samordning dekker hele vedtaksperioden`() {
        val fom = LocalDate.now()

        val (_, behandling) = sendInnFørsteSøknad(mottattTidspunkt = fom.atStartOfDay())

        val behandlingKlarForSamordning = behandling
            .løsSykdom(fom)
            .løsBistand(fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .leggTilVurderingsbehov(Vurderingsbehov.VURDER_AVSLAG_11_27)
            .løsAvslag11_27(skalAvslås1127 = false)

        val heleVedtaksperioden = dataSource.transaction(readOnly = true) { connection ->
            postgresRepositoryRegistry.provider(connection)
                .provide<VilkårsresultatRepository>()
                .hent(behandling.id)
                .finnVilkår(Vilkårtype.SAMORDNING)
                .vilkårsperioder()
                .let { perioder ->
                    Periode(
                        fom = perioder.minOf { it.periode.fom },
                        tom = perioder.maxOf { it.periode.tom },
                    )
                }
        }

        behandlingKlarForSamordning
            .løsSamordningMedGradering(periode = heleVedtaksperioden, gradering = 100)
            .medKontekst {
                val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
                val samordningVilkår = vilkårsresultat.finnVilkår(Vilkårtype.SAMORDNING)

                assertThat(samordningVilkår.vilkårsperioder())
                    .describedAs("11-27 OPPFYLT skal ikke overstyre samordning IKKE_OPPFYLT når hele perioden er dekket")
                    .allSatisfy { periode ->
                        assertThat(periode.erOppfylt()).isFalse()
                    }
            }
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_AVSLAG)
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }
    }
}

object Avslag11_27FlytTestUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.IngenValidering,
        BehandlingsflytFeature.Avslag11_27,
        BehandlingsflytFeature.KravSteg,
        BehandlingsflytFeature.KravAutomatiskVurdering,
    )
)