package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.PdlHendelseKafkaKonsumentTest.Companion.repositoryRegistry
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.LøsAvklaringsbehovHendelse
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarManuellInntektVurderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOppholdskravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningAndreStatligeYtelserLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSoningsforholdLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettYrkesskadeInntektLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KvalitetssikringLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivVedtaksbrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SykdomsvurderingForBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TjenestepensjonRefusjonskravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.YrkesskadeSakDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.AvklarOppholdkravLøsningForPeriodeDto
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.AndreStatligeYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurderingPeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvarDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingerForBarnetillegg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarnDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningYrkeskaderBeløpVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.SoningsvurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.SoningsvurderingerDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.SamordningVurderingData
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonskravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.mellomlagring.MellomlagretVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.motor.testutil.ManuellMotorImpl
import no.nav.aap.verdityper.dokument.JournalpostId
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

class TestScenarioOrkestrator(
    private val gatewayProvider: GatewayProvider,
    private val datasource: DataSource,
    private val motor: ManuellMotorImpl
) {
    fun løsStudent(behandling: Behandling): Behandling {
        return løsAvklaringsBehov(
            behandling,
            AvklarStudentLøsning(
                studentvurdering = StudentVurderingDTO(
                    begrunnelse = "Er student ok",
                    harAvbruttStudie = true,
                    godkjentStudieAvLånekassen = true,
                    avbruttPgaSykdomEllerSkade = true,
                    harBehovForBehandling = true,
                    avbruttStudieDato = LocalDate.now().minusMonths(1),
                    avbruddMerEnn6Måneder = true,
                )
            )
        )
    }

    fun løsSykdom(
        behandling: Behandling,
        vurderingGjelderFra: LocalDate,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean? = null
    ): Behandling {
        return løsAvklaringsBehov(
            behandling,
            AvklarSykdomLøsning(
                løsningerForPerioder = listOf(
                    SykdomsvurderingLøsningDto(
                        begrunnelse = "Er syk nok",
                        dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                        harSkadeSykdomEllerLyte = true,
                        erSkadeSykdomEllerLyteVesentligdel = true,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                        erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
                        erArbeidsevnenNedsatt = true,
                        yrkesskadeBegrunnelse = if (erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense != null) "test" else null,
                        fom = vurderingGjelderFra,
                        tom = null
                    )
                )
            )
        )
    }

    fun løsYrkesSkade(behandling: Behandling): Behandling {
        val yrkesskadeMedDato = hentFørsteYrkesskadeMedSkadeDato(behandling.id)
        requireNotNull(yrkesskadeMedDato) { "Kan ikke løse yrkesskade uten at det finnes en yrkesskade med skadedato" }

        return løsAvklaringsBehov(
            behandling,
            AvklarYrkesskadeLøsning(
                yrkesskadesvurdering = YrkesskadevurderingDto(
                    begrunnelse = "Er yrkesskade",
                    relevanteSaker = emptyList(),
                    relevanteYrkesskadeSaker = listOf(
                        YrkesskadeSakDto(yrkesskadeMedDato.ref, null),
                    ),
                    andelAvNedsettelsen = 50,
                    erÅrsakssammenheng = true
                )
            )
        )
    }

    fun løsBistand(behandling: Behandling): Behandling {
        return løsAvklaringsBehov(
            behandling,
            AvklarBistandsbehovLøsning(
                BistandVurderingLøsningDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null,
                )
            )
        )
    }

    fun løsRefusjonskrav(behandling: Behandling): Behandling {
        return løsAvklaringsBehov(
            behandling,
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
    }

    fun løsSykdomsvurderingBrev(behandling: Behandling): Behandling {
        return løsAvklaringsBehov(
            behandling,
            avklaringsBehovLøsning = SykdomsvurderingForBrevLøsning(
                vurdering = "Denne vurderingen skal vises i brev"
            )
        )
    }

    fun kvalitetssikreOk(
        behandling: Behandling,
        bruker: Bruker = Bruker("KVALITETSSIKRER")
    ): Behandling {
        val alleAvklaringsbehov = hentAlleAvklaringsbehov(behandling.id, datasource)
        return løsAvklaringsBehov(
            behandling,
            KvalitetssikringLøsning(alleAvklaringsbehov.filter { behov -> behov.erTotrinn() || behov.kreverKvalitetssikring() }
                .map { behov ->
                    TotrinnsVurdering(
                        behov.definisjon.kode, true, "begrunnelse", emptyList()
                    )
                }),
            bruker
        )
    }

    fun løsBeregningstidspunkt(behandling: Behandling, dato: LocalDate = LocalDate.now()): Behandling {
        return løsAvklaringsBehov(
            behandling,
            FastsettBeregningstidspunktLøsning(
                beregningVurdering = BeregningstidspunktVurderingDto(
                    begrunnelse = "Trenger hjelp fra Nav",
                    nedsattArbeidsevneDato = dato,
                    ytterligereNedsattArbeidsevneDato = null,
                    ytterligereNedsattBegrunnelse = null
                ),
            ),
        )
    }

    fun løsManuellInntektVurdering(behandling: Behandling): Behandling {
        return løsAvklaringsBehov(
            behandling,
            AvklarManuellInntektVurderingLøsning(
                manuellVurderingForManglendeInntekt = ManuellInntektVurderingDto(
                    begrunnelse = "Manuell inntekt vurdering ok",
                    belop = BigDecimal(400000.00)
                )
            )
        )
    }

    fun løsFastsettYrkesskadeInntekt(behandling: Behandling): Behandling {
        val yrkesskadeMedDato = hentFørsteYrkesskadeMedSkadeDato(behandling.id)
        requireNotNull(yrkesskadeMedDato) { "Kan ikke løse fastsettYrkesskadeInntekt uten at det finnes en yrkesskade med skadedato" }

        val antattÅrligInntekt = Beløp(BigDecimal(500000.00))
        return løsAvklaringsBehov(
            behandling,
            FastsettYrkesskadeInntektLøsning(
                BeregningYrkeskaderBeløpVurderingDTO(
                    vurderinger = listOf(
                        YrkesskadeBeløpVurderingDTO(
                            antattÅrligInntekt,
                            referanse = yrkesskadeMedDato.ref,
                            begrunnelse = "vurdert ok"
                        )
                    )
                )
            )
        )
    }

    fun løsBarnetillegg(behandling: Behandling): Behandling {
        // Henter ut ident,fødselsdato og navn fra db.
        var vurderteBarnListe = listOf<VurdertBarnDto>()
        datasource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val barnRepository = repositoryProvider.provide<BarnRepository>()
            val barnGrunnlag = barnRepository.hent(behandling.id)
            val oppgitteBarnListe = barnGrunnlag.oppgitteBarn?.oppgitteBarn ?: emptyList()

            if (oppgitteBarnListe.isNotEmpty()) {
                vurderteBarnListe = oppgitteBarnListe.mapIndexed { index, barn ->
                    VurdertBarnDto(
                        ident = barn.ident?.identifikator,
                        navn = barn.navn,
                        fødselsdato = barn.fødselsdato?.toLocalDate(),
                        vurderinger = listOf(
                            VurderingAvForeldreAnsvarDto(
                                fraDato = LocalDate.now().minusMonths((index + 1).toLong()),
                                harForeldreAnsvar = barn.relasjon != null,
                                begrunnelse = if (barn.relasjon != null) "Har fullt ansvar" else "Har ikke fullt ansvar",
                                erFosterForelder = null
                            )
                        ),
                        oppgittForeldreRelasjon = barn.relasjon
                    )
                }
            }
        }

        return løsAvklaringsBehov(
            behandling,
            AvklarBarnetilleggLøsning(
                vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                    vurderteBarn = vurderteBarnListe,
                    saksbehandlerOppgitteBarn = emptyList(),
                )
            )
        )
    }

    fun løsUtenSamordning(behandling: Behandling): Behandling {
        return this.løsAvklaringsBehov(
            behandling,
            AvklarSamordningGraderingLøsning(
                vurderingerForSamordning = VurderingerForSamordning("", true, null, emptyList())
            )
        )
    }

    fun løsSamordningAndreStatligeYtelser(behandling: Behandling): Behandling {
        return this.løsAvklaringsBehov(
            behandling,
            AvklarSamordningAndreStatligeYtelserLøsning(
                SamordningAndreStatligeYtelserVurderingDto(
                    "Samordning statlige ytelser ok",
                    listOf(SamordningAndreStatligeYtelserVurderingPeriodeDto(
                        AndreStatligeYtelser.BARNEPENSJON,
                        Periode(LocalDate.now().minusMonths(3), LocalDate.now().minusMonths(2))
                    ))
                )
            )
        )
    }

    fun løsSamordning(behandling: Behandling, sykepengerList: List<TestPerson.Sykepenger>): Behandling {
        val samordningVurderinger = sykepengerList.map { sykepenger ->
            SamordningVurderingData(
                Ytelse.entries.random(), // tilfeldig ytelse
                Periode(sykepenger.periode.fom, sykepenger.periode.tom),
                gradering = (10..100).random() // tilfeldig verdi mellom 10 og 100
            )
        }

        return this.løsAvklaringsBehov(
            behandling,
            AvklarSamordningGraderingLøsning(
                vurderingerForSamordning = VurderingerForSamordning(
                    "samordning ok",
                    true,
                    null,
                    samordningVurderinger
                )
            )
        )
    }

    fun løsTjenestepensjonRefusjonskravVurdering(behandling: Behandling): Behandling {
        return løsAvklaringsBehov(
            behandling,
            TjenestepensjonRefusjonskravLøsning(
                samordningRefusjonskrav = TjenestepensjonRefusjonskravVurdering(
                    harKrav = false,
                    begrunnelse = "Samordning refusjonskrav ok",
                    fom = null,
                    tom = null
                )
            )
        )
    }

    fun løsForutgåendeMedlemskap(behandling: Behandling): Behandling {
        return løsAvklaringsBehov(
            behandling,
            AvklarForutgåendeMedlemskapLøsning(
                ManuellVurderingForForutgåendeMedlemskapDto(
                    begrunnelse = "Forutgående medlemskap ok",
                    harForutgåendeMedlemskap = true,
                    varMedlemMedNedsattArbeidsevne = false,
                    medlemMedUnntakAvMaksFemAar = false
                )
            )
        )
    }

    fun løsOppholdskrav(behandling: Behandling): Behandling {
        return løsAvklaringsBehov(
            behandling,
            AvklarOppholdskravLøsning(
                løsningerForPerioder = listOf(
                    AvklarOppholdkravLøsningForPeriodeDto(
                        begrunnelse = "Oppholdskrav ok",
                        fom = LocalDate.now().minusMonths(2),
                        tom = null,
                        oppfylt = true,
                        land = "Norge"
                    )
                )
            )
        )
    }

    fun løsSoningsforhold(behandling: Behandling): Behandling {
        return løsAvklaringsBehov(
            behandling,
            AvklarSoningsforholdLøsning(
                soningsvurdering = SoningsvurderingerDto(
                    listOf(
                        SoningsvurderingDto(
                            begrunnelse = "Soningsforhold ok",
                            skalOpphore = false,
                            fraDato = LocalDate.now()
                        )
                    )
                )
            )
        )
    }

    /**
     * Løser avklaringsbehov og venter på svar.
     */
    fun løsAvklaringsBehov(
        behandling: Behandling,
        avklaringsBehovLøsning: AvklaringsbehovLøsning,
        bruker: Bruker = Bruker("SAKSBEHANDLER"),
        ingenEndringIGruppe: Boolean = false,
    ): Behandling {
        datasource.transaction {
            AvklaringsbehovHendelseHåndterer(
                AvklaringsbehovOrkestrator(postgresRepositoryRegistry.provider(it), gatewayProvider),
                AvklaringsbehovRepositoryImpl(it),
                BehandlingRepositoryImpl(it),
                MellomlagretVurderingRepositoryImpl(it),
            ).håndtere(
                behandling.id, LøsAvklaringsbehovHendelse(
                    løsning = avklaringsBehovLøsning,
                    behandlingVersjon = behandling.versjon,
                    bruker = bruker,
                    ingenEndringIGruppe = ingenEndringIGruppe
                )
            )
        }
        motor.kjørJobber()
        return hentBehandling(behandling.referanse, datasource)
    }

    fun løsForeslåVedtakLøsning(behandling: Behandling) {
        løsAvklaringsBehov(behandling, ForeslåVedtakLøsning())
    }

    fun fattVedtakEllerSendRetur(behandling: Behandling, returVed: Definisjon? = null): Behandling =
        løsAvklaringsBehov(
            behandling,
            FatteVedtakLøsning(
                hentAlleAvklaringsbehov(behandling.id, datasource)
                    .filter { behov -> behov.erTotrinn() }
                    .map { behov ->
                        TotrinnsVurdering(
                            behov.definisjon.kode, behov.definisjon != returVed, "begrunnelse", emptyList()
                        )
                    }),
            Bruker("BESLUTTER")
        )

    fun løsVedtaksbrev(behandling: Behandling, typeBrev: TypeBrev = TypeBrev.VEDTAK_INNVILGELSE): Behandling {
        val brevbestilling = hentBrevAvType(behandling, typeBrev)

        return this.løsAvklaringsBehov(behandling, vedtaksbrevLøsning(brevbestilling.referanse.brevbestillingReferanse))
    }

    private fun hentFørsteYrkesskadeMedSkadeDato(behandlingId: BehandlingId): Yrkesskade? {
        var yrkesskadeUtenSkadedato: Yrkesskade? = null
        datasource.transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val yrkesskadeRepository = repositoryProvider.provide<YrkesskadeRepository>()
            yrkesskadeUtenSkadedato =
                yrkesskadeRepository.hentHvisEksisterer(behandlingId)?.yrkesskader?.yrkesskader?.first { it.skadedato != null }
        }
        return yrkesskadeUtenSkadedato
    }

    private fun vedtaksbrevLøsning(brevbestillingReferanse: UUID): AvklaringsbehovLøsning {
        return SkrivVedtaksbrevLøsning(
            brevbestillingReferanse = brevbestillingReferanse,
            handling = SkrivBrevAvklaringsbehovLøsning.Handling.FERDIGSTILL
        )
    }

    private fun hentBrevAvType(behandling: Behandling, typeBrev: TypeBrev) =
        datasource.transaction(readOnly = true) { connection ->
            val brev = BrevbestillingRepositoryImpl(connection).hent(behandling.id)
            brev.firstOrNull { it.typeBrev == typeBrev }
                ?: error("Ingen brev av type $typeBrev. Følgende finnes: ${brev.joinToString { it.typeBrev.toString() }}")
        }

    private fun hentBehandling(behandlingReferanse: BehandlingReferanse, datasource: DataSource): Behandling {
        return datasource.transaction(readOnly = true) { connection ->
            BehandlingRepositoryImpl(connection).hent(behandlingReferanse)
        }
    }

    private fun hentAlleAvklaringsbehov(behandlingId: BehandlingId, datasource: DataSource): List<Avklaringsbehov> {
        return datasource.transaction(readOnly = true) {
            AvklaringsbehovRepositoryImpl(it).hentAvklaringsbehovene(
                behandlingId
            ).alle()
        }
    }

}