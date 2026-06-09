package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarManuellInntektVurderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOppholdskravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarPeriodisertForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarPeriodisertLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningAndreStatligeYtelserLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningSykestipendLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BekreftVurderingerOppfølgingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettYrkesskadeInntektLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.KvalitetssikringLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TjenestepensjonRefusjonskravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonskravVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivVedtaksbrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SykdomsvurderingForBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.YrkesskadeSakDto
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.YrkesskadevurderingDto
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.AvklarOppholdkravLøsningForPeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.AndreStatligeYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurderingPeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.PeriodisertManuellVurderingForForutgåendeMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningYrkeskaderBeløpVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ÅrsVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SamordningSykestipendVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.PeriodisertManuellVurderingForLovvalgMedlemskapDto
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLandEllerLandMedAvtale
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.PeriodisertStudentDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
import javax.sql.DataSource

private val SAKSBEHANDLER = Bruker("SAKSBEHANDLER")
private val BESLUTTER = Bruker("BESLUTTER")
private val KVALITETSSIKRER = Bruker("KVALITETSSIKRER")
private const val MAKS_ITERASJONER = 200

class TestBehandlingFullføringService(
    private val dataSource: DataSource,
    private val repositoryRegistry: RepositoryRegistry,
    private val gatewayProvider: GatewayProvider,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun fullforBehandling(sak: Sak, ventPåNyBehandling: Boolean = false) {
        val behandlingId = ventPåÅpenBehandlingOgReturnerBehandlingId(sak, ventPåNyBehandling) ?: run {
            log.error("Fant ingen åpen behandling for sak ${sak.id} innen tidsgrensen")
            return
        }

        val sisteBehandlingId = (1..MAKS_ITERASJONER).asSequence()
            .takeWhile { !erBehandlingAvsluttet(behandlingId) }
            .fold<Int, BehandlingId?>(null) { forrige, _ ->
                prosesserNesteSteg(sak, behandlingId) ?: forrige
            }

        if (!erBehandlingAvsluttet(behandlingId)) {
            log.error("Behandling ${sisteBehandlingId ?: behandlingId} ble ikke avsluttet innen $MAKS_ITERASJONER iterasjoner")
        }
    }

    private fun ventPåÅpenBehandlingOgReturnerBehandlingId(sak: Sak, ventPåNyBehandling: Boolean): BehandlingId? {
        val maksForsøk = if (ventPåNyBehandling) 300 else 150
        repeat(maksForsøk) {
            val behandling = dataSource.transaction(readOnly = true) { connection ->
                BehandlingService(repositoryRegistry.provider(connection), gatewayProvider)
                    .finnSisteYtelsesbehandlingFor(sak.id)
            }
            if (behandling != null && behandling.status() != Status.AVSLUTTET) {
                return behandling.id
            }
            Thread.sleep(200)
        }
        log.error("Tidsavbrudd ved venting på åpen behandling for sak ${sak.id}")
        return null
    }

    private fun erBehandlingAvsluttet(behandlingId: BehandlingId): Boolean {
        val behandling = dataSource.transaction(readOnly = true) { connection ->
            repositoryRegistry.provider(connection).provide<BehandlingRepository>().hent(behandlingId)
        }
        return behandling.status() == Status.AVSLUTTET
    }

    /**
     * Henter avklaringsbehov og løser det første åpne. Returnerer behandlingId hvis et behov ble løst.
     */
    @Suppress("ReturnCount")
    private fun prosesserNesteSteg(sak: Sak, forventetBehandlingId: BehandlingId): BehandlingId? {
        val behandling = dataSource.transaction(readOnly = true) { connection ->
            BehandlingService(repositoryRegistry.provider(connection), gatewayProvider)
                .finnSisteYtelsesbehandlingFor(sak.id)
        }

        if (behandling == null || behandling.id != forventetBehandlingId || behandling.status() == Status.AVSLUTTET) {
            Thread.sleep(200)
            return null
        }

        val alleAvklaringsbehov = dataSource.transaction(readOnly = true) { connection ->
            repositoryRegistry.provider(connection)
                .provide<AvklaringsbehovRepository>()
                .hentAvklaringsbehovene(behandling.id)
                .alle()
        }

        val åpentBehov = alleAvklaringsbehov
            .firstOrNull { it.erÅpent() && !it.definisjon.erVentebehov() }

        if (åpentBehov == null) {
            Thread.sleep(200)
            return behandling.id
        }

        val løsning = lagLøsning(åpentBehov, alleAvklaringsbehov, sak, behandling.id)
        if (løsning == null) {
            log.error("Ukjent avklaringsbehov: ${åpentBehov.definisjon} — avbryter fullføring")
            return null
        }

        val bruker = when (åpentBehov.definisjon) {
            Definisjon.FATTE_VEDTAK -> BESLUTTER
            Definisjon.KVALITETSSIKRING -> KVALITETSSIKRER
            else -> SAKSBEHANDLER
        }

        log.info("Løser avklaringsbehov ${åpentBehov.definisjon} for behandling ${behandling.id}")
        dataSource.transaction { connection ->
            AvklaringsbehovOrkestrator(repositoryRegistry.provider(connection), gatewayProvider)
                .løsAvklaringsbehovOgFortsettProsessering(behandling.id, løsning, bruker)
        }

        Thread.sleep(200)
        return behandling.id
    }

    private fun lagLøsning(
        behov: Avklaringsbehov,
        alleAvklaringsbehov: List<Avklaringsbehov>,
        sak: Sak,
        behandlingId: BehandlingId,
    ): AvklaringsbehovLøsning? = when (behov.definisjon) {
        Definisjon.AVKLAR_STUDENT -> AvklarStudentLøsning(
            løsningerForPerioder = listOf(
                PeriodisertStudentDto(
                    fom = sak.rettighetsperiode.fom,
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

        Definisjon.AVKLAR_SYKDOM -> AvklarSykdomLøsning(
            løsningerForPerioder = listOf(
                SykdomsvurderingLøsningDto(
                    begrunnelse = "Er syk nok",
                    dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                    harSkadeSykdomEllerLyte = true,
                    kodeverk = "ICPC2",
                    hoveddiagnose = "A03",
                    harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                    yrkesskadeBegrunnelse = null,
                    fom = sak.rettighetsperiode.fom,
                    tom = null,
                )
            )
        )

        Definisjon.AVKLAR_YRKESSKADE -> {
            val yrkesskade = hentFørsteYrkesskade(behandlingId)
            requireNotNull(yrkesskade) { "Kan ikke løse yrkesskade uten yrkesskade med skadedato" }
            AvklarYrkesskadeLøsning(
                yrkesskadesvurdering = YrkesskadevurderingDto(
                    begrunnelse = "Er yrkesskade",
                    relevanteSaker = emptyList(),
                    relevanteYrkesskadeSaker = listOf(YrkesskadeSakDto(yrkesskade.ref, null)),
                    andelAvNedsettelsen = 50,
                    erÅrsakssammenheng = true,
                )
            )
        }

        Definisjon.AVKLAR_BISTANDSBEHOV -> AvklarBistandsbehovLøsning(
            listOf(
                BistandLøsningDto(
                    fom = sak.rettighetsperiode.fom,
                    begrunnelse = "Trenger hjelp fra Nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null,
                    tom = null,
                )
            )
        )

        Definisjon.REFUSJON_KRAV -> RefusjonkravLøsning(
            listOf(RefusjonkravVurderingDto(harKrav = true, navKontor = ""))
        )

        Definisjon.SKRIV_SYKDOMSVURDERING_BREV -> SykdomsvurderingForBrevLøsning(
            vurdering = "Vurdering for brev"
        )

        Definisjon.BEKREFT_VURDERINGER_OPPFØLGING -> BekreftVurderingerOppfølgingLøsning()

        Definisjon.KVALITETSSIKRING -> KvalitetssikringLøsning(
            alleAvklaringsbehov
                .filter { it.erIkkeAvbrutt() && (it.erTotrinn() || it.kreverKvalitetssikring()) }
                .map {
                    TotrinnsVurdering(
                        it.definisjon.kode,
                        true,
                        "begrunnelse",
                        emptyList(),
                        markeringer = emptyList()
                    )
                }
        )

        Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT -> FastsettBeregningstidspunktLøsning(
            beregningVurdering = BeregningstidspunktVurderingDto(
                begrunnelse = "Beregningstidspunkt ok",
                nedsattArbeidsevneDato = sak.rettighetsperiode.fom,
                ytterligereNedsattArbeidsevneDato = null,
                ytterligereNedsattBegrunnelse = null,
            )
        )

        Definisjon.FASTSETT_MANUELL_INNTEKT -> AvklarManuellInntektVurderingLøsning(
            manuellVurderingForManglendeInntekt = ManuellInntektVurderingDto(
                begrunnelse = "Manuell inntekt vurdering ok",
                belop = null,
                vurderinger = listOf(
                    ÅrsVurdering(
                        beløp = BigDecimal("500000.00"),
                        eøsBeløp = null,
                        år = LocalDate.now().year - 1,
                        ferdigLignetPGI = null,
                    ),
                    ÅrsVurdering(
                        beløp = BigDecimal("500000.00"),
                        eøsBeløp = null,
                        år = LocalDate.now().year - 2,
                        ferdigLignetPGI = null,
                    ),
                    ÅrsVurdering(
                        beløp = BigDecimal("500000.00"),
                        eøsBeløp = null,
                        år = LocalDate.now().year - 3,
                        ferdigLignetPGI = null,
                    )
                )
            )
        )

        Definisjon.FASTSETT_YRKESSKADEINNTEKT -> {
            val yrkesskade = hentFørsteYrkesskade(behandlingId)
            requireNotNull(yrkesskade) { "Kan ikke løse yrkesskadeinntekt uten yrkesskade med skadedato" }
            FastsettYrkesskadeInntektLøsning(
                BeregningYrkeskaderBeløpVurderingDTO(
                    vurderinger = listOf(
                        YrkesskadeBeløpVurderingDTO(
                            antattÅrligInntekt = Beløp(BigDecimal("500000.00")),
                            referanse = yrkesskade.ref,
                            begrunnelse = "Vurdert ok",
                        )
                    )
                )
            )
        }

        Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP -> AvklarPeriodisertLovvalgMedlemskapLøsning(
            løsningerForPerioder = listOf(
                PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                    fom = sak.rettighetsperiode.fom,
                    tom = null,
                    begrunnelse = "Lovvalg Norge ok",
                    lovvalg = LovvalgDto(
                        begrunnelse = "Norsk lovvalg",
                        lovvalgsEØSLandEllerLandMedAvtale = EØSLandEllerLandMedAvtale.NOR,
                    ),
                    medlemskap = MedlemskapDto(
                        begrunnelse = "Medlem i folketrygden",
                        varMedlemIFolketrygd = true,
                    ),
                )
            )
        )

        Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP -> AvklarPeriodisertForutgåendeMedlemskapLøsning(
            løsningerForPerioder = listOf(
                PeriodisertManuellVurderingForForutgåendeMedlemskapDto(
                    fom = sak.rettighetsperiode.fom,
                    tom = null,
                    begrunnelse = "Forutgående medlemskap ok",
                    harForutgåendeMedlemskap = true,
                    varMedlemMedNedsattArbeidsevne = null,
                    medlemMedUnntakAvMaksFemAar = null,
                )
            )
        )

        Definisjon.AVKLAR_OPPHOLDSKRAV -> AvklarOppholdskravLøsning(
            løsningerForPerioder = listOf(
                AvklarOppholdkravLøsningForPeriodeDto(
                    begrunnelse = "Oppholdskrav ok",
                    fom = sak.rettighetsperiode.fom,
                    tom = null,
                    oppfylt = true,
                    land = "Norge",
                )
            )
        )

        Definisjon.AVKLAR_SAMORDNING_GRADERING -> AvklarSamordningGraderingLøsning(
            vurderingerForSamordning = VurderingerForSamordning("", true, null, emptyList())
        )

        Definisjon.AVKLAR_SAMORDNING_SYKESTIPEND -> AvklarSamordningSykestipendLøsning(
            sykestipendVurdering = SamordningSykestipendVurderingDto(
                begrunnelse = "Ingen sykestipend",
                perioder = emptySet(),
            )
        )

        Definisjon.SAMORDNING_ANDRE_STATLIGE_YTELSER -> AvklarSamordningAndreStatligeYtelserLøsning(
            SamordningAndreStatligeYtelserVurderingDto(
                "Ingen andre statlige ytelser",
                listOf(
                    SamordningAndreStatligeYtelserVurderingPeriodeDto(
                        AndreStatligeYtelser.BARNEPENSJON,
                        Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.fom.plusMonths(1))
                    )
                )
            )
        )

        Definisjon.SAMORDNING_REFUSJONS_KRAV -> TjenestepensjonRefusjonskravLøsning(
            samordningRefusjonskrav = TjenestepensjonRefusjonskravVurdering(
                harKrav = false,
                fom = null,
                tom = null,
                begrunnelse = "Ingen refusjonskrav",
            )
        )

        Definisjon.FORESLÅ_VEDTAK -> ForeslåVedtakLøsning()

        Definisjon.FATTE_VEDTAK -> FatteVedtakLøsning(
            alleAvklaringsbehov
                .filter { it.erIkkeAvbrutt() && it.erTotrinn() }
                .map {
                    TotrinnsVurdering(
                        it.definisjon.kode,
                        true,
                        "begrunnelse",
                        emptyList(),
                        markeringer = emptyList()
                    )
                }
        )

        Definisjon.SKRIV_VEDTAKSBREV -> {
            val brevbestilling = dataSource.transaction(readOnly = true) { connection ->
                repositoryRegistry.provider(connection)
                    .provide<BrevbestillingRepository>()
                    .hent(behandlingId)
                    .firstOrNull { it.typeBrev.erVedtak() && !it.typeBrev.erAutomatiskBrev() }
                    ?: error("Fant ikke vedtaksbrev for behandling $behandlingId")
            }
            SkrivVedtaksbrevLøsning(
                brevbestillingReferanse = brevbestilling.referanse.brevbestillingReferanse,
                handling = SkrivBrevAvklaringsbehovLøsning.Handling.FERDIGSTILL,
            )
        }

        else -> null
    }

    private fun hentFørsteYrkesskade(behandlingId: BehandlingId) =
        dataSource.transaction(readOnly = true) { connection ->
            repositoryRegistry.provider(connection)
                .provide<YrkesskadeRepository>()
                .hentHvisEksisterer(behandlingId)
                ?.yrkesskader
                ?.yrkesskader
                ?.firstOrNull { it.skadedato != null }
        }
}
