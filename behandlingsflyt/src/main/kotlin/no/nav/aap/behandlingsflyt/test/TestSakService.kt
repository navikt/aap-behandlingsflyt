package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PdlQueryException
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerDto
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
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDate
import java.time.LocalDateTime

class TestSakService(
    private val sakRepository: SakRepository,
    private val personRepository: PersonRepository,
    private val flytJobbRepository: FlytJobbRepository,
    private val identGateway: IdentGateway,
    private val apiInternGateway: ApiInternGateway
) {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakRepository = repositoryProvider.provide(),
        personRepository = repositoryProvider.provide(),
        flytJobbRepository = repositoryProvider.provide(),
        identGateway = gatewayProvider.provide(),
        apiInternGateway = gatewayProvider.provide()
    )

    fun opprettTestSak(
        ident: Ident,
        erStudent: Boolean,
        harYrkesskade: Boolean,
        harMedlemskap: Boolean,
        andreUtbetalinger: AndreUtbetalingerDto?,
        søknadsdato: LocalDate? = null,
    ): Sak {
        if (Miljø.erProd()) {
            error("Man kan ikke opprette testsaker i produksjon")
        }

        validerIdent(ident)

        val personOgSakService = PersonOgSakService(
            identGateway,
            apiInternGateway,
            personRepository,
            sakRepository
        )

        personOgSakService.finnSakerFor(ident).firstOrNull()?.let { return it }

        val sak = personOgSakService.finnEllerOpprett(ident, søknadsdato ?: LocalDate.now())

        val melding = SøknadV0(
            student = SøknadStudentDto(erStudent = erStudent.toJaNei()),
            yrkesskade = if (harYrkesskade) "Ja" else "Nei",
            oppgitteBarn = null,
            andreUtbetalinger = andreUtbetalinger,
            medlemskap = SøknadMedlemskapDto(
                harBoddINorgeSiste5År = if (harMedlemskap) "JA" else "NEI",
                harArbeidetINorgeSiste5År = null,
                arbeidetUtenforNorgeFørSykdom = null,
                iTilleggArbeidUtenforNorge = null,
                utenlandsOpphold = emptyList()
            )
        )

        flytJobbRepository.leggTil(
            HendelseMottattHåndteringJobbUtfører.nyJobb(
                sakId = sak.id,
                dokumentReferanse = InnsendingReferanse(JournalpostId("${System.currentTimeMillis()}")),
                brevkategori = InnsendingType.SØKNAD,
                kanal = Kanal.DIGITAL,
                melding = melding,
                mottattTidspunkt = søknadsdato?.atStartOfDay() ?: LocalDateTime.now(),
            )
        )

        return sak
    }

    private fun validerIdent(ident: Ident) {
        val identer = try {
            identGateway.hentAlleIdenterForPerson(ident)
        } catch (e: PdlQueryException) {
            throw if (e.message?.contains("Fant ikke person") == true) {
                OpprettTestSakException("Fant ikke person i PDL")
            } else {
                e
            }
        }
        if (identer.isEmpty()) {
            throw OpprettTestSakException("Fant ikke ident i PDL. Har man brukt en gyldig bruker fra Dolly?")
        }
    }

    private fun Boolean.toJaNei() = if (this) StudentStatus.Ja else StudentStatus.Nei
}

class OpprettTestSakException(message: String) : RuntimeException(message)