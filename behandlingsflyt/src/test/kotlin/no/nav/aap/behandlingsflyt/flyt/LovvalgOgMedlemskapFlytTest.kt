package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarPeriodisertLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLandEllerLandMedAvtale
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.PeriodisertManuellVurderingForLovvalgMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.UtenlandsPeriodeDto
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LovvalgOgMedlemskapFlytTest: AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    @Test
    fun `ved førstegangsbehandling med avslag før sykdom ved manglende medlemskap er ikke sykdomsvurdering for brev aktuelt`() {
        val (sak, behandling) = sendInnFørsteSøknad(
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3)),
            søknad = TestSøknader.SØKNAD_INGEN_MEDLEMSKAP
        )

        val oppdatertBehandling = behandling
            .løsLovvalg(sak.rettighetsperiode.fom, false)
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_AVSLAG)

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `ved førstegangsbehandling og annet lovvalgsland settes saken på vent`() {
        val (sak, behandling) = sendInnFørsteSøknad(
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3)),
            søknad = TestSøknader.SØKNAD_INGEN_MEDLEMSKAP
        )

        val oppdatertBehandling = behandling
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = sak.rettighetsperiode.fom,
                            tom = sak.rettighetsperiode.tom,
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.ESP),
                            medlemskap = null,
                        )
                    )
                )
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.VENTE_PÅ_UTENLANDSK_VIDEREFØRING_AVKLARING)
            }

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.UTREDES)
    }

    @Test
    fun `ved revurdering med avslag før sykdom ved manglende medlemskap er ikke sykdomsvurdering for brev aktuelt`() {
        val sak = happyCaseFørstegangsbehandling(LocalDate.now())
        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP)
        )

        val oppdatertBehandling = revurdering
            .løsLovvalg(sak.rettighetsperiode.fom, false)
            .løsUtenSamordning()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `skal kunne gjennomføre førstegangsbehandling med periodisert lovvalg og medlemskap`() {
        var (sak, førstegangsbehandling) = sendInnFørsteSøknad(søknad = TestSøknader.SØKNAD_INGEN_MEDLEMSKAP)

        førstegangsbehandling = førstegangsbehandling
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = sak.rettighetsperiode.fom,
                            tom = sak.rettighetsperiode.fom.plusMonths(2),
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", false)
                        ),
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = sak.rettighetsperiode.fom.plusMonths(2).plusDays(1),
                            tom = null,
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", true)
                        )
                    )
                )
            )
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand(sak.rettighetsperiode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()

        // Virkningstidspunktet skal settes til datoen hvor medlemskap er oppfylt
        val vedtak = hentVedtak(førstegangsbehandling.id)
        assertThat(vedtak.virkningstidspunkt).isEqualTo(sak.rettighetsperiode.fom.plusMonths(2).plusDays(1))

        assertThat(førstegangsbehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `skal kunne gjennomføre førstegangsbehandling og revurdering hvor oppfylt lovvalg og medlemskap flyttes en mnd`() {
        var (sak, førstegangsbehandling) = sendInnFørsteSøknad(søknad = TestSøknader.SØKNAD_INGEN_MEDLEMSKAP)
        val nyttTidspunktForOppfyltMedlemskap = sak.rettighetsperiode.fom.plusMonths(1).plusDays(1)

        førstegangsbehandling = førstegangsbehandling
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = sak.rettighetsperiode.fom,
                            tom = sak.rettighetsperiode.fom.plusMonths(2),
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", false)
                        ),
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = sak.rettighetsperiode.fom.plusMonths(2).plusDays(1),
                            tom = null,
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", true)
                        )
                    )
                )
            )
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand(sak.rettighetsperiode.fom)
            .løsRefusjonskrav()
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

        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.LOVVALG_OG_MEDLEMSKAP),
        )
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        // Skulle oppfylles en mnd tidligere
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = nyttTidspunktForOppfyltMedlemskap,
                            tom = null,
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", true)
                        )
                    )
                )
            )
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand(sak.rettighetsperiode.fom)
            .løsSykdomsvurderingBrev()
            .løsBeregningstidspunkt()
            .løsUtenSamordning()
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)

        // Virkningstidspunktet skal settes til datoen hvor medlemskap er oppfylt
        val vedtak = hentVedtak(revurdering.id)
        assertThat(vedtak.virkningstidspunkt).isEqualTo(sak.rettighetsperiode.fom.plusMonths(1).plusDays(1))

        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Lager avklaringsbehov i medlemskap når kravene til manuell avklaring oppfylles`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = periode.fom.atStartOfDay(),
            periode = periode,
            søknad = SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("NEI", null, "JA", null, null)
            )
        )

        // Validér avklaring
        val åpenAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpenAvklaringsbehov)
            .extracting<Definisjon> { it.definisjon }
            .containsOnly(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
    }

    @Test
    fun `Går automatisk forbi medlemskap når kravene til manuell avklaring ikke oppfylles`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )

        // Validér avklaring
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })
    }

    @Test
    fun `Gir oppfylt når bruker ikke har lovvalgsland men oppfyller trygdeloven`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        var behandling = sendInnSøknad(
            ident, periode,
            SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto(
                    "JA", null, "JA", null,
                    listOf(
                        UtenlandsPeriodeDto(
                            "SWE",
                            LocalDate.now().plusMonths(1),
                            LocalDate.now().minusMonths(1),
                            "JA",
                            null,
                            LocalDate.now().plusMonths(1),
                            LocalDate.now().minusMonths(1),
                        )
                    )
                )
            )
        )

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = behandling.løsLovvalg(periode.fom)

        // Validér riktig resultat
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP })
        assertTrue(vilkårsResultat.all { it.erOppfylt() })
    }

    @Test
    fun `Gir avslag når bruker har annet lovvalgsland`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        var behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto(
                    "JA", null, "JA", null,
                    listOf(
                        UtenlandsPeriodeDto(
                            "SWE",
                            LocalDate.now().plusMonths(1),
                            LocalDate.now().minusMonths(1),
                            "JA",
                            null,
                            LocalDate.now().plusMonths(1),
                            LocalDate.now().minusMonths(1),
                        )
                    )
                ),
            )
        )

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = behandling
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = periode.fom,
                            tom = null,
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.DNK),
                            medlemskap = null
                        )
                    )
                )
            )

        // Validér riktig resultat
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        assertTrue(åpneAvklaringsbehov.none { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })
        assertTrue(vilkårsResultat.none { it.erOppfylt() })
    }

    @Test
    fun `Gir avslag når bruker ikke er medlem i trygden`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        var behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto(
                    "JA", null, "JA", null,
                    listOf(
                        UtenlandsPeriodeDto(
                            "SWE",
                            LocalDate.now().plusMonths(1),
                            LocalDate.now().minusMonths(1),
                            "JA",
                            null,
                            LocalDate.now().plusMonths(1),
                            LocalDate.now().minusMonths(1),
                        )
                    )
                ),
            )
        )

        // Validér avklaring
        var åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.all { it.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP })

        // Trigger manuell vurdering
        behandling = behandling.løsLovvalg(periode.fom, false)

        // Validér avklaring
        åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov.none())

        // Validér riktig resultat
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        assertThat(vilkårsResultat).noneMatch { it.erOppfylt() }
        assertTrue(Avslagsårsak.IKKE_MEDLEM == vilkårsResultat.first().avslagsårsak)
    }

    @Test
    fun `Kan løse overstyringsbehov til ikke oppfylt`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )
            .løsLovvalgOverstyrt(periode.fom, false)
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .doesNotContain(Definisjon.MANUELL_OVERSTYRING_LOVVALG)


                // Validér riktig resultat
                val vilkårsResultat =
                    hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
                assertThat(vilkårsResultat).allMatch { !it.erOppfylt() }
                assertThat(vilkårsResultat.first().avslagsårsak).isEqualTo(Avslagsårsak.IKKE_MEDLEM)
            }
    }

    @Test
    fun `Kan løse overstyringsbehov til oppfylt`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        ).løsLovvalgOverstyrt(periode.fom, true)

        // Validér avklaring
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertTrue(åpneAvklaringsbehov.none { Definisjon.MANUELL_OVERSTYRING_LOVVALG == it.definisjon })

        // Validér riktig resultat
        val vilkårsResultat = hentVilkårsresultat(behandling.id).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder()
        val overstyrtManuellVurdering = dataSource.transaction {
            MedlemskapArbeidInntektRepositoryImpl(it).hentHvisEksisterer(behandling.id)?.vurderinger?.firstOrNull()?.overstyrt
        }
        assertTrue(vilkårsResultat.all { it.erOppfylt() })
        assertTrue(overstyrtManuellVurdering == true)
    }

}