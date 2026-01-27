package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.flate.AvbrytRevurderingVurderingDto
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.flate.AvbrytRevurderingÅrsakDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvbrytRevurderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TrekkSøknadLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.repository.behandling.vedtak.VedtakRepositoryImpl
import no.nav.aap.behandlingsflyt.test.AlleAvskrudd
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus

class TrekkOgAvbrytFlytTest: AbstraktFlytOrkestratorTest(AlleAvskrudd::class) {
    @Test
    fun `kan trekke søknad som har passert manuelt vurdert lovvalg`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val (_, behandling) = sendInnFørsteSøknad(
            periode = periode,
            søknad = SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("NEI", "NEI", "NEI", null, null)
            )
        )

        behandling.medKontekst {
            assertTrue(åpneAvklaringsbehov.all { Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP == it.definisjon })
        }
            // Løs lovvalg
            .løsLovvalg(periode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactly(Definisjon.AVKLAR_SYKDOM)
            }
            // Trekk søknad
            .leggTilVurderingsbehov(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SØKNAD_TRUKKET)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .contains(Definisjon.VURDER_TREKK_AV_SØKNAD)
            }
            .løsAvklaringsBehov(TrekkSøknadLøsning(begrunnelse = "trekker søknaden"))
            .medKontekst {
                assertThat(åpneAvklaringsbehov).isEmpty()
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
                dataSource.transaction {
                    assertThat(VedtakRepositoryImpl(it).hent(this.behandling.id)).isNull()
                }
            }
    }

    @Test
    fun `kan trekke søknad som har passert forutgående medlemskap`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val (_, behandling) = sendInnFørsteSøknad(
            periode = periode,
            søknad = SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("NEI", "NEI", "NEI", null, null)
            )
        )

        behandling
            .løsLovvalg(periode.fom)
            // Løs fram til forutgående
            .løsFramTilForutgåendeMedlemskap(periode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov)
                    .extracting<Definisjon> { it.definisjon }
                    .contains(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
            }
            // Løs forutgående
            .løsForutgåendeMedlemskap(periode.fom)
            .løsOppholdskrav(periode.fom)
            // Trekk søknad
            .leggTilVurderingsbehov(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SØKNAD_TRUKKET)
            .medKontekst {
                assertThat(åpneAvklaringsbehov)
                    .extracting<Definisjon> { it.definisjon }
                    .contains(Definisjon.VURDER_TREKK_AV_SØKNAD)
            }
            .løsAvklaringsBehov(TrekkSøknadLøsning(begrunnelse = "trekker søknaden"))
            .medKontekst {
                assertThat(åpneAvklaringsbehov).isEmpty()
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }
    }

    @Test
    fun `trukket søknad blokkerer nye ytelsesbehandlinger`() {
        // Sender inn en søknad
        var (sak, behandling) = sendInnFørsteSøknad()

        løsSykdom(behandling, sak.rettighetsperiode.fom)
            .leggTilVurderingsbehov(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SØKNAD_TRUKKET)
            .medKontekst {
                assertThat(åpneAvklaringsbehov)
                    .extracting<Definisjon> { it.definisjon }
                    .contains(Definisjon.VURDER_TREKK_AV_SØKNAD)
            }
            .løsAvklaringsBehov(TrekkSøknadLøsning(begrunnelse = "trekker søknaden"))
            .medKontekst {
                assertThat(åpneAvklaringsbehov).isEmpty()
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }

        behandling = sak.sendInnSøknad(
            SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei),
                yrkesskade = "NEI",
                oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "NEI", "NEI", "NEI", null)
            ),
        )

        assertThat(behandling.forrigeBehandlingId).isNull()
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Teste AvbrytRevurderingFlyt`() {
        // Førstegangsbehandling
        val sak = happyCaseFørstegangsbehandling()
        val førstegangsbehandling = hentSisteOpprettedeBehandlingForSak(sak.id)

        // Revurdering 1 - skal bli avbrutt
        val revurdering1 = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)
        )
            .medKontekst {
                assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsSykdom(sak.rettighetsperiode.fom)

        assertThat(revurdering1.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
        assertThat(revurdering1.forrigeBehandlingId).isEqualTo(førstegangsbehandling.id)

        // Avbryt revurdering 1
        revurdering1.leggTilVurderingsbehov(
            no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDERING_AVBRUTT
        ).medKontekst {
            assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }.contains(Definisjon.AVBRYT_REVURDERING)
        }

        løsAvklaringsBehov(
            revurdering1,
            AvbrytRevurderingLøsning(
                vurdering = AvbrytRevurderingVurderingDto(
                    årsak = AvbrytRevurderingÅrsakDto.REVURDERINGEN_BLE_OPPRETTET_VED_EN_FEIL,
                    begrunnelse = "Fordi den ikke er aktuell lenger"
                ),
            )
        )

        val avklaringsbehovene: List<Avklaringsbehov> = hentAlleAvklaringsbehov(revurdering1)
        val revurdering1FraRepo = hentBehandling(revurdering1.referanse)
        assertThat(revurdering1FraRepo.status()).isEqualTo(Status.AVSLUTTET)
        assertThat(avklaringsbehovene.none { it.erÅpent() }).isTrue()
        assertStatusForDefinisjon(avklaringsbehovene, Definisjon.AVBRYT_REVURDERING, AvklaringsbehovStatus.AVSLUTTET)
        assertStatusForDefinisjon(avklaringsbehovene, Definisjon.AVKLAR_SYKDOM, AvklaringsbehovStatus.AVBRUTT)
        assertStatusForDefinisjon(avklaringsbehovene, Definisjon.AVKLAR_BISTANDSBEHOV, AvklaringsbehovStatus.AVBRUTT)

        // Revurdering 2 - skal ikke kopiere data fra revurdering1 men fra førstegangsbehandling
        val revurdering2 = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.FORUTGAENDE_MEDLEMSKAP)
        )
            .medKontekst {
                assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }

        assertThat(revurdering2.forrigeBehandlingId).isEqualTo(førstegangsbehandling.id)

        // Verifiser at data er kopiert fra førstegangsbehandling
        val vilkårsresultat1 = hentVilkårsresultat(førstegangsbehandling.id)
        val vilkårsresultat2 = hentVilkårsresultat(revurdering2.id)

        assertThat(vilkårsresultat2).usingRecursiveComparison()
            .ignoringFields(
                "id",
                "faktagrunnlag",
                "vilkår.vilkårTidslinje",
                "vilkår.vurdertTidspunkt",
                "vilkår.faktagrunnlag",
                "vilkår.vilkårTidslinje",
                "vilkår.vurdertTidspunkt"
            )
            .isEqualTo(vilkårsresultat1)
    }

}