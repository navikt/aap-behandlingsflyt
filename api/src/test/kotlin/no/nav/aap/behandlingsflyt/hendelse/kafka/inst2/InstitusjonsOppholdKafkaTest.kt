package no.nav.aap.behandlingsflyt.hendelse.kafka.inst2

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.hendelse.mottak.MottattHendelseService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.InstitusjonsOppholdHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.InstitusjonsoppholdKilde
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.InstitusjonsoppholdsType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFlytJobbRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemoryBehandlingService
import no.nav.aap.behandlingsflyt.test.januar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource

class InstitusjonsOppholdKafkaTest {
    object FakeInstitusjonsoppholdGateway : InstitusjonsoppholdGateway {
        lateinit var opphold: Institusjonsopphold

        override fun innhent(person: Person): List<Institusjonsopphold> = listOf(opphold)

        override fun hentDataForHendelse(oppholdId: Long): Institusjonsopphold = opphold
    }

    private val service = InstitusjonsOppholdService(
        sakRepository = InMemorySakRepository,
        personRepository = InMemoryPersonRepository,
        hendelseService = MottattHendelseService(inMemoryRepositoryProvider),
        trukketSøknadService = TrukketSøknadService(inMemoryRepositoryProvider),
        behandlingService = InMemoryBehandlingService,
        institusjonsoppholdKlient = FakeInstitusjonsoppholdGateway,
    )

    @ParameterizedTest
    @NullSource
    @EnumSource(Institusjonstype::class)
    fun `AS ignoreres uavhengig av tidligere registeropphold`(tidligereType: Institusjonstype?) {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 1 januar 2026)
        if (tidligereType != null) {
            inMemoryRepositoryProvider.provide<InstitusjonsoppholdRepository>()
                .lagreOpphold(behandling.id, listOf(opphold(tidligereType)))
        }
        FakeInstitusjonsoppholdGateway.opphold = opphold(Institusjonstype.AS)

        service.håndter("melding", melding(sak.person))

        assertThat(InMemoryFlytJobbRepository.hentJobberForSak(sak.id.id)).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(Institusjonstype::class, names = ["HS", "FO"])
    fun `helse og soning registreres fortsatt`(type: Institusjonstype) {
        val (sak, _) = opprettInMemorySakOgBehandling(søknadsdato = 1 januar 2026)
        FakeInstitusjonsoppholdGateway.opphold = opphold(type)

        service.håndter("melding", melding(sak.person))

        assertThat(InMemoryFlytJobbRepository.hentJobberForSak(sak.id.id)).hasSize(1)
    }

    @Test
    fun `AS ignoreres for sak uten ytelsesbehandling`() {
        val sak = opprettInMemorySak(søknadsdato = 1 januar 2026)
        FakeInstitusjonsoppholdGateway.opphold = opphold(Institusjonstype.AS)

        service.håndter("melding", melding(sak.person))

        assertThat(InMemoryFlytJobbRepository.hentJobberForSak(sak.id.id)).isEmpty()
    }

    private fun melding(person: Person) = InstitusjonsOppholdHendelseKafkaMelding(
        hendelseId = 1,
        oppholdId = 1,
        norskident = person.aktivIdent().identifikator,
        type = InstitusjonsoppholdsType.OPPDATERING,
        kilde = InstitusjonsoppholdKilde.INST,
        institusjonsOpphold = null,
    )

    private fun opphold(type: Institusjonstype) = Institusjonsopphold(
        institusjonstype = type,
        kategori = when (type) {
            Institusjonstype.AS -> Oppholdstype.A
            Institusjonstype.HS -> Oppholdstype.H
            Institusjonstype.FO -> Oppholdstype.S
        },
        startdato = 1 januar 2026,
        sluttdato = null,
        orgnr = "123456789",
        institusjonsnavn = "Institusjon",
    )
}
