package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
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
import java.time.LocalDate

class InstitusjonsoppholdInformasjonskravIntegrasjonsTest {

    object FakeInstitusjonsoppholdGateway : InstitusjonsoppholdGateway {
        var response: List<Institusjonsopphold> = emptyList()

        override fun innhent(person: Person): List<Institusjonsopphold> = response

        override fun hentDataForHendelse(oppholdId: Long): Institusjonsopphold {
            TODO("Not yet implemented")
        }
    }

    private val gatewayProvider = minimalGatewayProvider {
        register<FakeInstitusjonsoppholdGateway>()
    }

    private fun institusjonsopphold(
        fom: LocalDate,
        tom: LocalDate?,
        institusjonsnavn: String
    ) = Institusjonsopphold(
        institusjonstype = Institusjonstype.HS,
        kategori = Oppholdstype.H,
        startdato = fom,
        sluttdato = tom,
        orgnr = "111222333",
        institusjonsnavn = institusjonsnavn
    )

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
