package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringStatusResponse
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
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object FakeServers : AutoCloseable {
    private val log = LoggerFactory.getLogger(javaClass)

    private val started = AtomicBoolean(false)

    // Fakes with own state
    private val statistikk = StatistikkFake()
    private val dokumentinnhenting = DokumentinnhentingFake()
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
    internal val legeerklæringStatuser: MutableList<LegeerklæringStatusResponse> get() = dokumentinnhenting.statuser

    fun start(testPersonService: TestPersonService = FakePersoner) {
        if (started.get()) {
            return
        }

        fakePersoner = testPersonService
        allFakes.forEach { it.start() }
        setProperties()
        started.set(true)
    }

    private fun setProperties() {
        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
        System.setProperty("nais.app.name", "behandlingsflyt")

        // Brev
        System.setProperty("integrasjon.brev.url", "http://localhost:${brev.port()}")
        System.setProperty("integrasjon.brev.scope", "brev")

        // Pdl
        System.setProperty("integrasjon.pdl.url", "http://localhost:${pdl.port()}")
        System.setProperty("integrasjon.pdl.scope", "pdl")

        //popp
        System.setProperty("integrasjon.inntekt.url", "http://localhost:${popp.port()}")
        System.setProperty("integrasjon.inntekt.scope", "popp")

        // Yrkesskade
        System.setProperty("integrasjon.yrkesskade.url", "http://localhost:${yrkesskade.port()}")
        System.setProperty("integrasjon.yrkesskade.scope", "yrkesskade")

        // Oppgavestyring
        System.setProperty("integrasjon.oppgavestyring.scope", "oppgavestyring")
        if (System.getenv("INTEGRASJON_OPPGAVESTYRING_URL").isNullOrEmpty()) {
            System.setProperty("integrasjon.oppgavestyring.url", "http://localhost:${oppgavestyring.port()}")
        }

        // MEDL
        System.setProperty("integrasjon.medl.url", "http://localhost:${medl.port()}")
        System.setProperty("integrasjon.medl.scope", "medl")

        // Inst
        System.setProperty("integrasjon.institusjonsopphold.url", "http://localhost:${inst2.port()}")
        System.setProperty("integrasjon.institusjonsoppholdenkelt.url", "http://localhost:${inst2.port()}")
        System.setProperty("integrasjon.institusjonsopphold.scope", "inst2")

        // Statistikk-app
        System.setProperty("integrasjon.statistikk.url", "http://localhost:${statistikk.port()}")
        System.setProperty("integrasjon.statistikk.scope", "statistikk")

        // Pesys
        System.setProperty("integrasjon.pesys.url", "http://localhost:${pesys.port()}")
        System.setProperty("integrasjon.pesys.scope", "scope")

        // Tilgang
        System.setProperty("integrasjon.tilgang.url", "http://localhost:${tilgang.port()}")
        System.setProperty("integrasjon.tilgang.scope", "scope")

        // Foreldrepenger
        System.setProperty("integrasjon.foreldrepenger.url", "http://localhost:${foreldrepenger.port()}")
        System.setProperty("integrasjon.foreldrepenger.scope", "scope")

        // Sykepenger
        System.setProperty("integrasjon.sykepenger.url", "http://localhost:${sykepenger.port()}")
        System.setProperty("integrasjon.sykepenger.scope", "scope")

        // Dokumentinnhenting
        System.setProperty(
            "integrasjon.dokumentinnhenting.url",
            "http://localhost:${dokumentinnhenting.port()}"
        )
        System.setProperty("integrasjon.dokumentinnhenting.scope", "scope")

        // Dagpenger
        System.setProperty("integrasjon.dagpenger.url", "http://localhost:${dagpenger.port()}")
        System.setProperty("integrasjon.dagpenger.scope", "scope")

        // Tiltakspenger
        System.setProperty("integrasjon.tiltakspenger.url", "http://localhost:${tiltakspenger.port()}")
        System.setProperty("integrasjon.tiltakspenger.scope", "scope")

        // Dokarkiv
        System.setProperty("integrasjon.dokarkiv.url", "http://localhost:${dokarkiv.port()}")
        System.setProperty("integrasjon.dokarkiv.scope", "scope")

        // AAregisteret
        System.setProperty("integrasjon.aareg.url", "http://localhost:${aareg.port()}")
        System.setProperty("integrasjon.aareg.scope", "scope")

        // Inntektskomponenten
        System.setProperty("integrasjon.inntektskomponenten.url", "http://localhost:${ainntekt.port()}")
        System.setProperty("integrasjon.inntektskomponenten.scope", "scope")

        // Datadeling
        System.setProperty("integrasjon.datadeling.url", "http://localhost:${datadeling.port()}")
        System.setProperty("integrasjon.datadeling.scope", "scope")

        // Utbetal
        System.setProperty("integrasjon.utbetal.url", "http://localhost:${utbetal.port()}")
        System.setProperty("integrasjon.utbetal.scope", "utbetal")

        // Meldekort
        System.setProperty("integrasjon.meldekort.url", "http://localhost:${meldekort.port()}")
        System.setProperty("integrasjon.meldekort.scope", "meldekort")

        //tjenestepensjon
        System.setProperty("integrasjon.tjenestepensjon.url", "http://localhost:${tjenestePensjon.port()}")
        System.setProperty("integrasjon.tjenestepensjon.scope", "tjenestepensjon")

        // Dokarkiv
        System.setProperty("integrasjon.dokarkiv.url", "http://localhost:${dokarkiv.port()}")
        System.setProperty("integrasjon.dokarkiv.scope", "scope")

        //unleash
        System.setProperty("nais.app.name", "behandlingsflyt")
        System.setProperty("unleash.server.api.url", "http://localhost:${unleash.port()}")
        System.setProperty("unleash.server.api.token", "token-behandlingsflyt-unleash")

        //Azp
        System.setProperty("integrasjon.tilgang.azp", java.util.UUID.randomUUID().toString())
        System.setProperty("integrasjon.brev.azp", java.util.UUID.randomUUID().toString())
        System.setProperty("integrasjon.dokumentinnhenting.azp", java.util.UUID.randomUUID().toString())
        System.setProperty("integrasjon.postmottak.azp", java.util.UUID.randomUUID().toString())
        System.setProperty("integrasjon.saksbehandling.azp", java.util.UUID.randomUUID().toString())
        System.setProperty("integrasjon.azure.token.generator.azp", java.util.UUID.randomUUID().toString())

        // Norg
        System.setProperty("integrasjon.norg.url", "http://localhost:${norg.port()}")

        // NOM
        System.setProperty("integrasjon.nom.url", "http://localhost:${nom.port()}/graphql")
        System.setProperty("integrasjon.nom.scope", "scope")

        // Kabal
        System.setProperty("integrasjon.kabal.url", "http://localhost:${kabal.port()}")
        System.setProperty("integrasjon.kabal.scope", "scope")

        //Enhetsregisteret
        System.setProperty("integrasjon.ereg.url", "http://localhost:${ereg.port()}")
        System.setProperty("integrasjon.ereg.scope", "scope")

        // Sam
        System.setProperty("integrasjon.sam.url", "http://localhost:${sam.port()}")
        System.setProperty("integrasjon.sam.scope", "sam")

        // Gosys
        System.setProperty("integrasjon.gosys.url", "http://localhost:${gosys.port()}")
        System.setProperty("integrasjon.gosys.scope", "scope")

        // Texas
        System.setProperty("nais.token.endpoint", "http://localhost:${texas.port()}/token")
        System.setProperty("nais.token.exchange.endpoint", "http://localhost:${texas.port()}/token/exchange")
        System.setProperty("nais.token.introspection.endpoint", "http://localhost:${texas.port()}/introspect")

        // LeaderElector
        System.setProperty("ELECTOR_GET_URL", "http://localhost:${leaderElector.port()}")

        // aap-saksbehandling-pdf
        System.setProperty("integrasjon.pdfgen.url", "http://localhost:${pdfGen.port()}")
        System.setProperty("integrasjon.pdfgen.scope", "scope")
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
