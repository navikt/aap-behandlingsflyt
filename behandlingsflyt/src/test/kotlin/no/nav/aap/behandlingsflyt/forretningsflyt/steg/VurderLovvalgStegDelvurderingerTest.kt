package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.BeregnLovvalgDelvurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgDelvurderingerGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgDelvurderingerSporingGrunnlag
import no.nav.aap.etterlevelse.sporing.Sporing
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantSøknadKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgRevurdering
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryLovvalgDelvurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryOppgittUtenlandsopplysningerRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySporingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStønadsperiodeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class VurderLovvalgStegDelvurderingerTest {
    private val steg = VurderLovvalgSteg(
        inMemoryRepositoryProvider,
        createGatewayProvider { register<AlleAvskruddUnleash>() },
    )
    private val repository = InMemoryLovvalgDelvurderingRepository

    private val dato1 = 1 januar 2024
    private val dato2 = 1 januar 2025
    private val dato3 = 1 januar 2026

    private lateinit var førsteBehandling: Behandling
    private lateinit var revurdering: Behandling

    @BeforeEach
    fun setup() {
        val (_, første, revurdering) = opprettInMemorySakOgRevurdering(søknadsdato = dato1)
        this.førsteBehandling = første
        this.revurdering = revurdering
    }

    @Test
    fun `lagrer delvurdering for hver relevant dato med riktig behandling`() {
        stønadsperioder(førsteBehandling, dato1 to RelevantKravType.NY_STØNADSPERIODE, dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)

        utfør(førsteBehandling)

        val lagret = repository.hentAlle(førsteBehandling.id)
        assertThat(lagret.map { it.relevantDato }).containsExactly(dato1, dato2)
        assertThat(lagret.map { it.vurdertIBehandling }).containsOnly(førsteBehandling.id)
    }

    @Test
    fun `faller tilbake til start på rettighetsperioden uten stønadsperioder`() {
        utfør(førsteBehandling)

        assertThat(repository.hentAlle(førsteBehandling.id).map { it.relevantDato }).containsExactly(dato1)
    }

    @Test
    fun `beholder gamle vurderinger og beregner kun ny dato i revurdering`() {
        oppgittOpphold(førsteBehandling, 1 mai 2024, 1 juni 2024)
        stønadsperioder(førsteBehandling, dato1 to RelevantKravType.NY_STØNADSPERIODE, dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)
        utfør(førsteBehandling)
        val fraFørste = repository.hentAlle(førsteBehandling.id)

        // Oppholdet fra første behandling er borte, så en ny beregning for dato1 ville gitt et annet resultat
        repository.kopier(førsteBehandling.id, revurdering.id)
        oppgittOpphold(revurdering, null, null)
        stønadsperioder(
            revurdering,
            dato1 to RelevantKravType.NY_STØNADSPERIODE,
            dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR,
            dato3 to RelevantKravType.MIGRERT_STØNADSPERIODE,
        )
        utfør(revurdering)

        val lagret = repository.hentAlle(revurdering.id).associateBy { it.relevantDato }
        assertThat(lagret.keys).containsExactly(dato1, dato2, dato3)
        assertThat(lagret.getValue(dato1)).isEqualTo(fraFørste[0])
        assertThat(lagret.getValue(dato2)).isEqualTo(fraFørste[1])
        assertThat(lagret.getValue(dato1).vurdertIBehandling).isEqualTo(førsteBehandling.id)
        assertThat(lagret.getValue(dato2).vurdertIBehandling).isEqualTo(førsteBehandling.id)
        assertThat(lagret.getValue(dato3).vurdertIBehandling).isEqualTo(revurdering.id)
    }

    @Test
    fun `nyeste dato beregnes på nytt og får ny behandling når resultatet endres`() {
        stønadsperioder(førsteBehandling, dato1 to RelevantKravType.NY_STØNADSPERIODE, dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)
        utfør(førsteBehandling)
        val fraFørste = repository.hentAlle(førsteBehandling.id)

        repository.kopier(førsteBehandling.id, revurdering.id)
        oppgittOpphold(revurdering, 1 mai 2025, 1 juni 2025)
        stønadsperioder(revurdering, dato1 to RelevantKravType.NY_STØNADSPERIODE, dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)
        utfør(revurdering)

        val lagret = repository.hentAlle(revurdering.id)
        assertThat(lagret[0]).isEqualTo(fraFørste[0])
        assertThat(lagret[1].vurdering()).isNotEqualTo(fraFørste[1].vurdering())
        assertThat(lagret[1].vurdertIBehandling).isEqualTo(revurdering.id)
    }

    @Test
    fun `uendret nyeste dato beholder opprinnelig behandling`() {
        stønadsperioder(førsteBehandling, dato1 to RelevantKravType.NY_STØNADSPERIODE, dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)
        utfør(førsteBehandling)
        val fraFørste = repository.hentAlle(førsteBehandling.id)

        repository.kopier(førsteBehandling.id, revurdering.id)
        stønadsperioder(revurdering, dato1 to RelevantKravType.NY_STØNADSPERIODE, dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)
        utfør(revurdering)

        assertThat(repository.hentAlle(revurdering.id)).isEqualTo(fraFørste)
    }

    @Test
    fun `datoer som er nye i behandlingen beregnes på nytt ved ny kjøring når de ikke lenger er nyeste`() {
        stønadsperioder(førsteBehandling, dato1 to RelevantKravType.NY_STØNADSPERIODE)
        utfør(førsteBehandling)
        val fraFørste = repository.hentAlle(førsteBehandling.id)
        repository.kopier(førsteBehandling.id, revurdering.id)

        stønadsperioder(revurdering, dato1 to RelevantKravType.NY_STØNADSPERIODE, dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)
        utfør(revurdering)
        val etterFørsteKjøring = repository.hentAlle(revurdering.id).associateBy { it.relevantDato }

        oppgittOpphold(revurdering, 1 mai 2025, 1 juni 2025)
        stønadsperioder(
            revurdering,
            dato1 to RelevantKravType.NY_STØNADSPERIODE,
            dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR,
            dato3 to RelevantKravType.MIGRERT_STØNADSPERIODE,
        )
        utfør(revurdering)

        val lagret = repository.hentAlle(revurdering.id).associateBy { it.relevantDato }
        assertThat(lagret.getValue(dato1)).isEqualTo(fraFørste[0])
        assertThat(lagret.getValue(dato2).vurdering()).isNotEqualTo(etterFørsteKjøring.getValue(dato2).vurdering())
        assertThat(lagret.getValue(dato2).vurdertIBehandling).isEqualTo(revurdering.id)
        assertThat(lagret.getValue(dato3).vurdertIBehandling).isEqualTo(revurdering.id)
    }

    @Test
    fun `beregner alle datoer når forrige behandling ikke har delvurderinger`() {
        // Steget har ikke kjørt i forrige behandling, men vilkåret må finnes for at revurderingen kan kjøre
        val forrigeVilkår = InMemoryVilkårsresultatRepository.hent(førsteBehandling.id)
        forrigeVilkår.leggTilHvisIkkeEksisterer(Vilkårtype.LOVVALG)
        InMemoryVilkårsresultatRepository.lagre(førsteBehandling.id, forrigeVilkår)

        stønadsperioder(revurdering, dato1 to RelevantKravType.NY_STØNADSPERIODE, dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)
        utfør(revurdering)

        val lagret = repository.hentAlle(revurdering.id)
        assertThat(lagret.map { it.relevantDato }).containsExactly(dato1, dato2)
        assertThat(lagret.map { it.vurdertIBehandling }).containsOnly(revurdering.id)
    }

    @Test
    fun `lagrer én sporing per behandling som inneholder alle relevante datoer`() {
        stønadsperioder(førsteBehandling, dato1 to RelevantKravType.NY_STØNADSPERIODE, dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)
        utfør(førsteBehandling)

        val førsteSporing = InMemorySporingRepository.hent(førsteBehandling.id, BeregnLovvalgDelvurderinger.sporingsnøkkel)
        assertThat(førsteSporing).isNotNull
        assertThat(datoerISporing(førsteSporing!!)).containsExactly(dato1.toString(), dato2.toString())

        repository.kopier(førsteBehandling.id, revurdering.id)
        stønadsperioder(
            revurdering,
            dato1 to RelevantKravType.NY_STØNADSPERIODE,
            dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR,
            dato3 to RelevantKravType.MIGRERT_STØNADSPERIODE,
        )
        utfør(revurdering)

                val revurderingSporing = InMemorySporingRepository.hent(revurdering.id, BeregnLovvalgDelvurderinger.sporingsnøkkel)
        assertThat(datoerISporing(revurderingSporing!!))
            .containsExactly(dato1.toString(), dato2.toString(), dato3.toString())
        assertThat(revurderingSporing.grunnlag.get("forrigeBehandlingsVurderinger").map { it.get("relevantDato").asText() })
            .containsExactly(dato1.toString(), dato2.toString())
        assertThat(datoerISporing(InMemorySporingRepository.hent(førsteBehandling.id, BeregnLovvalgDelvurderinger.sporingsnøkkel)!!))
            .containsExactly(dato1.toString(), dato2.toString())
    }

    @Test
    fun `sporingen kan kjøres på nytt og gir nøyaktig det som ble lagret`() {
        oppgittOpphold(førsteBehandling, 1 mai 2024, 1 juni 2024)
        stønadsperioder(førsteBehandling, dato1 to RelevantKravType.NY_STØNADSPERIODE, dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)
        utfør(førsteBehandling)
        repository.kopier(førsteBehandling.id, revurdering.id)
        oppgittOpphold(revurdering, null, null)
        stønadsperioder(
            revurdering,
            dato1 to RelevantKravType.NY_STØNADSPERIODE,
            dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR,
            dato3 to RelevantKravType.MIGRERT_STØNADSPERIODE,
        )
        utfør(revurdering)

        val sporing = InMemorySporingRepository.hent(revurdering.id, BeregnLovvalgDelvurderinger.sporingsnøkkel)!!
        val grunnlag = DefaultJsonMapper.fromJson<LovvalgDelvurderingerSporingGrunnlag>(sporing.grunnlag.toString())

        assertThat(BeregnLovvalgDelvurderinger.kjørMedSporing(grunnlag))
            .isEqualTo(repository.hentAlle(revurdering.id))
    }

    private fun datoerISporing(sporing: Sporing): List<String> =
        sporing.grunnlag.get("relevanteDatoer").map { it.asText() }

    @Test
    fun `fjerner vurdering for dato som ikke lenger er relevant`() {
        stønadsperioder(førsteBehandling, dato1 to RelevantKravType.NY_STØNADSPERIODE, dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)
        utfør(førsteBehandling)

        repository.kopier(førsteBehandling.id, revurdering.id)
        stønadsperioder(revurdering, dato2 to RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR)
        utfør(revurdering)

        assertThat(repository.hentAlle(revurdering.id).map { it.relevantDato }).containsExactly(dato2)
    }

    private fun utfør(behandling: Behandling) {
        steg.utfør(flytKontekstMedPerioder {
            this.behandling = behandling
            this.rettighetsperiode = Periode(dato1, Tid.MAKS)
        })
    }

    private fun oppgittOpphold(behandling: Behandling, fra: LocalDate?, til: LocalDate?) {
        InMemoryOppgittUtenlandsopplysningerRepository.lagreOppgittUtenlandsOppplysninger(
            behandling.id,
            JournalpostId("1"),
            UtenlandsOppholdData(
                harBoddINorgeSiste5År = true,
                harArbeidetINorgeSiste5År = true,
                arbeidetUtenforNorgeFørSykdom = false,
                iTilleggArbeidUtenforNorge = false,
                utenlandsOpphold = if (fra == null) null else listOf(
                    UtenlandsPeriode(land = "SWE", tilDato = til, fraDato = fra, iArbeid = false, utenlandsId = "1")
                ),
            )
        )
    }

    private fun stønadsperioder(behandling: Behandling, vararg perioder: Pair<LocalDate, RelevantKravType>) {
        val kravOgStønadsperioder = perioder.map { (startDato, type) ->
            val referanse = Kravreferanse.ny()
            val krav = RelevantSøknadKrav(
                referanse = referanse,
                journalpostId = JournalpostId(startDato.toString()),
                vurdertAv = SYSTEMBRUKER,
                begrunnelse = "Test",
                vurdertIBehandling = behandling.id,
                opprettet = Instant.now(),
                søknadsdato = Søknadsdato(startDato, SøknadsdatoÅrsak.SøknadMottatt, "Test"),
                overstyrMuligRettFra = null,
                muligRettFra = startDato,
            )
            val gjeninntreden = type == RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR
            krav to StønadsperiodeVurdering(
                referanse = referanse,
                opprettet = Instant.now(),
                vurdertIBehandling = behandling.id,
                vurdertAv = SYSTEMBRUKER,
                begrunnelse = "Test",
                harHattOrdinærSiste52Uker = gjeninntreden,
                harGjenværendeKvote = gjeninntreden,
                relevantKravType = type,
                startDato = startDato,
            )
        }
        InMemoryKravRepository.lagre(behandling.id, kravOgStønadsperioder.map { it.first }.toSet())
        InMemoryStønadsperiodeRepository.lagre(
            behandling.id,
            kravOgStønadsperioder.map { it.second }.toSet()
        )
    }
}
