package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningUføreLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.YrkesskadeSakDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class YrkesskadeFlytTest : AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    @Test
    fun `skal ikke vise avklaringsbehov for yrkesskade ved avslag i tidligere steg`() {
        val personMedYrkesskade = TestPersoner.PERSON_MED_YRKESSKADE()
        val (sak, behandling) = sendInnFørsteSøknad(
            person = personMedYrkesskade,
        )

        val oppdatertBehandling = behandling
            .løsAvklaringsBehov(
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
                            fom = sak.rettighetsperiode.fom,
                            tom = null,
                        )
                    )
                )
            )
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_AVSLAG)

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `ved revurdering med yrkesskade og årsakssammenheng må både beregningstidspunkt og yrkesskadeinntekt avklares ved revurdering av beregning`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val person = TestPersoner.PERSON_MED_YRKESSKADE()
        // Sender inn en søknad
        val (sak, behandling) = sendInnFørsteSøknad(
            periode = periode,
            person = person,
            søknad = TestSøknader.STANDARD_SØKNAD
        )

        val oppdatertBehandling = behandling
            .løsSykdom(periode.fom)
            .løsBistand(periode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Veldig relevante",
                        relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                        relevanteYrkesskadeSaker = person.yrkesskade.map {
                            YrkesskadeSakDto(
                                it.saksreferanse,
                                null,
                            )
                        },
                        andelAvNedsettelsen = 50,
                        erÅrsakssammenheng = true
                    )
                )
            )
            .løsBeregningstidspunkt()
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_INNVILGELSE)

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)

        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_BEREGNING),
        )
            .løsBeregningstidspunkt()
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)


        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `ved revurdering med yrkesskade skal både årsakssammenheng og yrkesskadeinntekt avklares hvis årsak revurder yrkesskade`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val person = TestPersoner.PERSON_MED_YRKESSKADE()
        // Sender inn en søknad
        val (sak, behandling) = sendInnFørsteSøknad(
            periode = periode,
            person = person,
            søknad = TestSøknader.STANDARD_SØKNAD
        )

        val oppdatertBehandling = behandling
            .løsSykdom(periode.fom)
            .løsBistand(periode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Veldig relevante",
                        relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                        relevanteYrkesskadeSaker = person.yrkesskade.map {
                            YrkesskadeSakDto(
                                it.saksreferanse,
                                null,
                            )
                        },
                        andelAvNedsettelsen = 50,
                        erÅrsakssammenheng = true
                    )
                )
            )
            .løsBeregningstidspunkt()
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_INNVILGELSE)

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)

        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_YRKESSKADE),
        )
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Veldig relevante",
                        relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                        relevanteYrkesskadeSaker = person.yrkesskade.map {
                            YrkesskadeSakDto(
                                it.saksreferanse,
                                null,
                            )
                        },
                        andelAvNedsettelsen = 50,
                        erÅrsakssammenheng = true
                    )
                )
            )
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)


        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `ved revurdering med yrkesskade hvor yrkesskade fjernes må forutgående medlemskap vurderes da den igjen er aktuell`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val person = TestPersoner.PERSON_MED_YRKESSKADE()
        // Sender inn en søknad
        val (sak, behandling) = sendInnFørsteSøknad(
            periode = periode,
            person = person,
            søknad = TestSøknader.SØKNAD_INGEN_MEDLEMSKAP
        )

        val oppdatertBehandling = behandling
            .løsLovvalg(periode.fom)
            .løsSykdom(periode.fom)
            .løsBistand(periode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Ikke lenger relevant, dette var feil i fgbh",
                        relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                        relevanteYrkesskadeSaker = person.yrkesskade.map {
                            YrkesskadeSakDto(
                                it.saksreferanse,
                                null,
                            )
                        },
                        andelAvNedsettelsen = 50,
                        erÅrsakssammenheng = true
                    )
                )
            )
            .løsBeregningstidspunkt()
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_INNVILGELSE)

        assertThat(oppdatertBehandling.status()).isEqualTo(Status.AVSLUTTET)

        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_YRKESSKADE),
        )
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Ikke lenger relevant, dette var feil i fgbh",
                        relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                        relevanteYrkesskadeSaker = emptyList(),
                        andelAvNedsettelsen = null,
                        erÅrsakssammenheng = false
                    )
                )
            )
            .løsForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)

        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade - yrkesskade har årsakssammenheng`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val person = TestPersoner.PERSON_MED_YRKESSKADE()

        // Sender inn en søknad
        var (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling = behandling
            .medKontekst {
                assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
            }
            .leggTilVurderingsbehov(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_YRKESSKADE)
            .løsSykdom(
                vurderingGjelderFra = sak.rettighetsperiode.fom,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true
            )
            .løsBistand(sak.rettighetsperiode.fom)
            .løsAvklaringsBehov(
                RefusjonkravLøsning(
                    listOf(
                        RefusjonkravVurderingDto(
                            harKrav = true,
                            fom = LocalDate.now(),
                            tom = null,
                            navKontor = "",
                        )
                    )
                )
            )
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Veldig relevante",
                        relevanteSaker = person.yrkesskade.map { it.saksreferanse },
                        relevanteYrkesskadeSaker = person.yrkesskade.map {
                            YrkesskadeSakDto(
                                it.saksreferanse,
                                null,
                            )
                        },
                        andelAvNedsettelsen = 50,
                        erÅrsakssammenheng = true
                    )
                )
            )
            .løsBeregningstidspunkt()
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .medKontekst {
                // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { avklaringsbehov ->
                    assertThat(avklaringsbehov.definisjon).isEqualTo(
                        Definisjon.FORESLÅ_VEDTAK
                    )
                }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                // Saken står til To-trinnskontroll hos beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.FATTE_VEDTAK) }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                val brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
                // Det er bestilt vedtaksbrev som er klar for forhåndsvisning og editering
                assertThat(brevBestilling.status).isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR)

                // Venter på at brevet skal fullføres
                assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.SKRIV_VEDTAKSBREV) }

                val vilkårsresultat = dataSource.transaction { VilkårsresultatRepositoryImpl(it).hent(behandling.id) }
                    .finnVilkår(Vilkårtype.SYKDOMSVILKÅRET).tidslinje().komprimer()

                val underveisPeriode = dataSource.transaction {
                    UnderveisRepositoryImpl(it).hent(behandling.id)
                }.somTidslinje().helePerioden()

                assertTidslinje(
                    vilkårsresultat.begrensetTil(underveisPeriode),
                    Periode(periode.fom, underveisPeriode.tom) to {
                        assertThat(it.innvilgelsesårsak).isEqualTo(Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG)
                    })
            }
            .løsVedtaksbrev()
            .medKontekst {
                // Brevet er fullført
                val brevBestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
                assertThat(brevBestilling.status).isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT)
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(2)
            .anyMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }
            .anyMatch { vilkårsperiode -> !vilkårsperiode.erOppfylt() }

        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }

        // Verifiser at beregningsgrunnlaget er av type yrkesskade
        dataSource.transaction {
            assertThat(BeregningsgrunnlagRepositoryImpl(it).hentHvisEksisterer(behandling.id)?.javaClass).isEqualTo(
                GrunnlagYrkesskade::class.java
            )
        }
    }

    @Test
    fun `innvilge v yrkesskadegrunnlag`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        val person = TestPersoner.PERSON_MED_YRKESSKADE().medBarn(
            listOf(
                TestPerson(
                    identer = setOf(Ident("1234123")),
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(3)),
                )
            )
        )

        var (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode,
        )

        // Sender inn en søknad
        behandling
            .løsSykdom(sak.rettighetsperiode.fom) // erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true) // erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true)
            .løsBistand(sak.rettighetsperiode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .leggTilVurderingsbehov(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_YRKESSKADE)
        sak.sendInnMeldekort(
            journalpostId = JournalpostId("220"),
            mottattTidspunkt = LocalDateTime.now(),
            timerArbeidet = Periode(LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(3))
                .dager()
                .associateWith { 0.0 }
        )

        behandling = behandling.kvalitetssikreOk()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Ikke årsakssammenheng",
                        relevanteSaker = emptyList(),
                        relevanteYrkesskadeSaker = emptyList(),
                        andelAvNedsettelsen = null,
                        erÅrsakssammenheng = true
                    )
                ),
            )
            .løsBeregningstidspunkt()
            .løsYrkesskadeInntekt(person.yrkesskade)
            // Skal ikke løse forutgående medlemsskap
            .løsOppholdskrav(fom)
            .løsBarnetillegg()
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                // Saken står til To-trinnskontroll hos beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.FATTE_VEDTAK) }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
                // Venter på at brevet skal fullføres
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.SKRIV_VEDTAKSBREV) }
            }
            .løsVedtaksbrev()
            .medKontekst {
                val brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }

        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)

        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }
            .allMatch { vilkårsperiode -> vilkårsperiode.innvilgelsesårsak == Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG }
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val fom = LocalDate.now().minusMonths(3)
        val periode = Periode(fom, fom.plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        val virkningstidspunkt = LocalDate.now().minusYears(3)
        val person = TestPersoner.PERSON_MED_YRKESSKADE().medBarn(
            listOf(
                TestPerson(
                    identer = setOf(Ident("1234123")),
                    fødselsdato = Fødselsdato(virkningstidspunkt),
                )
            )
        ).medUføre(Prosent(50), virkningstidspunkt = virkningstidspunkt)

        var (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode,
        )

        // Sender inn en søknad
        behandling
            .medKontekst {
                assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
                assertThat(åpneAvklaringsbehov).isNotEmpty()
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .leggTilVurderingsbehov(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.REVURDER_YRKESSKADE)
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand(sak.rettighetsperiode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()

        sak.sendInnMeldekort(
            journalpostId = JournalpostId("220"),
            mottattTidspunkt = LocalDateTime.now(),
            timerArbeidet = Periode(LocalDate.now().minusMonths(3), LocalDate.now().plusMonths(3))
                .dager()
                .associateWith { 0.0 }
        )

        behandling = behandling.kvalitetssikreOk()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Ikke årsakssammenheng",
                        relevanteSaker = emptyList(),
                        relevanteYrkesskadeSaker = emptyList(),
                        andelAvNedsettelsen = null,
                        erÅrsakssammenheng = false
                    )
                ),
            )
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap(fom)
            .løsOppholdskrav(fom)
            .løsBarnetillegg()
            .løsAvklaringsBehov(
                AvklarSamordningUføreLøsning(
                    samordningUføreVurdering = SamordningUføreVurderingDto(
                        begrunnelse = "Samordnet med uføre",
                        vurderingPerioder = listOf(
                            SamordningUføreVurderingPeriodeDto(
                                virkningstidspunkt = virkningstidspunkt, uføregradTilSamordning = 50
                            )
                        )
                    )
                )
            )
            .løsAndreStatligeYtelser()
            .medKontekst {
                // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .beslutterGodkjennerIkke(returVed = Definisjon.AVKLAR_SYKDOM)
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand(sak.rettighetsperiode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsAvklaringsBehov(
                AvklarYrkesskadeLøsning(
                    yrkesskadesvurdering = YrkesskadevurderingDto(
                        begrunnelse = "Ikke årsakssammenheng",
                        relevanteSaker = emptyList(),
                        relevanteYrkesskadeSaker = emptyList(),
                        andelAvNedsettelsen = null,
                        erÅrsakssammenheng = false
                    )
                )
            )
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap(fom)
            .løsOppholdskrav(fom)
            .løsAvklaringsBehov(
                AvklarSamordningUføreLøsning(
                    samordningUføreVurdering = SamordningUføreVurderingDto(
                        begrunnelse = "Samordnet med uføre",
                        vurderingPerioder = listOf(
                            SamordningUføreVurderingPeriodeDto(
                                virkningstidspunkt = virkningstidspunkt, uføregradTilSamordning = 50
                            )
                        )
                    )
                )
            )
            .medKontekst {
                // Saken er tilbake til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.FORESLÅ_VEDTAK) }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                // Saken står til To-trinnskontroll hos beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.FATTE_VEDTAK) }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
            }

        // Skal feile dersom man prøver å sende til beslutter etter at vedtaket er fattet
        val avklaringsbehovFeil = org.junit.jupiter.api.assertThrows<UgyldigForespørselException> {
            løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())
        }
        assertThat(avklaringsbehovFeil.message).contains("Forsøker å løse avklaringsbehov FORESLÅ_VEDTAK(kode='5098') som er definert i et steg før nåværende steg[BREV]")
        val vedtak = hentVedtak(behandling.id)
        assertThat(vedtak.vedtakstidspunkt.toLocalDate()).isToday

        val brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)

        // Det er bestilt vedtaksbrev som er klar for forhåndsvisning og editering
        assertThat(brevbestilling.status).isEqualTo(
            no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FORHÅNDSVISNING_KLAR
        )

        behandling = behandling.medKontekst {
            // Venter på at brevet skal fullføres
            assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.SKRIV_VEDTAKSBREV) }
        }
            .løsVedtaksbrev()
            .medKontekst {
                val brevbestilling = hentBrevAvType(behandling, TypeBrev.VEDTAK_INNVILGELSE)
                // Brevet er fullført
                assertThat(brevbestilling.status).isEqualTo(
                    no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT
                )
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }

        // Henter vurder alder-vilkår
        // Assert utfall
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(2)
            .anyMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }
            .anyMatch { vilkårsperiode -> !vilkårsperiode.erOppfylt() }

        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }

        val underveisGrunnlag = dataSource.transaction { connection ->
            UnderveisRepositoryImpl(connection).hent(behandling.id)
        }

        assertThat(underveisGrunnlag.perioder).isNotEmpty
        assertThat(underveisGrunnlag.perioder.any { it.arbeidsgradering.gradering.prosentverdi() > 0 }).isTrue()

        // Saken er avsluttet, så det skal ikke være flere åpne avklaringsbehov
        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).isEmpty()

        sak.sendInnSøknad(
            søknad = TestSøknader.STANDARD_SØKNAD,
            mottattTidspunkt = LocalDateTime.now().minusMonths(3),
            journalpostId = JournalpostId("299"),
        ).medKontekst {
            assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
            assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
        }
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    løsningerForPerioder = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("1349532")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            fom = periode.fom,
                            tom = null,
                        )
                    )
                ),
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).anySatisfy { it.definisjon == Definisjon.AVKLAR_BISTANDSBEHOV }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
    }
}