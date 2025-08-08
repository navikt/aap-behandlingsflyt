package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDate
import java.time.LocalDateTime

class TestSakService(
    private val sakRepository: SakRepository,
    private val personRepository: PersonRepository,
    private val flytJobbRepository: FlytJobbRepository,
    private val identGateway: IdentGateway
) {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): this(
        sakRepository = repositoryProvider.provide(),
        personRepository = repositoryProvider.provide(),
        flytJobbRepository = repositoryProvider.provide(),
        identGateway = gatewayProvider.provide()
    )


    fun opprettTestSak(ident: Ident, erStudent: Boolean, harYrkesskade: Boolean, harMedlemskap: Boolean): Sak {
        if(Miljø.erProd()) {
            throw RuntimeException("Man kan ikke opprette testtsaker i produskjon")
        }

        val identer = identGateway.hentAlleIdenterForPerson(ident)
        if(identer.isEmpty()) {
            throw RuntimeException("Fant ikke ident i PDL. Har man brukt en gyldig bruker fra Dolly?")
        }

        val sakService = PersonOgSakService(
            identGateway,
            personRepository,
            sakRepository
        )

        val periode = Periode(
            LocalDate.now(),
            LocalDate.now().plusYears(1).minusDays(1)
        )

        if(sakService.finnSakerFor(ident).isNotEmpty()) {
            throw RuntimeException("Det finnes allerede en eller flere saker for bruker $ident. Vennligst bruk en annen testbruker eller gjenbruk den åpne saken.")
        }

        val sak = sakService.finnEllerOpprett(ident, periode)

        val melding = SøknadV0(
            student = SøknadStudentDto(erStudent = erStudent.toJaNei()),
            yrkesskade = harYrkesskade.toJaNei(),
            oppgitteBarn = null,
            medlemskap = SøknadMedlemskapDto(
                harBoddINorgeSiste5År =  harMedlemskap.toJaNei(),
                harArbeidetINorgeSiste5År = null,
                arbeidetUtenforNorgeFørSykdom = null,
                iTilleggArbeidUtenforNorge = null,
                utenlandsOpphold = emptyList()
            )
        )

        flytJobbRepository.leggTil(
            HendelseMottattHåndteringJobbUtfører.nyJobb(
                sakId = sak.id,
                dokumentReferanse = InnsendingReferanse(JournalpostId("" + System.currentTimeMillis())),
                brevkategori = InnsendingType.SØKNAD,
                kanal = Kanal.DIGITAL,
                melding = melding,
                mottattTidspunkt = LocalDateTime.now(),
            )
        )

        return sak
    }

    private fun Boolean.toJaNei() = if (this) "JA" else "NEI"
}