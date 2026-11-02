package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.flyt.InstitusjonFlytTest
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class InstitusjonsoppholdInformasjonskravIntegrasjonsTest {

    object FakeInstitusjonsoppholdGateway : InstitusjonsoppholdGateway {
        var response: List<Institusjonsopphold> = emptyList()

        override fun innhent(person: Person): List<Institusjonsopphold> = response

        override fun hentDataForHendelse(oppholdId: Long): Institusjonsopphold {
            TODO("Not yet implemented")
        }
    }

    private val gatewayProvider = minimalGatewayProvider(InstitusjonFlytTest.SammenhengendeInstitusjonsoppholdUnleash::class) {
        register<FakeInstitusjonsoppholdGateway>()
    }

    private fun institusjonsopphold(
        fom: LocalDate,
        tom: LocalDate?,
        institusjonsnavn: String,
        type: Institusjonstype = Institusjonstype.HS,
    ) = Institusjonsopphold(
        institusjonstype = type,
        kategori = Oppholdstype.H,
        startdato = fom,
        sluttdato = tom,
        orgnr = "111222333",
        institusjonsnavn = institusjonsnavn
    )

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `AS filtreres bort før lagring med og uten sammenhengende opphold`(sammenhengendeOpphold: Boolean) {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 mars 2026)
        FakeInstitusjonsoppholdGateway.response = listOf(
            institusjonsopphold(1 mars 2026, 10 mars 2026, "Aldershjem", Institusjonstype.AS),
            institusjonsopphold(11 mars 2026, 20 mars 2026, "Helseinstitusjon"),
            institusjonsopphold(21 mars 2026, null, "Fengsel", Institusjonstype.FO),
        )
        val gateways = minimalGatewayProvider(
            if (sammenhengendeOpphold) InstitusjonFlytTest.SammenhengendeInstitusjonsoppholdUnleash::class
            else AlleAvskruddUnleash::class
        ) {
            register<FakeInstitusjonsoppholdGateway>()
        }
        val informasjonskrav = InstitusjonsoppholdInformasjonskrav.konstruer(inMemoryRepositoryProvider, gateways)
        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            rettighetsperiode = sak.rettighetsperiode
        }
        val input = informasjonskrav.klargjør(kontekst)
        val registerdata = informasjonskrav.hentData(input)

        informasjonskrav.oppdater(input, registerdata, kontekst)

        assertThat(informasjonskrav.hentHvisEksisterer(behandling.id)?.oppholdene?.opphold?.map { it.verdi.type })
            .containsExactlyInAnyOrder(Institusjonstype.HS, Institusjonstype.FO)
    }

    @Test
    fun `AS skal ikke knytte et gammelt helseopphold til rettighetsperioden`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 mars 2026)
        FakeInstitusjonsoppholdGateway.response = listOf(
            institusjonsopphold(1 januar 2026, 1 februar 2026, "Helseinstitusjon"),
            institusjonsopphold(1 mars 2026, null, "Aldershjem", Institusjonstype.AS),
        )
        val informasjonskrav = InstitusjonsoppholdInformasjonskrav.konstruer(inMemoryRepositoryProvider, gatewayProvider)

        val registerdata = informasjonskrav.hentData(InstitusjonsoppholdInformasjonskrav.Input(sak))

        assertThat(registerdata.opphold).isEmpty()
        assertThat(informasjonskrav.behovForRevurdering(behandling.id)).isEmpty()
    }

    @Test
    fun `sammenhengende opphold som strekker seg inn i rettighetsperioden skal begge lagres, selv om kun det siste overlapper isolert sett`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 mars 2026)
        InMemorySakRepository.oppdaterRettighetsperiode(sak.id, Periode(1 mars 2026, Tid.MAKS))

        val oppholdA = institusjonsopphold(1 januar 2026, 1 februar 2026, "Institusjon A")
        val oppholdB = institusjonsopphold(1 februar 2026, null, "Institusjon B")
        FakeInstitusjonsoppholdGateway.response = listOf(oppholdA, oppholdB)

        val institusjonsoppholdInformasjonskrav = InstitusjonsoppholdInformasjonskrav.konstruer(
            inMemoryRepositoryProvider,
            gatewayProvider
        )

        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            this.rettighetsperiode = sak.rettighetsperiode
        }

        val input = institusjonsoppholdInformasjonskrav.klargjør(kontekst)
        val registerdata = institusjonsoppholdInformasjonskrav.hentData(input)

        assertThat(registerdata.opphold).hasSize(2)
        assertThat(registerdata.opphold.map { it.institusjonsnavn })
            .containsExactlyInAnyOrder("Institusjon A", "Institusjon B")

        val endret = institusjonsoppholdInformasjonskrav.oppdater(input, registerdata, kontekst)
        assertThat(endret).isEqualTo(Informasjonskrav.Endret.ENDRET)

        val lagretGrunnlag = institusjonsoppholdInformasjonskrav.hentHvisEksisterer(behandling.id)
        assertThat(lagretGrunnlag?.oppholdene?.opphold).hasSize(2)
    }

    @Test
    fun `opphold med gap skal ikke dras inn selv om det ligger i registeret`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 mars 2026)
        InMemorySakRepository.oppdaterRettighetsperiode(sak.id, Periode(1 mars 2026, Tid.MAKS))

        val gammeltOpphold = institusjonsopphold(1 januar 2026, 1 februar 2026, "Gammel institusjon")
        val relevantOpphold = institusjonsopphold(3 februar 2026, null, "Relevant institusjon")
        FakeInstitusjonsoppholdGateway.response = listOf(gammeltOpphold, relevantOpphold)

        val institusjonsoppholdInformasjonskrav = InstitusjonsoppholdInformasjonskrav.konstruer(
            inMemoryRepositoryProvider,
            gatewayProvider
        )

        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            this.rettighetsperiode = sak.rettighetsperiode
        }

        val input = institusjonsoppholdInformasjonskrav.klargjør(kontekst)
        val registerdata = institusjonsoppholdInformasjonskrav.hentData(input)

        assertThat(registerdata.opphold.map { it.institusjonsnavn })
            .containsExactly("Relevant institusjon")
    }
}
