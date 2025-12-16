package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykepengerVurderingDto
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OrdinærAapFlytTest: AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    @Test
    fun `happy case førstegangsbehandling + revurder førstegangssøknad, nei på viss varighet, nei på 11-13 - avslag`() {
        val sak = happyCaseFørstegangsbehandling()
        val behandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom, false)

        behandling
            .løsSykdomsvurderingBrev()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs("Siden vurderingenGjelderFra er lik kravdato (rettighetsperiode.fom), så kan man revurdere 11-13")
                    .containsExactlyInAnyOrder(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
            .løsAvklaringsBehov(
                // Vi svarer nei på rett til sykepengererstatning
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "HAR IKKE RETT",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = false,
                        grunn = null,
                        gjelderFra = sak.rettighetsperiode.fom
                    ),
                )
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FORESLÅ_VEDTAK)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                val underveisGrunnlag = dataSource.transaction { connection ->
                    UnderveisRepositoryImpl(connection).hent(this.behandling.id)
                }

                assertThat(underveisGrunnlag.perioder).isNotEmpty
                assertThat(underveisGrunnlag.perioder).extracting<RettighetsType>(Underveisperiode::rettighetsType)
                    .describedAs("Ingen perioder skal være oppfylt.")
                    .allSatisfy { rettighetsType ->
                        assertThat(rettighetsType).isEqualTo(null)
                    }
            }
    }

    @Test
    fun `ved avslag på 11-5 hoppes det rett til beslutter-steget`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        var (_, behandling) = sendInnFørsteSøknad(periode = periode)

        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling)
        assertThat(alleAvklaringsbehov).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsAvklaringsBehov(
            behandling,
            AvklarSykdomLøsning(
                løsningerForPerioder = listOf(
                    SykdomsvurderingLøsningDto(
                        begrunnelse = "Er ikke syk nok",
                        dokumenterBruktIVurdering = listOf(JournalpostId("1231299")),
                        harSkadeSykdomEllerLyte = false,
                        erArbeidsevnenNedsatt = null,
                        erSkadeSykdomEllerLyteVesentligdel = null,
                        erNedsettelseIArbeidsevneAvEnVissVarighet = null,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = null,
                        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                        yrkesskadeBegrunnelse = null,
                        fom = periode.fom,
                        tom = null
                    )
                )
            ),
        )
        //behandling = løsOvergangUføre(behandling)
        behandling = løsSykdomsvurderingBrev(behandling)

        behandling = kvalitetssikreOk(behandling)

        // Saken står til To-trinnskontroll hos beslutter
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling)
        assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.FATTE_VEDTAK) }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = løsFatteVedtak(behandling)
        assertThat(behandling.status()).isEqualTo(Status.IVERKSETTES)

        val vedtak = hentVedtak(behandling.id)
        assertThat(vedtak.vedtakstidspunkt.toLocalDate()).isToday

        val resultat = dataSource.transaction {
            ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id)
        }
        assertThat(resultat).isEqualTo(Resultat.AVSLAG)
        val brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_AVSLAG)

        behandling =
            løsAvklaringsBehov(behandling, vedtaksbrevLøsning(brevbestilling.referanse.brevbestillingReferanse))
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        // Verifiserer at sykdomsvilkåret ikke er oppfylt
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder()).hasSize(1)
            .allMatch { vilkårsperiode -> !vilkårsperiode.erOppfylt() }

        // Saken er avsluttet, så det skal ikke være flere åpne avklaringsbehov
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).isEmpty()
    }

    @Test
    fun `ved førstegangsbehandling i steget for sykdom skal sykdomsvurdering for brev vises etter refusjonskrav er løst og før kvalitetsvurdering`() {
        var (sak, førstegangsbehandling) = sendInnFørsteSøknad()

        førstegangsbehandling = førstegangsbehandling
            .medKontekst {
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand(sak.rettighetsperiode.fom)
            .løsRefusjonskrav()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy {
                    assertThat(it.definisjon).isEqualTo(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)
                }
            }
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()

        assertThat(førstegangsbehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `ved revurdering i steget for sykdom skal sykdomsvurdering for brev vises etter refusjonskrav`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val person = TestPersoner.STANDARD_PERSON()
        val sak = happyCaseFørstegangsbehandling(periode.fom, person)

        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
        )
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand(sak.rettighetsperiode.fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.SKRIV_SYKDOMSVURDERING_BREV) }
            }
            .løsSykdomsvurderingBrev() // Krever ikke kvalitetskontroll i revurdering
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)

        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Blir satt på vent for etterspørring av informasjon`() {
        val (sak, behandling) = sendInnFørsteSøknad()
        behandling.medKontekst {
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            assertThat(åpneAvklaringsbehov.map { it.definisjon }).contains(Definisjon.AVKLAR_SYKDOM)
        }

        settBehandlingPåVent(
            behandling.id,
            BehandlingSattPåVent(
                frist = null,
                begrunnelse = "Avventer dokumentasjon",
                bruker = SYSTEMBRUKER,
                behandlingVersjon = behandling.versjon,
                grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER
            )
        )

        behandling.medKontekst {
            val frist = åpneAvklaringsbehov.first { it.erVentepunkt() }.frist()
            assertThat(frist).isNotNull

            assertThat(åpneAvklaringsbehov.map { it.definisjon })
                .hasSize(2)
                .containsExactlyInAnyOrder(Definisjon.MANUELT_SATT_PÅ_VENT, Definisjon.AVKLAR_SYKDOM)

        }

        sak.sendInnSøknad(
            SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", "JA", "NEI", "NEI", null)
            ),
        ).medKontekst {
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            assertThat(åpneAvklaringsbehov.map { it.definisjon })
                .containsExactlyInAnyOrder(Definisjon.MANUELT_SATT_PÅ_VENT, Definisjon.AVKLAR_SYKDOM)
        }
    }

}