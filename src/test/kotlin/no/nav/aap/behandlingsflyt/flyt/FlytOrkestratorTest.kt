package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.avklaringsbehov.bistand.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.student.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.student.StudentVurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.AvklarYrkesskadeLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.NedreGrense
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.Status
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.dbconnect.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.adapter.PersonRegisterMock
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.adapter.YrkesskadeRegisterMock
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.flyt.steg.StegStatus
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.Tilstand
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsresultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.hendelse.mottak.DokumentMottattPersonHendelse
import no.nav.aap.behandlingsflyt.hendelse.mottak.HendelsesMottak
import no.nav.aap.behandlingsflyt.hendelse.mottak.LøsAvklaringsbehovBehandlingHendelse
import no.nav.aap.behandlingsflyt.prosessering.Motor
import no.nav.aap.behandlingsflyt.prosessering.OppgaveRepository
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.Person
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.SakId
import no.nav.aap.behandlingsflyt.sak.SakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FlytOrkestratorTest {

    companion object {
        val dataSource = InitTestDatabase.dataSource
        val motor = Motor(dataSource, 1)
        val hendelsesMottak = HendelsesMottak(dataSource)

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            motor.start()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            motor.stop()
        }
    }

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val ident = Ident("123123123123")
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        PersonRegisterMock.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(18))))
        YrkesskadeRegisterMock.konstruer(ident = ident, periode = periode)

        // Sender inn en søknad
        hendelsesMottak.håndtere(ident, DokumentMottattPersonHendelse(periode = periode))
        ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = hentBehandling(sak.id)
        assertThat(behandling.type).isEqualTo(Førstegangsbehandling)

        assertThat(behandling.avklaringsbehov()).isNotEmpty()
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        dataSource.transaction {
            hendelsesMottak.håndtere(
                it,
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarStudentLøsning(
                        studentvurdering = StudentVurdering(
                            begrunnelse = "Er student",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            oppfyller11_14 = false,
                            avbruttStudieDato = null
                        )
                    )
                )
            )
        }
        ventPåSvar()

        dataSource.transaction {
            hendelsesMottak.håndtere(
                it,
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarYrkesskadeLøsning(
                        yrkesskadevurdering = Yrkesskadevurdering(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            erÅrsakssammenheng = false,
                            skadetidspunkt = null
                        )
                    )
                )
            )
        }
        ventPåSvar()

        dataSource.transaction {
            hendelsesMottak.håndtere(
                it,
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarSykdomLøsning(
                        sykdomsvurdering = Sykdomsvurdering(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                            nedreGrense = NedreGrense.FEMTI,
                            nedsattArbeidsevneDato = LocalDate.now()
                        )
                    )
                )
            )
        }
        ventPåSvar()

        dataSource.transaction {
            hendelsesMottak.håndtere(
                it,
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = AvklarBistandsbehovLøsning(
                        bistandVurdering = BistandVurdering(
                            begrunnelse = "Trenger hjelp fra nav",
                            erBehovForBistand = true
                        ),
                    )
                )
            )
        }
        ventPåSvar()

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        assertThat(behandling.avklaringsbehov()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.FORESLÅ_VEDTAK }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        dataSource.transaction {
            hendelsesMottak.håndtere(
                it,
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = ForeslåVedtakLøsning("Begrunnelse")
                )
            )
        }
        ventPåSvar()

        // Saken står til To-trinnskontroll hos beslutter
        assertThat(behandling.avklaringsbehov()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.FATTE_VEDTAK }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        behandling = hentBehandling(sak.id)

        dataSource.transaction {
            hendelsesMottak.håndtere(
                it,
                behandling.id,
                LøsAvklaringsbehovBehandlingHendelse(
                    løsning = FatteVedtakLøsning(behandling.avklaringsbehov().filter { it.erTotrinn() }
                        .map { TotrinnsVurdering(it.definisjon.kode, true, "begrunnelse") })
                )
            )
        }
        ventPåSvar()

        behandling = hentBehandling(sak.id)
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }

        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(sykdomsvilkåret.vilkårsperioder())
            .hasSize(1)
            .allMatch { vilkårsperiode -> vilkårsperiode.erOppfylt() }
    }

    private fun hentSak(ident: Ident, periode: Periode): Sak {
        return dataSource.transaction { connection ->
            SakRepository(connection).finnEllerOpprett(PersonRepository(connection).finnEllerOpprett(ident), periode)
        }
    }

    private fun hentVilkårsresultat(behandlingId: BehandlingId): Vilkårsresultat {
        return dataSource.transaction { connection ->
            VilkårsresultatRepository(connection).hent(behandlingId)
        }
    }

    private fun hentBehandling(sakId: SakId): Behandling {
        return dataSource.transaction { connection ->
            val finnSisteBehandlingFor = BehandlingRepository(connection).finnSisteBehandlingFor(sakId)
            requireNotNull(finnSisteBehandlingFor)
        }
    }

    private fun ventPåSvar() {
        dataSource.transaction {
            while (OppgaveRepository(it).harOppgaver() || motor.harOppgaverKjørende()) {
                Thread.sleep(50L)
            }
        }
    }

    @Test
    fun `Ikke oppfylt på grunn av alder på søknadstidspunkt`() {
        val ident = Ident("123123123125")
        hentPerson(ident)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        PersonRegisterMock.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(17))))

        hendelsesMottak.håndtere(ident, DokumentMottattPersonHendelse(periode = periode))
        ventPåSvar()

        val sak = hentSak(ident, periode)
        val behandling = requireNotNull(hentBehandling(sak.id))
        assertThat(behandling.type).isEqualTo(Førstegangsbehandling)

        val stegHistorikk = behandling.stegHistorikk()
        assertThat(stegHistorikk.map { it.tilstand }).contains(
            Tilstand(
                StegType.AVKLAR_STUDENT,
                StegStatus.AVKLARINGSPUNKT
            )
        )

        //Henter vurder alder-vilkår
        //Assert utfall
        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val aldersvilkår = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(aldersvilkår.vilkårsperioder())
            .hasSize(1)
            .noneMatch { vilkårsperiodeForAlder -> vilkårsperiodeForAlder.erOppfylt() }
    }

    private fun hentPerson(ident: Ident): Person {
        var person: Person? = null
        dataSource.transaction {
            person = PersonRepository(it).finnEllerOpprett(ident)
        }
        return person!!
    }

    @Test
    fun `Blir satt på vent for etterspørring av informasjon`() {
        val ident = Ident("123123123127")
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        PersonRegisterMock.konstruer(ident, Personopplysning(Fødselsdato(LocalDate.now().minusYears(20))))

        hendelsesMottak.håndtere(ident, DokumentMottattPersonHendelse(periode = periode))
        ventPåSvar()

        val sak = hentSak(ident, periode)
        var behandling = requireNotNull(hentBehandling(sak.id))

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        assertThat(behandling.avklaringsbehov()).anySatisfy { it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM }

        hendelsesMottak.håndtere(
            behandling.id,
            BehandlingSattPåVent()
        )

        behandling = hentBehandling(sak.id)
        assertThat(behandling.avklaringsbehov())
            .hasSize(2)
            .anySatisfy { it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT }
            .anySatisfy { it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM }

        hendelsesMottak.håndtere(ident, DokumentMottattPersonHendelse(periode = periode))
        ventPåSvar()

        behandling = hentBehandling(sak.id)
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        assertThat(behandling.avklaringsbehov())
            .hasSize(2)
            .anySatisfy { !it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT }
            .anySatisfy { it.erÅpent() && it.definisjon == Definisjon.AVKLAR_SYKDOM }

    }
}
