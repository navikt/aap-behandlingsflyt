package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.test.fakes.AaregFake
import no.nav.aap.behandlingsflyt.test.fakes.AinntektFake
import no.nav.aap.behandlingsflyt.test.fakes.BrevFake
import no.nav.aap.behandlingsflyt.test.fakes.DagpengerFake
import no.nav.aap.behandlingsflyt.test.fakes.DatadelingFake
import no.nav.aap.behandlingsflyt.test.fakes.DokarkivFake
import no.nav.aap.behandlingsflyt.test.fakes.DokumentinnhentingFake
import no.nav.aap.behandlingsflyt.test.fakes.EregFake
import no.nav.aap.behandlingsflyt.test.fakes.FakeServer
import no.nav.aap.behandlingsflyt.test.fakes.ForeldrepengerFake
import no.nav.aap.behandlingsflyt.test.fakes.GosysFake
import no.nav.aap.behandlingsflyt.test.fakes.Inst2Fake
import no.nav.aap.behandlingsflyt.test.fakes.KabalFake
import no.nav.aap.behandlingsflyt.test.fakes.LeaderElectorFake
import no.nav.aap.behandlingsflyt.test.fakes.MedlFake
import no.nav.aap.behandlingsflyt.test.fakes.MeldekortFake
import no.nav.aap.behandlingsflyt.test.fakes.NomFake
import no.nav.aap.behandlingsflyt.test.fakes.NorgFake
import no.nav.aap.behandlingsflyt.test.fakes.OppgavestyringFake
import no.nav.aap.behandlingsflyt.test.fakes.PdfgenFake
import no.nav.aap.behandlingsflyt.test.fakes.PdlFake
import no.nav.aap.behandlingsflyt.test.fakes.PesysFake
import no.nav.aap.behandlingsflyt.test.fakes.PoppFake
import no.nav.aap.behandlingsflyt.test.fakes.SamFake
import no.nav.aap.behandlingsflyt.test.fakes.StatistikkFake
import no.nav.aap.behandlingsflyt.test.fakes.SykepengerFake
import no.nav.aap.behandlingsflyt.test.fakes.TexasFake
import no.nav.aap.behandlingsflyt.test.fakes.TilgangFake
import no.nav.aap.behandlingsflyt.test.fakes.TiltakspengerFake
import no.nav.aap.behandlingsflyt.test.fakes.TjenestePensjonFake
import no.nav.aap.behandlingsflyt.test.fakes.UnleashFake
import no.nav.aap.behandlingsflyt.test.fakes.UtbetalFake
import no.nav.aap.behandlingsflyt.test.fakes.YrkesskadeFake
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object FakeServers : AutoCloseable {
    private val log = LoggerFactory.getLogger(javaClass)

    private val started = AtomicBoolean(false)

    // Fakes with own state
    private val statistikk = StatistikkFake()
    private val dokumentinnhenting = DokumentinnhentingFake { fakePersoner }
    private val brev = BrevFake()

    // Stateless fakes
    private val texas = TexasFake()
    private val oppgavestyring = OppgavestyringFake()
    private val sam = SamFake()
    private val gosys = GosysFake()
    private val tilgang = TilgangFake()
    private val unleash = UnleashFake()
    private val kabal = KabalFake()
    private val aareg = AaregFake()
    private val ereg = EregFake()
    private val datadeling = DatadelingFake()
    private val utbetal = UtbetalFake()
    private val meldekort = MeldekortFake()
    private val nom = NomFake()
    private val norg = NorgFake()
    private val leaderElector = LeaderElectorFake()
    private val pdfGen = PdfgenFake()

    // Fakes needing fakePersoner - lazy lambda
    private val pdl = PdlFake { fakePersoner }
    private val yrkesskade = YrkesskadeFake { fakePersoner }
    private val popp = PoppFake { fakePersoner }
    private val pesys = PesysFake { fakePersoner }
    private val inst2 = Inst2Fake { fakePersoner }
    private val medl = MedlFake { fakePersoner }
    private val foreldrepenger = ForeldrepengerFake { fakePersoner }
    private val sykepenger = SykepengerFake { fakePersoner }
    private val dagpenger = DagpengerFake { fakePersoner }
    private val tiltakspenger = TiltakspengerFake { fakePersoner }
    private val tjenestePensjon = TjenestePensjonFake { fakePersoner }
    private val ainntekt = AinntektFake { fakePersoner }
    private val dokarkiv = DokarkivFake()

    private val allFakes: List<FakeServer> = listOf(
        texas, brev, yrkesskade, pdl, popp, oppgavestyring, inst2, sam, medl, tilgang, foreldrepenger, pesys,
        sykepenger, statistikk, dokumentinnhenting, ainntekt, aareg, datadeling, utbetal, meldekort, tjenestePensjon,
        unleash, nom, norg, kabal, ereg, dagpenger, tiltakspenger, gosys, leaderElector, dokarkiv, pdfGen
    )

    private lateinit var fakePersoner: TestPersonService

    // Forwarded state
    internal val statistikkHendelser: MutableList<StoppetBehandling> get() = statistikk.hendelser
    internal val legeerklæringStatuser: MutableList<DialogmeldingStatusTilBehandslingsflytDto> get() = dokumentinnhenting.statuser

    fun start(testPersonService: TestPersonService = FakePersoner) {
        if (!started.compareAndSet(false, true)) {
            return
        }

        fakePersoner = testPersonService
        allFakes.forEach { it.start() }
        setProperties()
    }

    private fun setProperties() {
        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
        System.setProperty("NAIS_APP_NAME", "behandlingsflyt")

        // Brev
        System.setProperty("INTEGRASJON_BREV_URL", "http://localhost:${brev.port()}")
        System.setProperty("INTEGRASJON_BREV_SCOPE", "brev")

        // Pdl
        System.setProperty("INTEGRASJON_PDL_URL", "http://localhost:${pdl.port()}")
        System.setProperty("INTEGRASJON_PDL_SCOPE", "pdl")

        //popp
        System.setProperty("INTEGRASJON_INNTEKT_URL", "http://localhost:${popp.port()}")
        System.setProperty("INTEGRASJON_INNTEKT_SCOPE", "popp")

        // Yrkesskade
        System.setProperty("INTEGRASJON_YRKESSKADE_URL", "http://localhost:${yrkesskade.port()}")
        System.setProperty("INTEGRASJON_YRKESSKADE_SCOPE", "yrkesskade")

        // Oppgavestyring
        System.setProperty("INTEGRASJON_OPPGAVESTYRING_SCOPE", "oppgavestyring")
        if (System.getenv("INTEGRASJON_OPPGAVESTYRING_URL").isNullOrEmpty()) {
            System.setProperty("INTEGRASJON_OPPGAVESTYRING_URL", "http://localhost:${oppgavestyring.port()}")
        }

        // MEDL
        System.setProperty("INTEGRASJON_MEDL_URL", "http://localhost:${medl.port()}")
        System.setProperty("INTEGRASJON_MEDL_SCOPE", "medl")

        // Inst
        System.setProperty("INTEGRASJON_INSTITUSJONSOPPHOLD_URL", "http://localhost:${inst2.port()}")
        System.setProperty("INTEGRASJON_INSTITUSJONSOPPHOLDENKELT_URL", "http://localhost:${inst2.port()}")
        System.setProperty("INTEGRASJON_INSTITUSJONSOPPHOLD_SCOPE", "inst2")

        // Statistikk-app
        System.setProperty("INTEGRASJON_STATISTIKK_URL", "http://localhost:${statistikk.port()}")
        System.setProperty("INTEGRASJON_STATISTIKK_SCOPE", "statistikk")

        // Pesys
        System.setProperty("INTEGRASJON_PESYS_URL", "http://localhost:${pesys.port()}")
        System.setProperty("INTEGRASJON_PESYS_SCOPE", "scope")

        // Tilgang
        if (System.getenv("INTEGRASJON_TILGANG_URL").isNullOrEmpty()) {
            System.setProperty("INTEGRASJON_TILGANG_URL", "http://localhost:${tilgang.port()}")
        }
        System.setProperty("INTEGRASJON_TILGANG_SCOPE", "scope")

        // Foreldrepenger
        System.setProperty("INTEGRASJON_FORELDREPENGER_URL", "http://localhost:${foreldrepenger.port()}")
        System.setProperty("INTEGRASJON_FORELDREPENGER_SCOPE", "scope")

        // Sykepenger
        System.setProperty("INTEGRASJON_SYKEPENGER_URL", "http://localhost:${sykepenger.port()}")
        System.setProperty("INTEGRASJON_SYKEPENGER_SCOPE", "scope")

        // Dokumentinnhenting
        if (System.getenv("INTEGRASJON_DOKUMENTINNHENTING_URL").isNullOrEmpty()) {
            System.setProperty("INTEGRASJON_DOKUMENTINNHENTING_URL", "http://localhost:${dokumentinnhenting.port()}")
        }
        System.setProperty("INTEGRASJON_DOKUMENTINNHENTING_SCOPE", "dokumentinnhenting")

        // Dagpenger
        System.setProperty("INTEGRASJON_DAGPENGER_URL", "http://localhost:${dagpenger.port()}")
        System.setProperty("INTEGRASJON_DAGPENGER_SCOPE", "scope")

        // Tiltakspenger
        System.setProperty("INTEGRASJON_TILTAKSPENGER_URL", "http://localhost:${tiltakspenger.port()}")
        System.setProperty("INTEGRASJON_TILTAKSPENGER_SCOPE", "scope")

        // Dokarkiv
        System.setProperty("INTEGRASJON_DOKARKIV_URL", "http://localhost:${dokarkiv.port()}")
        System.setProperty("INTEGRASJON_DOKARKIV_SCOPE", "scope")

        // AAregisteret
        System.setProperty("INTEGRASJON_AAREG_URL", "http://localhost:${aareg.port()}")
        System.setProperty("INTEGRASJON_AAREG_SCOPE", "scope")

        // Inntektskomponenten
        System.setProperty("INTEGRASJON_INNTEKTSKOMPONENTEN_URL", "http://localhost:${ainntekt.port()}")
        System.setProperty("INTEGRASJON_INNTEKTSKOMPONENTEN_SCOPE", "scope")

        // Datadeling
        System.setProperty("INTEGRASJON_DATADELING_URL", "http://localhost:${datadeling.port()}")
        System.setProperty("INTEGRASJON_DATADELING_SCOPE", "scope")

        // Utbetal
        System.setProperty("INTEGRASJON_UTBETAL_URL", "http://localhost:${utbetal.port()}")
        System.setProperty("INTEGRASJON_UTBETAL_SCOPE", "utbetal")

        // Meldekort
        System.setProperty("INTEGRASJON_MELDEKORT_URL", "http://localhost:${meldekort.port()}")
        System.setProperty("INTEGRASJON_MELDEKORT_SCOPE", "meldekort")

        //tjenestepensjon
        System.setProperty("INTEGRASJON_TJENESTEPENSJON_URL", "http://localhost:${tjenestePensjon.port()}")
        System.setProperty("INTEGRASJON_TJENESTEPENSJON_SCOPE", "tjenestepensjon")

        // Dokarkiv
        System.setProperty("INTEGRASJON_DOKARKIV_URL", "http://localhost:${dokarkiv.port()}")
        System.setProperty("INTEGRASJON_DOKARKIV_SCOPE", "scope")

        //unleash
        System.setProperty("NAIS_APP_NAME", "behandlingsflyt")
        System.setProperty("UNLEASH_SERVER_API_URL", "http://localhost:${unleash.port()}")
        System.setProperty("UNLEASH_SERVER_API_TOKEN", "token-behandlingsflyt-unleash")

        //Azp
        System.setProperty("INTEGRASJON_TILGANG_AZP", java.util.UUID.randomUUID().toString())
        System.setProperty("INTEGRASJON_BREV_AZP", java.util.UUID.randomUUID().toString())
        System.setProperty("INTEGRASJON_DOKUMENTINNHENTING_AZP", java.util.UUID.randomUUID().toString())
        System.setProperty("INTEGRASJON_POSTMOTTAK_AZP", java.util.UUID.randomUUID().toString())
        System.setProperty("INTEGRASJON_SAKSBEHANDLING_AZP", java.util.UUID.randomUUID().toString())
        System.setProperty("INTEGRASJON_AZURE_TOKEN_GENERATOR_AZP", java.util.UUID.randomUUID().toString())

        // Norg
        System.setProperty("INTEGRASJON_NORG_URL", "http://localhost:${norg.port()}")

        // NOM
        System.setProperty("INTEGRASJON_NOM_URL", "http://localhost:${nom.port()}/graphql")
        System.setProperty("INTEGRASJON_NOM_SCOPE", "scope")

        // Kabal
        System.setProperty("INTEGRASJON_KABAL_URL", "http://localhost:${kabal.port()}")
        System.setProperty("INTEGRASJON_KABAL_SCOPE", "scope")

        //Enhetsregisteret
        System.setProperty("INTEGRASJON_EREG_URL", "http://localhost:${ereg.port()}")
        System.setProperty("INTEGRASJON_EREG_SCOPE", "scope")

        // Sam
        System.setProperty("INTEGRASJON_SAM_URL", "http://localhost:${sam.port()}")
        System.setProperty("INTEGRASJON_SAM_SCOPE", "sam")

        // Gosys
        System.setProperty("INTEGRASJON_GOSYS_URL", "http://localhost:${gosys.port()}")
        System.setProperty("INTEGRASJON_GOSYS_SCOPE", "scope")

        // Texas
        System.setProperty("NAIS_TOKEN_ENDPOINT", "http://localhost:${texas.port()}/token")
        System.setProperty("NAIS_TOKEN_EXCHANGE_ENDPOINT", "http://localhost:${texas.port()}/token/exchange")
        System.setProperty("NAIS_TOKEN_INTROSPECTION_ENDPOINT", "http://localhost:${texas.port()}/introspect")

        // LeaderElector
        System.setProperty("ELECTOR_GET_URL", "http://localhost:${leaderElector.port()}")

        // aap-saksbehandling-pdf
        System.setProperty("INTEGRASJON_PDFGEN_URL", "http://localhost:${pdfGen.port()}")
        System.setProperty("INTEGRASJON_PDFGEN_SCOPE", "scope")
    }

    override fun close() {
        log.info("Closing Servers.")
        if (!started.get()) {
            return
        }
        allFakes.forEach { it.stop() }
    }
}

object TexasPortHolder {
    private val texasPort = AtomicInteger(0)

    fun setPort(port: Int) {
        texasPort.set(port)
    }

    fun getPort(): Int {
        return texasPort.get()
    }
}