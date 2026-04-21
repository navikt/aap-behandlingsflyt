package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOppholdskravLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOvergangUføreLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarPeriodisertLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentEnkelLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarVedtakslengdeLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertAvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.AvklarOppholdkravLøsningForPeriodeDto
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeService
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLandEllerLandMedAvtale
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.PeriodisertManuellVurderingForLovvalgMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.UføreSøknadVedtakResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.flate.OvergangUføreLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.PeriodisertSykepengerVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurderingDto
import no.nav.aap.behandlingsflyt.flyt.TestSøknader.SØKNAD_INGEN_MEDLEMSKAP
import no.nav.aap.behandlingsflyt.integrasjon.defaultGatewayProvider
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.prosessering.OpprettJobbUtvidVedtakslengdeJobbUtfører
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepositoryImpl
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

object VedtakslengdeFlytUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.UtvidVedtakslengdeUnderEttAr,
    )
)

object AvklarVedtakslengdeFlytUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.VedtakslengdeAvklaringsbehov,
        BehandlingsflytFeature.OpprettManuellVedtakslengdeBehandling
    )
)

class VedtakslengdeFlytTest : AbstraktFlytOrkestratorTest(VedtakslengdeFlytUnleash::class) {

    private val clock = fixedClock(1 desember 2025)

    @Test
    fun `skal sette sluttdato ett år frem i tid når siste oppfylte rettighetstype er bistand ved førstegangsbehandling`() {
        val søknadstidspunkt = LocalDateTime.now(clock)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt)
        val startDato = sak.rettighetsperiode.fom

        førstegangsbehandling
            .løsSykdom(startDato, erOppfylt = true)
            .løsBistand(startDato, erOppfylt = true)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsOppholdskrav(startDato)
            .løsAndreStatligeYtelser()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                    startDato.plussEtÅrMedHverdager(
                        ÅrMedHverdager.FØRSTE_ÅR
                    )
                )
            }
    }

    @Test
    fun `skal holde sluttdato uendret når siste oppfylte rettighetstype fortsatt er bistand ved revurdering`() {
        val sak = happyCaseFørstegangsbehandling(LocalDate.now(clock))
        val endringsdato = sak.rettighetsperiode.fom.plusMonths(10)

        /* Gir AAP som arbeidssøker. */
        sak.opprettManuellRevurdering(
            Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
        )
            .løsSykdom(vurderingGjelderFra = endringsdato, erOppfylt = true)
            .løsBistand(endringsdato, erOppfylt = true)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                    sak.rettighetsperiode.fom.plusMonths(
                        12
                    ).minusDays(1)
                )
            }
    }

    @Test
    fun `skal sette sluttdato 6 måneder frem i tid når siste oppfylte rettighetstype er overgang arbeid ved førstegangsbehandling`() {
        val søknadstidspunkt = LocalDateTime.now(clock)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt)
        val startDato = sak.rettighetsperiode.fom
        val overgangDato = startDato.plusMonths(1)

        førstegangsbehandling
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    løsningerForPerioder = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123128")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseMinstHalvparten = null,
                            erNedsettelseMerEnnYrkesskadegrense = null,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            fom = startDato,
                            tom = overgangDato.minusDays(1)
                        ),
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Ikke syk",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123128")),
                            harSkadeSykdomEllerLyte = false,
                            erSkadeSykdomEllerLyteVesentligdel = false,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = false,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = false,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erNedsettelseMinstHalvparten = null,
                            erNedsettelseMerEnnYrkesskadegrense = null,
                            erArbeidsevnenNedsatt = false,
                            yrkesskadeBegrunnelse = null,
                            fom = overgangDato,
                            tom = null
                        ),
                    )
                )

            )
            .løsAvklaringsBehov(
                AvklarBistandsbehovLøsning(
                    løsningerForPerioder = listOf(
                        BistandLøsningDto(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForAktivBehandling = true,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = null,
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = null,
                            fom = startDato,
                            tom = overgangDato.minusDays(1)
                        ),
                        BistandLøsningDto(
                            fom = overgangDato,
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForAktivBehandling = false,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = false,
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = "Skal over i arbeid",
                            tom = null
                        ),
                    )
                )
            )
            .løsOvergangArbeid(Utfall.OPPFYLT, fom = overgangDato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsOppholdskrav(startDato)
            .løsAndreStatligeYtelser()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                    overgangDato.plusMonths(6).minusDays(1)
                )
            }
    }

    @Test
    fun `skal sette sluttdato 14 måneder frem i tid når siste oppfylte rettighetstype er overgang arbeid ved førstegangsbehandlin og overgang er 8 måneder frem i tid`() {
        val søknadstidspunkt = LocalDateTime.now(clock)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt)
        val startDato = sak.rettighetsperiode.fom
        val overgangDato = startDato.plusMonths(8)

        førstegangsbehandling
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    løsningerForPerioder = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123128")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseMinstHalvparten = null,
                            erNedsettelseMerEnnYrkesskadegrense = null,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            fom = startDato,
                            tom = overgangDato.minusDays(1)
                        ),
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Ikke syk",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123128")),
                            harSkadeSykdomEllerLyte = false,
                            erSkadeSykdomEllerLyteVesentligdel = false,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = false,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = false,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erNedsettelseMinstHalvparten = null,
                            erNedsettelseMerEnnYrkesskadegrense = null,
                            erArbeidsevnenNedsatt = false,
                            yrkesskadeBegrunnelse = null,
                            fom = overgangDato,
                            tom = null
                        ),
                    )
                )

            )
            .løsAvklaringsBehov(
                AvklarBistandsbehovLøsning(
                    løsningerForPerioder = listOf(
                        BistandLøsningDto(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForAktivBehandling = true,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = null,
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = null,
                            fom = startDato,
                            tom = overgangDato.minusDays(1)
                        ),
                        BistandLøsningDto(
                            fom = overgangDato,
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForAktivBehandling = false,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = false,
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = "Skal over i arbeid",
                            tom = null
                        ),
                    )
                )
            )
            .løsOvergangArbeid(Utfall.OPPFYLT, fom = overgangDato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsOppholdskrav(startDato)
            .løsAndreStatligeYtelser()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                    overgangDato.plusMonths(6).minusDays(1)
                )
            }
    }

    @Test
    fun `skal sette sluttdato til og med unntaksvilkåret, også når siste oppfylte rettighetstype er bistand`() {
        val søknadstidspunkt = LocalDateTime.now(clock)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt)
        val startDato = sak.rettighetsperiode.fom
        val overgangDato = startDato.plusMonths(8)
        val tilbakeTilBistandDato = startDato.plusMonths(14)

        førstegangsbehandling
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    løsningerForPerioder = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123128")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseMinstHalvparten = null,
                            erNedsettelseMerEnnYrkesskadegrense = null,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            fom = startDato,
                            tom = overgangDato.minusDays(1)
                        ),
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Ikke syk",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123128")),
                            harSkadeSykdomEllerLyte = false,
                            erSkadeSykdomEllerLyteVesentligdel = false,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = false,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = false,
                            erNedsettelseMinstHalvparten = null,
                            erNedsettelseMerEnnYrkesskadegrense = null,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = false,
                            yrkesskadeBegrunnelse = null,
                            fom = overgangDato,
                            tom = tilbakeTilBistandDato.minusDays(1)
                        ),
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123128")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseMinstHalvparten = null,
                            erNedsettelseMerEnnYrkesskadegrense = null,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            fom = tilbakeTilBistandDato,
                            tom = null
                        ),
                    )
                )

            )
            .løsAvklaringsBehov(
                AvklarBistandsbehovLøsning(
                    løsningerForPerioder = listOf(
                        BistandLøsningDto(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForAktivBehandling = true,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = null,
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = null,
                            fom = startDato,
                            tom = overgangDato.minusDays(1)
                        ),
                        BistandLøsningDto(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForAktivBehandling = false,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = false,
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = "Skal over i arbeid",
                            fom = overgangDato,
                            tom = tilbakeTilBistandDato.minusDays(1)
                        ),
                        BistandLøsningDto(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForAktivBehandling = true,
                            erBehovForArbeidsrettetTiltak = false,
                            erBehovForAnnenOppfølging = null,
                            skalVurdereAapIOvergangTilArbeid = null,
                            overgangBegrunnelse = null,
                            fom = tilbakeTilBistandDato,
                            tom = null
                        ),
                    )
                )
            )
            .løsOvergangArbeid(Utfall.OPPFYLT, fom = overgangDato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsOppholdskrav(startDato)
            .løsAndreStatligeYtelser()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                    overgangDato.plusMonths(6).minusDays(1)
                )
            }
    }

    @Test
    fun `skal sette sluttdato 14 måneder frem i tid ved overgang arbeid som siste oppfylte rettighetstype ved revurdering`() {
        val sak = happyCaseFørstegangsbehandling(LocalDate.now(clock))
        val endringsdato = sak.rettighetsperiode.fom.plusMonths(8)

        /* Gir AAP som arbeidssøker. */
        sak.opprettManuellRevurdering(
            Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
        )
            .løsSykdom(vurderingGjelderFra = endringsdato, erOppfylt = false)
            .løsBistand(endringsdato, erOppfylt = false)
            .løsOvergangArbeid(Utfall.OPPFYLT, fom = endringsdato)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                    endringsdato.plusMonths(6).minusDays(1)
                )
            }
    }

    @Test
    fun `skal sette sluttdato 8 måneder frem i tid når siste oppfylte rettighetstype er overgang uføre ved førstegangsbehandling`() {
        val søknadstidspunkt = LocalDateTime.now(clock)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt)
        val startDato = sak.rettighetsperiode.fom

        førstegangsbehandling
            .løsSykdom(startDato, vissVarighet = true, erOppfylt = true)
            .løsBistand(startDato, false)
            .løsAvklaringsBehov(
                AvklarOvergangUføreLøsning(
                    løsningerForPerioder = listOf(
                        OvergangUføreLøsningDto(
                            begrunnelse = "Overgang uføre ok",
                            brukerHarSøktOmUføretrygd = true,
                            brukerHarFåttVedtakOmUføretrygd = UføreSøknadVedtakResultat.NEI,
                            brukerRettPåAAP = true,
                            overgangBegrunnelse = null,
                            fom = startDato,
                            tom = null,
                        )
                    )
                )
            )
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsOppholdskrav(startDato)
            .løsAndreStatligeYtelser()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                    startDato.plusMonths(8).minusDays(1)
                )
            }
    }

    @Test
    fun `skal sette sluttdato 6 måneder frem i tid når siste oppfylte rettighetstype er student ved førstegangsbehandling`() {
        val søknadstidspunkt = LocalDateTime.now(clock)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(
            søknad = TestSøknader.SØKNAD_STUDENT,
            mottattTidspunkt = søknadstidspunkt
        )

        val startDato = sak.rettighetsperiode.fom

        førstegangsbehandling
            .løsAvklaringsBehov(
                AvklarStudentEnkelLøsning(
                    studentvurdering = StudentVurderingDTO(
                        begrunnelse = "Student ok",
                        harAvbruttStudie = true,
                        godkjentStudieAvLånekassen = true,
                        avbruttPgaSykdomEllerSkade = true,
                        harBehovForBehandling = true,
                        avbruttStudieDato = startDato,
                        avbruddMerEnn6Måneder = true
                    ),
                )
            )
            .løsSykdom(vurderingGjelderFra = startDato.plusMonths(6), erOppfylt = false)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsOppholdskrav(startDato)
            .løsSykestipend()
            .løsAndreStatligeYtelser()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                    startDato.plusMonths(6).minusDays(1)
                )
            }
    }

    @Test
    fun `skal beholde vedtatte underveisperioder dersom vedtakslengde innskrenkes pga endret rettighetstype ved revurdering`() {
        val sak = happyCaseFørstegangsbehandling(LocalDate.now(clock))
        val endringsdato = sak.rettighetsperiode.fom.plusMonths(2)

        dataSource.transaction { connection ->
            val underveisRepository = UnderveisRepositoryImpl(connection)
            val vedtakslengdeRepository = VedtakslengdeRepositoryImpl(connection)
            val behandling = BehandlingService(
                postgresRepositoryRegistry.provider(connection),
                defaultGatewayProvider { }).finnSisteYtelsesbehandlingFor(sak.id)
            val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling!!.id)
            val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxByOrNull { it.periode.tom }
            assertThat(sisteUnderveisperiode?.periode?.tom).isEqualTo(
                vedtakslengdeRepository.hentHvisEksisterer(behandling.id)!!.gjeldendeVurdering()?.sluttdato
            )
        }

        /* Gir AAP som arbeidssøker. */
        sak.opprettManuellRevurdering(
            Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
        )
            .løsSykdom(vurderingGjelderFra = endringsdato, erOppfylt = false)
            .løsBistand(endringsdato, erOppfylt = false)
            .løsOvergangArbeid(Utfall.OPPFYLT, fom = endringsdato)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val underveisRepository: UnderveisRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)
                val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling.id)
                val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxByOrNull { it.periode.tom }
                assertThat(sisteUnderveisperiode?.periode?.tom).isEqualTo(
                    sak.rettighetsperiode.fom.plussEtÅrMedHverdager(
                        ÅrMedHverdager.FØRSTE_ÅR
                    )
                )
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                    sak.rettighetsperiode.fom.plussEtÅrMedHverdager(
                        ÅrMedHverdager.FØRSTE_ÅR
                    )
                )
            }
    }

    @Test
    fun `forleng vedtak med passert slutt`() {
        val søknadstidspunkt = LocalDateTime.now(clock).minusYears(1)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt)
        val rettighetsperiode = sak.rettighetsperiode
        val startDato = sak.rettighetsperiode.fom

        førstegangsbehandling
            .løsSykdom(startDato)
            .løsBistand(startDato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsOppholdskrav(startDato)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)


        val sluttdatoFørstegangsbehandling = dataSource.transaction { connection ->
            val førstegangsbehandling = BehandlingRepositoryImpl(connection).finnFørstegangsbehandling(sak.id)

            val underveisGrunnlag = UnderveisRepositoryImpl(connection).hentHvisEksisterer(førstegangsbehandling.id)
            val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!
            assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(
                rettighetsperiode.fom.plussEtÅrMedHverdager(
                    ÅrMedHverdager.FØRSTE_ÅR
                )
            )
            assertThat(sisteUnderveisperiode.utfall).isEqualTo(Utfall.OPPFYLT)
            assertThat(sisteUnderveisperiode.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)

            val rettighetstypeTidslinje =
                VilkårsresultatRepositoryImpl(connection).hent(førstegangsbehandling.id).rettighetstypeTidslinje()

            // Rettighetstidslinjen begrenses av aldersvilkåret
            val aldersvilkåret = VilkårsresultatRepositoryImpl(connection).hent(førstegangsbehandling.id)
                .finnVilkår(Vilkårtype.ALDERSVILKÅRET)

            assertThat(rettighetstypeTidslinje.perioder().maxOfOrNull { it.tom }).isEqualTo(
                aldersvilkåret.tidslinje().segmenter().filter { it.verdi.utfall == Utfall.OPPFYLT }
                    .maxOfOrNull { it.periode.tom })

            sisteUnderveisperiode.periode.tom
        }

        kjørUtvidVedtakslengdeJobb()

        dataSource.transaction { connection ->
            val automatiskBehandling = BehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).finnBehandlingMedSisteFattedeVedtak(sak.id)!!

            val vedtakslengdeVurdering =
                VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(automatiskBehandling.id)
            assertThat(vedtakslengdeVurdering).isNotNull

            val underveisGrunnlag = UnderveisRepositoryImpl(connection).hentHvisEksisterer(automatiskBehandling.id)
            val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!
            assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(
                sluttdatoFørstegangsbehandling.plussEtÅrMedHverdager(
                    ÅrMedHverdager.ANDRE_ÅR
                )
            )
            assertThat(sisteUnderveisperiode.utfall).isEqualTo(Utfall.OPPFYLT)
            assertThat(sisteUnderveisperiode.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)

            val rettighetstypeTidslinje =
                VilkårsresultatRepositoryImpl(connection).hent(automatiskBehandling.id).rettighetstypeTidslinje()

            // Rettighetstidslinjen begrenses av aldersvilkåret
            val aldersvilkåret = VilkårsresultatRepositoryImpl(connection).hent(automatiskBehandling.id)
                .finnVilkår(Vilkårtype.ALDERSVILKÅRET)

            assertThat(rettighetstypeTidslinje.perioder().maxOfOrNull { it.tom }).isEqualTo(
                aldersvilkåret.tidslinje().segmenter().filter { it.verdi.utfall == Utfall.OPPFYLT }
                    .maxOfOrNull { it.periode.tom })
        }
    }

    @Test
    fun `forlenger ikke vedtak med avslag`() {
        val søknadstidspunkt = LocalDateTime.now(clock).minusYears(1)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(
            søknad = SØKNAD_INGEN_MEDLEMSKAP,
            mottattTidspunkt = søknadstidspunkt
        )
        val rettighetsperiode = sak.rettighetsperiode

        førstegangsbehandling
            .løsLovvalg(rettighetsperiode.fom, medlem = false)
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_AVSLAG)


        dataSource.transaction { connection ->
            val førstegangsbehandling = BehandlingRepositoryImpl(connection).finnFørstegangsbehandling(sak.id)

            val underveisGrunnlag = UnderveisRepositoryImpl(connection).hentHvisEksisterer(førstegangsbehandling.id)
            val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!
            assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(
                rettighetsperiode.fom.plussEtÅrMedHverdager(
                    ÅrMedHverdager.FØRSTE_ÅR
                )
            )
            assertThat(sisteUnderveisperiode.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
            assertThat(sisteUnderveisperiode.rettighetsType).isEqualTo(null)
        }

        kjørUtvidVedtakslengdeJobb()

        dataSource.transaction { connection ->
            val behandlingMedSisteFattedeVedtak = BehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).finnBehandlingMedSisteFattedeVedtak(sak.id)!!

            assertThat(behandlingMedSisteFattedeVedtak.id).isEqualTo(førstegangsbehandling.id)
        }
    }

    @Test
    fun `forlenger ikke vedtak med passert slutt hvor rettighetstype bistand ikke er oppfylt hele neste forlengelsesperiode`() {
        val søknadstidspunkt = LocalDateTime.now(clock).minusYears(1)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(
            søknad = SØKNAD_INGEN_MEDLEMSKAP,
            mottattTidspunkt = søknadstidspunkt
        )
        val rettighetsperiode = sak.rettighetsperiode
        val startDato = sak.rettighetsperiode.fom
        val datoMedlemskapIkkeOppfyltFra = sak.rettighetsperiode.fom.plusYears(1).plusMonths(6)
        val datoMedlemskapIkkeOppfyltTil = sak.rettighetsperiode.fom.plusYears(1).plusMonths(7).minusDays(1)

        førstegangsbehandling
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = sak.rettighetsperiode.fom,
                            tom = datoMedlemskapIkkeOppfyltFra.minusDays(1),
                            begrunnelse = "lovvalg ok",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", true),
                        ),
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = datoMedlemskapIkkeOppfyltFra,
                            tom = datoMedlemskapIkkeOppfyltTil,
                            begrunnelse = "ikke lenger oppfylt",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", false),
                        ),
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = datoMedlemskapIkkeOppfyltTil.plusDays(1),
                            tom = rettighetsperiode.tom,
                            begrunnelse = "lovvalg ok",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", false),
                        )
                    )
                )
            )
            .løsSykdom(startDato)
            .løsBistand(startDato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsForutgåendeMedlemskap(startDato, medlem = true)
            .løsOppholdskrav(startDato)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)


        val sluttdatoFørstegangsbehandling = dataSource.transaction { connection ->
            val førstegangsbehandling = BehandlingRepositoryImpl(connection).finnFørstegangsbehandling(sak.id)

            val underveisGrunnlag = UnderveisRepositoryImpl(connection).hentHvisEksisterer(førstegangsbehandling.id)
            val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!
            assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(
                rettighetsperiode.fom.plussEtÅrMedHverdager(
                    ÅrMedHverdager.FØRSTE_ÅR
                )
            )
            assertThat(sisteUnderveisperiode.utfall).isEqualTo(Utfall.OPPFYLT)
            assertThat(sisteUnderveisperiode.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)

            val rettighetstypeTidslinje =
                VilkårsresultatRepositoryImpl(connection).hent(førstegangsbehandling.id).rettighetstypeTidslinje()

            // Rettighetstidslinjen begrenses av lovvalgsvilkåret
            val lovvalgsvilkåret = VilkårsresultatRepositoryImpl(connection).hent(førstegangsbehandling.id)
                .finnVilkår(Vilkårtype.LOVVALG)

            assertThat(rettighetstypeTidslinje.perioder().maxOfOrNull { it.tom }).isEqualTo(
                lovvalgsvilkåret.tidslinje().segmenter().filter { it.verdi.utfall == Utfall.OPPFYLT }
                    .maxOfOrNull { it.periode.tom })

            sisteUnderveisperiode.periode.tom
        }

        kjørUtvidVedtakslengdeJobb(
            vedtakslengdeGatewayProvider = testGatewayProvider(AlleAvskruddUnleash::class),
        )

        dataSource.transaction { connection ->
            val behandlingMedSisteFattedeVedtak = BehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).finnBehandlingMedSisteFattedeVedtak(sak.id)!!

            val vedtakslengdeVurdering =
                VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(behandlingMedSisteFattedeVedtak.id)
            assertThat(vedtakslengdeVurdering).isNotNull()

            val underveisGrunnlag =
                UnderveisRepositoryImpl(connection).hentHvisEksisterer(behandlingMedSisteFattedeVedtak.id)
            val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!
            assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(sluttdatoFørstegangsbehandling)
        }
    }

    @Test
    fun `forlenger vedtak hvor bistandsbehov kun er oppfylt for en kort periode i andre året`() {
        val søknadstidspunkt = LocalDateTime.now(clock).minusYears(1)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt)
        val rettighetsperiode = sak.rettighetsperiode
        val startDato = sak.rettighetsperiode.fom
        val datoOppholdskravIkkeOppfyltFra = sak.rettighetsperiode.fom.plusYears(1).plusMonths(6)

        førstegangsbehandling
            .løsSykdom(startDato)
            .løsBistand(startDato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsAvklaringsBehov(
                AvklarOppholdskravLøsning(
                    løsningerForPerioder = listOf(
                        AvklarOppholdkravLøsningForPeriodeDto(
                            oppfylt = true,
                            land = "",
                            fom = startDato,
                            tom = datoOppholdskravIkkeOppfyltFra.minusDays(1),
                            begrunnelse = "Fiske"
                        ),
                        AvklarOppholdkravLøsningForPeriodeDto(
                            oppfylt = false,
                            land = "Sverige",
                            fom = datoOppholdskravIkkeOppfyltFra,
                            begrunnelse = "Fiske"
                        )
                    )
                )
            )
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)


        dataSource.transaction { connection ->
            val førstegangsbehandling = BehandlingRepositoryImpl(connection).finnFørstegangsbehandling(sak.id)

            val underveisGrunnlag = UnderveisRepositoryImpl(connection).hentHvisEksisterer(førstegangsbehandling.id)
            val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!
            assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(
                rettighetsperiode.fom.plussEtÅrMedHverdager(
                    ÅrMedHverdager.FØRSTE_ÅR
                )
            )
            assertThat(sisteUnderveisperiode.utfall).isEqualTo(Utfall.OPPFYLT)
            assertThat(sisteUnderveisperiode.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)
        }

        kjørUtvidVedtakslengdeJobb()

        dataSource.transaction { connection ->
            val behandlingMedSisteFattedeVedtak = BehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).finnBehandlingMedSisteFattedeVedtak(sak.id)!!

            val vedtakslengdeVurdering =
                VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(behandlingMedSisteFattedeVedtak.id)
            assertThat(vedtakslengdeVurdering).isNotNull()
            assertThat(vedtakslengdeVurdering?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                datoOppholdskravIkkeOppfyltFra.minusDays(
                    1
                )
            )

            val underveisGrunnlag =
                UnderveisRepositoryImpl(connection).hentHvisEksisterer(behandlingMedSisteFattedeVedtak.id)
            val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!
            assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(datoOppholdskravIkkeOppfyltFra.minusDays(1))
        }
    }

    @Test
    fun `forlenger ikke vedtak hvor bistandsbehov ikke er oppfylt i to korte periode i andre året - krever manuell behandling`() {
        val søknadstidspunkt = LocalDateTime.now(clock).minusYears(1)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt)
        val startDato = sak.rettighetsperiode.fom
        val forventetSluttdato = startDato.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)
        val datoOppholdskravIkkeOppfyltFra = sak.rettighetsperiode.fom.plusYears(1).plusMonths(2)
        val nesteDatoOppholdskravIkkeOppfyltFra = datoOppholdskravIkkeOppfyltFra.plusMonths(4)

        førstegangsbehandling
            .løsSykdom(startDato)
            .løsBistand(startDato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            // Legger inn flere hull etter ett år - dette skal føre til manuell behandling ved utvidelse av vedtakslengde
            .løsAvklaringsBehov(
                AvklarOppholdskravLøsning(
                    løsningerForPerioder = listOf(
                        AvklarOppholdkravLøsningForPeriodeDto(
                            oppfylt = true,
                            land = null,
                            fom = startDato,
                            tom = datoOppholdskravIkkeOppfyltFra.minusDays(1),
                            begrunnelse = "Oppholder seg i Norge"
                        ),
                        AvklarOppholdkravLøsningForPeriodeDto(
                            oppfylt = false,
                            land = "Sverige",
                            fom = datoOppholdskravIkkeOppfyltFra,
                            tom = datoOppholdskravIkkeOppfyltFra.plusMonths(1).minusDays(1),
                            begrunnelse = "Fiske"
                        ),
                        AvklarOppholdkravLøsningForPeriodeDto(
                            oppfylt = true,
                            land = null,
                            fom = datoOppholdskravIkkeOppfyltFra.plusMonths(1),
                            tom = nesteDatoOppholdskravIkkeOppfyltFra.minusDays(1),
                            begrunnelse = "Oppholder seg i Norge"
                        ),
                        AvklarOppholdkravLøsningForPeriodeDto(
                            oppfylt = false,
                            land = "Sverige",
                            fom = nesteDatoOppholdskravIkkeOppfyltFra,
                            begrunnelse = "Fiske"
                        )
                    )
                )
            )
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)


        dataSource.transaction { connection ->
            val førstegangsbehandling = BehandlingRepositoryImpl(connection).finnFørstegangsbehandling(sak.id)

            val vedtakslengdeVurdering =
                VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(førstegangsbehandling.id)
            assertThat(vedtakslengdeVurdering).isNotNull()
            assertThat(vedtakslengdeVurdering?.gjeldendeVurdering()?.sluttdato).isEqualTo(forventetSluttdato)
        }

        kjørUtvidVedtakslengdeJobb()

        dataSource.transaction { connection ->
            val behandlingMedSisteFattedeVedtak = BehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).finnBehandlingMedSisteFattedeVedtak(sak.id)!!

            val vedtakslengdeVurdering =
                VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(behandlingMedSisteFattedeVedtak.id)
            assertThat(vedtakslengdeVurdering).isNotNull()
            assertThat(vedtakslengdeVurdering?.gjeldendeVurdering()?.sluttdato).isEqualTo(forventetSluttdato)
        }
    }

    @Test
    fun `forleng også vedtak i åpen behandling dersom vedtaksutvidelse-jobb kjører`() {
        val søknadstidspunkt = LocalDateTime.now(clock).minusYears(1)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt)
        val rettighetsperiode = sak.rettighetsperiode
        val startDato = sak.rettighetsperiode.fom

        førstegangsbehandling
            .løsSykdom(startDato)
            .løsBistand(startDato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsOppholdskrav(startDato)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)


        val åpenBehandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom, vissVarighet = true)
            .løsBistand(sak.rettighetsperiode.fom)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val underveisRepository: UnderveisRepository = repositoryProvider.provide()
                val vedtakslengdeVurdering =
                    vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)
                val underveisGrunnlag = underveisRepository.hentHvisEksisterer(this.behandling.id)
                val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!

                assertThat(vedtakslengdeVurdering?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                    rettighetsperiode.fom.plussEtÅrMedHverdager(
                        ÅrMedHverdager.FØRSTE_ÅR
                    )
                )
                assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(
                    rettighetsperiode.fom.plussEtÅrMedHverdager(
                        ÅrMedHverdager.FØRSTE_ÅR
                    )
                )
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FATTE_VEDTAK)
            }

        val sluttdatoFørstegangsbehandling = dataSource.transaction { connection ->
            val førstegangsbehandling = BehandlingRepositoryImpl(connection).finnFørstegangsbehandling(sak.id)
            val underveisGrunnlag = UnderveisRepositoryImpl(connection).hentHvisEksisterer(førstegangsbehandling.id)
            val vedtakslengdeVurdering =
                VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(førstegangsbehandling.id)

            val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!

            assertThat(vedtakslengdeVurdering?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                rettighetsperiode.fom.plussEtÅrMedHverdager(
                    ÅrMedHverdager.FØRSTE_ÅR
                )
            )
            assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(
                rettighetsperiode.fom.plussEtÅrMedHverdager(
                    ÅrMedHverdager.FØRSTE_ÅR
                )
            )

            sisteUnderveisperiode.periode.tom
        }

        kjørUtvidVedtakslengdeJobb()

        dataSource.transaction { connection ->
            val automatiskBehandling = BehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).finnBehandlingMedSisteFattedeVedtak(sak.id)!!

            val nySluttdato = sluttdatoFørstegangsbehandling.plussEtÅrMedHverdager(ÅrMedHverdager.ANDRE_ÅR)

            verifiserUtvidetSluttdato(connection, automatiskBehandling.id, nySluttdato)
            verifiserUtvidetSluttdato(connection, åpenBehandling.id, nySluttdato)
        }

    }

    private fun verifiserUtvidetSluttdato(
        connection: DBConnection,
        behandlingId: BehandlingId,
        nySluttdato: LocalDate
    ) {
        val vedtakslengdeVurdering = VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        assertThat(vedtakslengdeVurdering).isNotNull

        val underveisGrunnlag = UnderveisRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        val sisteUnderveisperiode = underveisGrunnlag?.perioder?.maxBy { it.periode.fom }!!

        assertThat(sisteUnderveisperiode.periode.tom).isEqualTo(nySluttdato)
        assertThat(sisteUnderveisperiode.utfall).isEqualTo(Utfall.OPPFYLT)
        assertThat(sisteUnderveisperiode.rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)

        val rettighetstypeTidslinje =
            VilkårsresultatRepositoryImpl(connection).hent(behandlingId).rettighetstypeTidslinje()

        // Rettighetstidslinjen begrenses av aldersvilkåret
        val aldersvilkåret = VilkårsresultatRepositoryImpl(connection).hent(behandlingId)
            .finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(rettighetstypeTidslinje.perioder().maxOfOrNull { it.tom }).isEqualTo(
            aldersvilkåret.tidslinje().segmenter().filter { it.verdi.utfall == Utfall.OPPFYLT }
                .maxOfOrNull { it.periode.tom })
    }

    @Test
    fun `forleng vedtak med passert slutt og gjør så en revurdering hvor sluttdato utvides pga overgang arbeid`() {
        val søknadstidspunkt = LocalDateTime.now(clock).minusYears(1)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt)
        val startDato = sak.rettighetsperiode.fom

        førstegangsbehandling
            .løsSykdom(startDato)
            .løsBistand(startDato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsOppholdskrav(startDato)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)


        val sluttdatoFørstegangsbehandling = dataSource.transaction { connection ->
            val førstegangsbehandling = BehandlingRepositoryImpl(connection).finnFørstegangsbehandling(sak.id)
            val vedtakslengdeVurdering =
                VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(førstegangsbehandling.id)
            vedtakslengdeVurdering!!.gjeldendeVurdering()!!.sluttdato
        }

        kjørUtvidVedtakslengdeJobb()

        dataSource.transaction { connection ->
            val automatiskBehandling = BehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).finnBehandlingMedSisteFattedeVedtak(sak.id)!!

            val vedtakslengdeVurdering =
                VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(automatiskBehandling.id)
            assertThat(vedtakslengdeVurdering?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                sluttdatoFørstegangsbehandling.plussEtÅrMedHverdager(
                    ÅrMedHverdager.ANDRE_ÅR
                )
            )
            assertThat(vedtakslengdeVurdering?.gjeldendeVurdering()?.utvidetMed).isEqualTo(ÅrMedHverdager.ANDRE_ÅR)
        }

        // Gjør endring som kommer to måneder etter forlengelsen har gått ut - dette skal gi ny sluttdato pga rettighetstype
        val endringsdato = LocalDate.now(clock).plusYears(1).plusMonths(2)

        sak.opprettManuellRevurdering(
            Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
        )
            .løsSykdom(vurderingGjelderFra = endringsdato, erOppfylt = false)
            .løsBistand(endringsdato, erOppfylt = false)
            .løsOvergangArbeid(Utfall.OPPFYLT, fom = endringsdato)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                    endringsdato.plusMonths(6).minusDays(1)
                )
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.utvidetMed).isEqualTo(ÅrMedHverdager.ANDRE_ÅR)
            }
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_11_17)

        // Gjør om fra arbeidssøker tilbake til bistandsbehov - skal behoholde sluttdatoen
        sak.opprettManuellRevurdering(
            Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND
        )
            .løsSykdom(vurderingGjelderFra = endringsdato, erOppfylt = true)
            .løsBistand(endringsdato, erOppfylt = true)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                    endringsdato.plusMonths(6).minusDays(1)
                )
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.utvidetMed).isEqualTo(ÅrMedHverdager.ANDRE_ÅR)
            }
    }

    private fun kjørUtvidVedtakslengdeJobb(
        vedtakslengdeGatewayProvider: GatewayProvider = gatewayProvider,
    ) {
        dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)

            val opprettJobbUtvidVedtakslengdeJobbUtfører = OpprettJobbUtvidVedtakslengdeJobbUtfører(
                behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
                vedtakslengdeService = VedtakslengdeService(repositoryProvider, vedtakslengdeGatewayProvider),
                flytJobbRepository = FlytJobbRepositoryImpl(connection),
                unleashGateway = AlleAvskruddUnleash,
                clock = clock,
            )

            opprettJobbUtvidVedtakslengdeJobbUtfører.utfør(JobbInput(OpprettJobbUtvidVedtakslengdeJobbUtfører))
        }

        motor.kjørJobber()
    }
}

class AvklarVedtakslengdeFlytTest : AbstraktFlytOrkestratorTest(AvklarVedtakslengdeFlytUnleash::class) {

    private val clock = fixedClock(1 desember 2025)

    @Test
    fun `skal trigge avklaringsbehov for vedtakslengde når vurderingsbehov VEDTAKSLENGDE_MANUELT er lagt til i førstegangsbehandling`() {
        val søknadstidspunkt = LocalDateTime.now(clock)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt)
        val startDato = sak.rettighetsperiode.fom

        sak.leggTilVurderingsbehov(førstegangsbehandling.referanse, listOf(Vurderingsbehov.VEDTAKSLENGDE_MANUELT))

        val manueltOverstyrtSluttdato = startDato.plusMonths(15)
        val manueltOverstyrtBegrunnelse = "Vurdert vedtakslengde manuelt"

        førstegangsbehandling
            .løsSykdom(startDato, erOppfylt = true)
            .løsBistand(startDato, erOppfylt = true)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsOppholdskrav(startDato)
            .løsAndreStatligeYtelser()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)

                assertThat(vedtakslengdeGrunnlag).isNotNull
                assertThat(vedtakslengdeGrunnlag?.vurderinger?.size).isEqualTo(1)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(
                    startDato.plussEtÅrMedHverdager(
                        ÅrMedHverdager.FØRSTE_ÅR
                    )
                )
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.begrunnelse).isEqualTo("Automatisk vurdert")
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.vurdertAutomatisk).isTrue
            }
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .contains(Definisjon.AVKLAR_VEDTAKSLENGDE)
            }
            .løsAvklaringsBehov(
                AvklarVedtakslengdeLøsning(
                    løsningerForPerioder = listOf(
                        VedtakslengdeVurderingDto(
                            fom = startDato,
                            tom = manueltOverstyrtSluttdato,
                            sluttdato = manueltOverstyrtSluttdato,
                            begrunnelse = "Vurdert vedtakslengde manuelt"
                        )
                    )
                )
            )
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)

                assertThat(vedtakslengdeGrunnlag).isNotNull
                assertThat(vedtakslengdeGrunnlag?.vurderinger?.size).isEqualTo(2)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(manueltOverstyrtSluttdato)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.begrunnelse).isEqualTo(
                    manueltOverstyrtBegrunnelse
                )
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.vurdertManuelt).isTrue
            }
    }

    @Test
    fun `skal trigge avklaringsbehov for vedtakslengde når vurderingsbehov VEDTAKSLENGDE_MANUELT er lagt til i revurdering`() {
        val sak = happyCaseFørstegangsbehandling(LocalDate.now(clock))
        val automatiskSluttdato = sak.rettighetsperiode.fom.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)

        dataSource.transaction { connection ->
            val behandling = BehandlingRepositoryImpl(connection).finnFørstegangsbehandling(sak.id)
            val vedtakslengdeGrunnlag = VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(behandling.id)
            assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(automatiskSluttdato)
            assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.vurdertAutomatisk).isTrue
        }

        val nyManuellSluttdato = automatiskSluttdato.plussEtÅrMedHverdager(ÅrMedHverdager.ANDRE_ÅR)
        val nyBegrunnelse = "Utvidet vedtakslengde manuelt"

        sak.opprettManuellRevurdering(
            Vurderingsbehov.VEDTAKSLENGDE_MANUELT
        )
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .contains(Definisjon.AVKLAR_VEDTAKSLENGDE)
            }
            .løsAvklaringsBehov(
                AvklarVedtakslengdeLøsning(
                    løsningerForPerioder = listOf(
                        VedtakslengdeVurderingDto(
                            fom = automatiskSluttdato.plusDays(1),
                            tom = nyManuellSluttdato,
                            sluttdato = nyManuellSluttdato,
                            begrunnelse = nyBegrunnelse
                        )
                    )
                )
            )
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)
                assertThat(vedtakslengdeGrunnlag?.vurderinger?.size).isEqualTo(2)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(nyManuellSluttdato)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.begrunnelse).isEqualTo(nyBegrunnelse)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.vurdertManuelt).isTrue
            }
    }

    @Test
    fun `skal kunne overstyre automatisk vurdering for vedtakslengde med manuell vurdering`() {
        val søknadstidspunkt = LocalDateTime.now(clock)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt)
        val startDato = sak.rettighetsperiode.fom
        val forventetSluttdato = startDato.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)

        val manueltOverstyrtSluttdato = startDato.plusMonths(15)
        val manueltOverstyrtBegrunnelse = "Vurdert vedtakslengde manuelt"

        førstegangsbehandling
            .løsSykdom(startDato, erOppfylt = true)
            .løsBistand(startDato, erOppfylt = true)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            .løsOppholdskrav(startDato)
            .løsAndreStatligeYtelser()
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)

                // Ett år som forventet med ordinær rettighet
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(forventetSluttdato)
            }
            // Overstyrer likevel manuelt til 15 måneder
            .løsAvklaringsBehov(
                AvklarVedtakslengdeLøsning(
                    løsningerForPerioder = listOf(
                        VedtakslengdeVurderingDto(
                            fom = startDato,
                            tom = manueltOverstyrtSluttdato,
                            sluttdato = manueltOverstyrtSluttdato,
                            begrunnelse = "Vurdert vedtakslengde manuelt"
                        )
                    )
                )
            )
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)

                assertThat(vedtakslengdeGrunnlag).isNotNull
                assertThat(vedtakslengdeGrunnlag?.vurderinger?.size).isEqualTo(2)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(manueltOverstyrtSluttdato)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.begrunnelse).isEqualTo(
                    manueltOverstyrtBegrunnelse
                )
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.vurdertManuelt).isTrue
            }
    }

    @Test
    fun `jobb oppretter manuell behandling når neste periode inneholder to korte perioder med bistand`() {
        val søknadstidspunkt = LocalDateTime.now(clock).minusYears(1)
        val (sak, førstegangsbehandling) = sendInnFørsteSøknad(mottattTidspunkt = søknadstidspunkt)
        val startDato = sak.rettighetsperiode.fom
        val forventetSluttdato = startDato.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)
        val datoOppholdskravIkkeOppfyltFra = sak.rettighetsperiode.fom.plusYears(1).plusMonths(2)
        val nesteDatoOppholdskravIkkeOppfyltFra = datoOppholdskravIkkeOppfyltFra.plusMonths(4)

        førstegangsbehandling
            .løsSykdom(startDato)
            .løsBistand(startDato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikre()
            .løsBeregningstidspunkt(startDato)
            // Legger inn flere hull etter ett år - dette skal føre til manuell behandling ved utvidelse av vedtakslengde
            .løsAvklaringsBehov(
                AvklarOppholdskravLøsning(
                    løsningerForPerioder = listOf(
                        AvklarOppholdkravLøsningForPeriodeDto(
                            oppfylt = true,
                            land = null,
                            fom = startDato,
                            tom = datoOppholdskravIkkeOppfyltFra.minusDays(1),
                            begrunnelse = "Oppholder seg i Norge"
                        ),
                        AvklarOppholdkravLøsningForPeriodeDto(
                            oppfylt = false,
                            land = "Sverige",
                            fom = datoOppholdskravIkkeOppfyltFra,
                            tom = datoOppholdskravIkkeOppfyltFra.plusMonths(1).minusDays(1),
                            begrunnelse = "Fiske"
                        ),
                        AvklarOppholdkravLøsningForPeriodeDto(
                            oppfylt = true,
                            land = null,
                            fom = datoOppholdskravIkkeOppfyltFra.plusMonths(1),
                            tom = nesteDatoOppholdskravIkkeOppfyltFra.minusDays(1),
                            begrunnelse = "Oppholder seg i Norge"
                        ),
                        AvklarOppholdkravLøsningForPeriodeDto(
                            oppfylt = false,
                            land = "Sverige",
                            fom = nesteDatoOppholdskravIkkeOppfyltFra,
                            begrunnelse = "Fiske"
                        )
                    )
                )
            )
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)


        dataSource.transaction { connection ->
            val førstegangsbehandling = BehandlingRepositoryImpl(connection).finnFørstegangsbehandling(sak.id)

            val vedtakslengdeVurdering =
                VedtakslengdeRepositoryImpl(connection).hentHvisEksisterer(førstegangsbehandling.id)
            assertThat(vedtakslengdeVurdering).isNotNull()
            assertThat(vedtakslengdeVurdering?.gjeldendeVurdering()?.sluttdato).isEqualTo(forventetSluttdato)
        }

        kjørUtvidVedtakslengdeJobb()

        val manuellBehandlingMedVurderingsbehovVedtakslengdeManuelt = dataSource.transaction { connection ->
            val behandling = BehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).finnSisteYtelsesbehandlingFor(sak.id)!!

            assertThat(behandling.vurderingsbehov().map { it.type })
                .containsExactly(no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.VEDTAKSLENGDE_MANUELT)
            behandling
        }

        val forlengelseFom = nesteDatoOppholdskravIkkeOppfyltFra.plusDays(1)

        manuellBehandlingMedVurderingsbehovVedtakslengdeManuelt
            .løsAvklaringsBehov(
                AvklarVedtakslengdeLøsning(
                    løsningerForPerioder = listOf(
                        VedtakslengdeVurderingDto(
                            fom = forventetSluttdato.plusDays(1),
                            tom = forlengelseFom,
                            sluttdato = forlengelseFom,
                            begrunnelse = "Vurdert vedtakslengde manuelt"
                        )
                    )
                )
            )
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_ENDRING)
            .medKontekst {
                val vedtakslengdeRepository: VedtakslengdeRepository = repositoryProvider.provide()
                val vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(this.behandling.id)

                assertThat(vedtakslengdeGrunnlag).isNotNull
                assertThat(vedtakslengdeGrunnlag?.vurderinger?.size).isEqualTo(2)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.sluttdato).isEqualTo(forlengelseFom)
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.begrunnelse).isEqualTo(
                    "Vurdert vedtakslengde manuelt"
                )
                assertThat(vedtakslengdeGrunnlag?.gjeldendeVurdering()?.vurdertManuelt).isTrue
            }
    }

    private fun kjørUtvidVedtakslengdeJobb() {
        dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)

            val opprettJobbUtvidVedtakslengdeJobbUtfører = OpprettJobbUtvidVedtakslengdeJobbUtfører(
                behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
                vedtakslengdeService = VedtakslengdeService(repositoryProvider, gatewayProvider),
                flytJobbRepository = FlytJobbRepositoryImpl(connection),
                unleashGateway = AvklarVedtakslengdeFlytUnleash,
                clock = clock,
            )

            opprettJobbUtvidVedtakslengdeJobbUtfører.utfør(JobbInput(OpprettJobbUtvidVedtakslengdeJobbUtfører))
        }

        motor.kjørJobber()
    }
}
