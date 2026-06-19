package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PdlQueryException
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDate
import java.time.LocalDateTime

class TestSakService(
    private val flytJobbRepository: FlytJobbRepository,
    private val identGateway: IdentGateway,
    private val personOgSakService: PersonOgSakService,
    private val behandlingService: BehandlingService,
    private val mottattDokumentRepository: MottattDokumentRepository
) {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        flytJobbRepository = repositoryProvider.provide(),
        identGateway = gatewayProvider.provide(),
        personOgSakService = PersonOgSakService(gatewayProvider, repositoryProvider),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        mottattDokumentRepository = repositoryProvider.provide()
    )

    fun opprettTestSak(
        ident: Ident,
        erStudent: Boolean,
        harYrkesskade: Boolean,
        harMedlemskap: Boolean,
        andreUtbetalinger: AndreUtbetalingerDto?,
        søknadsdato: LocalDate? = null,
    ): OpprettTestSakResultat {
        if (Miljø.erProd()) {
            error("Man kan ikke opprette testsaker i produksjon")
        }

        validerIdent(ident)

        personOgSakService.finnSakerFor(ident).firstOrNull()?.let { eksisterendeSak ->
            val sisteBehandling = behandlingService.finnSisteYtelsesbehandlingFor(eksisterendeSak.id)
            if (sisteBehandling != null && sisteBehandling.status() == Status.AVSLUTTET) {
                val nySøknad = lagSøknad(erStudent, harYrkesskade, harMedlemskap, andreUtbetalinger)
                val forrigeSøknad = mottattDokumentRepository
                    .hentDokumenterAvType(sisteBehandling.id, InnsendingType.SØKNAD)
                    .firstOrNull()
                    ?.strukturerteData<SøknadV0>()
                    ?.data
                if (nySøknad != forrigeSøknad) {
                    // Annerledes payload — legg til ny søknadsjobb for å utløse revurdering
                    leggTilSøknadJobb(eksisterendeSak, nySøknad, søknadsdato)
                    return OpprettTestSakResultat(eksisterendeSak, ventPåNyBehandling = true)
                }
            }
            return OpprettTestSakResultat(eksisterendeSak, ventPåNyBehandling = false)
        }

        val sak = personOgSakService.finnEllerOpprett(ident, søknadsdato ?: LocalDate.now())
        val søknad = lagSøknad(erStudent, harYrkesskade, harMedlemskap, andreUtbetalinger)
        leggTilSøknadJobb(sak, søknad, søknadsdato)

        return OpprettTestSakResultat(sak, ventPåNyBehandling = false)
    }

    private fun lagSøknad(
        erStudent: Boolean,
        harYrkesskade: Boolean,
        harMedlemskap: Boolean,
        andreUtbetalinger: AndreUtbetalingerDto?,
    ) = SøknadV0(
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

    private fun leggTilSøknadJobb(
        sak: Sak,
        søknad: SøknadV0,
        søknadsdato: LocalDate?,
    ) {
        flytJobbRepository.leggTil(
            HendelseMottattHåndteringJobbUtfører.nyJobb(
                sakId = sak.id,
                dokumentReferanse = InnsendingReferanse(JournalpostId("${System.currentTimeMillis()}")),
                brevkategori = InnsendingType.SØKNAD,
                kanal = Kanal.DIGITAL,
                melding = søknad,
                mottattTidspunkt = søknadsdato?.atStartOfDay() ?: LocalDateTime.now(),
            )
        )
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

data class OpprettTestSakResultat(val sak: Sak, val ventPåNyBehandling: Boolean)