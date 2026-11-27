package no.nav.aap.behandlingsflyt.avløp

import io.mockk.spyk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedagerGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgRevurdering
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.GjenståendeSykedagerDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.GjenståendeSykedagerGrunnlagDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.NyÅrsakTilBehandlingV0
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.pip.PipService
import no.nav.aap.behandlingsflyt.prosessering.MeldeperiodeTilMeldekortBackendJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.VarsleOppgaveOmHendelseJobbUtFører
import no.nav.aap.behandlingsflyt.prosessering.datadeling.DatadelingMeldePerioderOgSakStatusJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.datadeling.DatadelingMeldekortJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.statistikk.StatistikkJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFlytJobbRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryGjenståendeSykedagerRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.meldeplikt.MeldepliktStatus
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.random.Random

class BehandlingHendelseServiceTest {
    private val flytJobbRepository = spyk(InMemoryFlytJobbRepository)

    @Test
    fun `verifiser at FlytJobbRepository blir kalt med riktige argumenter`() {
        // SETUP

        val (behandlingHendelseService, behandling) = settOppFlyt()

        val avklaringsbehovene = avklaringsbehoveneMedÉttÅpent()

        // ACT

        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)

        // VERIFY
        val calls = mutableListOf<JobbInput>()
        verify {
            flytJobbRepository.leggTil(capture(calls))
        }

        assertThat(calls.size).isEqualTo(5)
        val jobbTyperOpprettet = calls.map { it.type() }
        assertThat(jobbTyperOpprettet).contains(VarsleOppgaveOmHendelseJobbUtFører.type)
        assertThat(jobbTyperOpprettet).contains(StatistikkJobbUtfører.type)
        assertThat(jobbTyperOpprettet).contains(DatadelingMeldePerioderOgSakStatusJobbUtfører.type)
        assertThat(jobbTyperOpprettet).contains(DatadelingMeldekortJobbUtfører.type)
        assertThat(jobbTyperOpprettet).contains(MeldeperiodeTilMeldekortBackendJobbUtfører.type)

        val hendelse = DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(calls.first().payload())
        assertThat(hendelse.referanse.referanse).isEqualTo(behandling.referanse.referanse)
    }

    @ParameterizedTest
    @EnumSource(
        GjenståendeSykedagerGrunnlag::class,
        names = ["VEDTAK_I_NY_LØSNING", "SISTE_UTBETALING_I_INFOTRYGD", "MAKSDATO_I_INFOTRYGD"]
    )
    fun `sender gjenstående sykedager og maksdato til oppgave for førstegangsbehandling`(
        grunnlag: GjenståendeSykedagerGrunnlag
    ) {
        val (behandlingHendelseService, behandling) = settOppFlyt()
        InMemoryGjenståendeSykedagerRepository.lagre(
            behandling.id,
            GjenståendeSykedager(grunnlag, 42, 1 januar 2026)
        )

        behandlingHendelseService.stoppet(behandling, avklaringsbehoveneMedÉttÅpent())

        assertThat(oppgaveHendelse().gjenståendeSykedager).isEqualTo(
            GjenståendeSykedagerDto(
                GjenståendeSykedagerGrunnlagDto.valueOf(grunnlag.name), 42, 1 januar 2026, 2 mars 2026
            )
        )
    }

    @Test
    fun `sender ikke gjenstående sykedager til oppgave for revurdering`() {
        val (sak, behandling, revurdering) = opprettInMemorySakOgRevurdering()
        lagreInnvilgelsePåForrigeBehandling(behandling, sak)

        InMemoryGjenståendeSykedagerRepository.lagre(
            revurdering.id,
            GjenståendeSykedager(GjenståendeSykedagerGrunnlag.VEDTAK_I_NY_LØSNING, 42, 1 januar 2026)
        )

        behandlingHendelseService().stoppet(revurdering, avklaringsbehoveneMedÉttÅpent())

        assertThat(oppgaveHendelse().gjenståendeSykedager).isNull()
    }

    @Test
    fun `sender null til oppgave når førstegangsbehandling mangler gjenstående sykedager`() {
        val (behandlingHendelseService, behandling) = settOppFlyt()

        behandlingHendelseService.stoppet(behandling, avklaringsbehoveneMedÉttÅpent())

        assertThat(oppgaveHendelse().gjenståendeSykedager).isNull()
    }

    @Test
    fun `skal ikke opprette oppgaveHendelse dersom avklaringsbehovene er tomme`() {
        // SETUP

        val (behandlingHendelseService, behandling) = settOppFlyt()

        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, nyBehandlingId())

        // ACT

        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)

        // VERIFY

        val calls = mutableListOf<JobbInput>()
        verify {
            flytJobbRepository.leggTil(capture(calls))
        }

        assertThat(calls.size).isEqualTo(4)
        val jobbTyperOpprettet = calls.map { it.type() }
        assertThat(jobbTyperOpprettet).doesNotContain(VarsleOppgaveOmHendelseJobbUtFører.type)
        assertThat(jobbTyperOpprettet).contains(StatistikkJobbUtfører.type)
        assertThat(jobbTyperOpprettet).contains(DatadelingMeldePerioderOgSakStatusJobbUtfører.type)
        assertThat(jobbTyperOpprettet).contains(DatadelingMeldekortJobbUtfører.type)
        assertThat(jobbTyperOpprettet).contains(MeldeperiodeTilMeldekortBackendJobbUtfører.type)
    }

    @Test
    fun `Hvis vurderingsbehov opprettet flere ganger på samme sak, utled reserverTil basert på senest mottatte dokument`() {
        val (behandlingHendelseService, behandling) = settOppFlyt()

        val avklaringsbehovene = avklaringsbehoveneMedÉttÅpent()

        // To trekk søknad-"dokumenter"
        InMemoryMottattDokumentRepository.lagre(
            MottattDokument(
                referanse = InnsendingReferanse(
                    type = InnsendingReferanse.Type.MANUELL_OPPRETTELSE,
                    verdi = "123"
                ),
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                mottattTidspunkt = LocalDateTime.now().minusDays(1),
                opprettetTid = LocalDateTime.now().minusDays(1),
                type = InnsendingType.NY_ÅRSAK_TIL_BEHANDLING,
                kanal = Kanal.DIGITAL,
                status = Status.MOTTATT,
                strukturertDokument = StrukturertDokument(
                    data = NyÅrsakTilBehandlingV0(
                        årsakerTilBehandling = listOf(Vurderingsbehov.SØKNAD_TRUKKET),
                        behandlingReferanse = behandling.referanse.toString(),
                        reserverTilBruker = "Veileder",
                        beskrivelse = "Søknad skal kanskje trekkes"
                    )
                )
            )
        )
        InMemoryMottattDokumentRepository.lagre(
            MottattDokument(
                referanse = InnsendingReferanse(
                    type = InnsendingReferanse.Type.MANUELL_OPPRETTELSE,
                    verdi = "123"
                ),
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                mottattTidspunkt = LocalDateTime.now().minusHours(2),
                opprettetTid = LocalDateTime.now().minusHours(2),
                type = InnsendingType.NY_ÅRSAK_TIL_BEHANDLING,
                kanal = Kanal.DIGITAL,
                status = Status.MOTTATT,
                strukturertDokument = StrukturertDokument(
                    data = NyÅrsakTilBehandlingV0(
                        årsakerTilBehandling = listOf(Vurderingsbehov.SØKNAD_TRUKKET),
                        behandlingReferanse = behandling.referanse.toString(),
                        reserverTilBruker = "Veileder2",
                        beskrivelse = "Søknad skal kanskje trekkes"
                    )
                )
            )
        )

        behandlingHendelseService.stoppet(behandling, avklaringsbehovene)

        val calls = mutableListOf<JobbInput>()
        verify {
            flytJobbRepository.leggTil(capture(calls))
        }

        // reserverTil skal settes til veileder som opprettet avklaringsbehovet sist
        val hendelse = DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(calls.first().payload())
        assertThat(hendelse.referanse.referanse).isEqualTo(behandling.referanse.referanse)
        assertThat(hendelse.reserverTil).isEqualTo("Veileder2")
        assertThat(hendelse.reserverTilPerAvklaringsbehov).isEqualTo(
            mutableMapOf(
                Pair(
                    Definisjon.VURDER_TREKK_AV_SØKNAD.kode.name,
                    "Veileder2"
                )
            )
        )

    }


    private fun avklaringsbehoveneMedÉttÅpent(): Avklaringsbehovene {
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, nyBehandlingId())
        avklaringsbehovene.leggTil(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP, StegType.VURDER_LOVVALG, null, null, null)
        return avklaringsbehovene
    }

    private fun nyBehandlingId() = BehandlingId(Random.nextLong(1_000_000, Long.MAX_VALUE))

    private fun oppgaveHendelse(): BehandlingFlytStoppetHendelse {
        val calls = mutableListOf<JobbInput>()
        verify { flytJobbRepository.leggTil(capture(calls)) }
        val jobb = calls.first { it.type() == VarsleOppgaveOmHendelseJobbUtFører.type }
        return DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(jobb.payload())
    }

    private fun behandlingHendelseService(): BehandlingHendelseServiceImpl {
        val gatewayProvider = minimalGatewayProvider()
        return BehandlingHendelseServiceImpl(
            flytJobbRepository = flytJobbRepository,
            SakService(inMemoryRepositoryProvider, gatewayProvider),
            InMemoryMottattDokumentRepository,
            PipService(inMemoryRepositoryProvider),
            BehandlingService(inMemoryRepositoryProvider, gatewayProvider),
            AvklaringsbehovService(inMemoryRepositoryProvider, gatewayProvider),
            LokalUnleash,
            InMemoryGjenståendeSykedagerRepository,
        )
    }

    private fun settOppFlyt(): Pair<BehandlingHendelseServiceImpl, Behandling> {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        return Pair(behandlingHendelseService(), behandling)
    }

    private fun lagreInnvilgelsePåForrigeBehandling(
        behandling: Behandling,
        sak: Sak
    ) {
        InMemoryUnderveisRepository.lagre(
            behandling.id, listOf(
                Underveisperiode(
                    periode = sak.rettighetsperiodeEttÅrFraStartDato(),
                    meldePeriode = sak.rettighetsperiode,
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`66_PROSENT`,
                    institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
                    arbeidsgradering = ArbeidsGradering(
                        totaltAntallTimer = TimerArbeid(antallTimer = BigDecimal(0)),
                        andelArbeid = Prosent.`0_PROSENT`,
                        fastsattArbeidsevne = Prosent.`0_PROSENT`,
                        gradering = Prosent.`0_PROSENT`,
                        opplysningerMottatt = null
                    ),
                    trekk = Dagsatser(antall = 0),
                    brukerAvKvoter = setOf(),
                    meldepliktStatus = MeldepliktStatus.FØR_VEDTAK,
                    meldepliktGradering = null,
                    backFillStansOpphorEnabled = false,
                    meldedato = null
                )
            ), object : Faktagrunnlag {})
    }
}