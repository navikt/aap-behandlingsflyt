package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArbeidIPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class DatadelingMeldekortService(
    private val saksRepository: SakRepository,
    private val underveisRepository: UnderveisRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, @Suppress("unused") gatewayProvider: GatewayProvider) : this(
        saksRepository = repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        mottattDokumentRepository = repositoryProvider.provide(),
        meldeperiodeRepository = repositoryProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun opprettKontraktObjekter(
        sakId: SakId, behandlingId: BehandlingId
    ): List<DetaljertMeldekortDTO> = hentOgKonverterMeldekort(sakId, behandlingId).orEmpty()

    private fun hentOgKonverterMeldekort(
        sakId: SakId, behandlingId: BehandlingId
    ): List<DetaljertMeldekortDTO>? {
        val sak = saksRepository.hent(sakId)
        val personIdent = sak.person.aktivIdent()

        val meldekortene = mottattDokumentRepository.hentDokumenterAvType(sakId, InnsendingType.MELDEKORT)
            .map { it to it.strukturerteData<no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort>()?.data }
            .map { (dokument, m) ->
                Meldekort.fraKontrakt(
                    dokument.referanse.asJournalpostId,
                    dokument.mottattTidspunkt,
                    requireNotNull(m) { "Meldekort mangler strukturert data. JournalpostId=${dokument.referanse.asJournalpostId}" })
            }.ifEmpty { return null }

        val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandlingId)
        val helePerioden = underveisGrunnlag?.somTidslinje()?.helePerioden() ?: sak.rettighetsperiodeEttÅrFraStartDato()
        val meldePeriodene = meldeperiodeRepository.hentMeldeperioder(behandlingId, helePerioden)

        return if (meldePeriodene.isNotEmpty()) {
            filtrerOgMapMeldekort(meldekortene, meldePeriodene, personIdent, sak, behandlingId)
        } else {
            log.warn("Ingen meldeperioder funnet for behandlingId=${behandlingId.id}")
            null
        }
    }

    private fun filtrerOgMapMeldekort(
        meldekortene: List<Meldekort>,
        meldePeriodene: List<Periode>,
        personIdent: Ident,
        sak: no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak,
        behandlingId: BehandlingId
    ): List<DetaljertMeldekortDTO> {
        return meldekortene.mapNotNull { meldekort ->
            val arbeidsperiode = meldekort.arbeidsperiode()
            val meldekortetsPeriode = arbeidsperiode?.let { finnMeldekortetsPeriode(arbeidsperiode, meldePeriodene) }

            when {
                arbeidsperiode == null -> {
                    log.warn(
                        "Meldekort uten arbeidstimer ble ignorert. journalpostId=${meldekort.journalpostId.identifikator}, behandlingId=${behandlingId.id}"
                    )
                    null
                }

                meldekortetsPeriode == null -> {
                    log.warn(
                        "Meldekort med arbeidstimer som ikke samsvarer med noen meldekortperiode for behandlingen ble ignorert. journalpostId=${meldekort.journalpostId.identifikator}, behandlingId=${behandlingId.id}, arbeidsperiode=$arbeidsperiode"
                    )
                    null
                }

                else -> tilKontrakt(
                    meldekort, personIdent, sak.saksnummer, behandlingId, meldekortetsPeriode
                )
            }
        }
    }

    internal fun tilKontrakt(
        meldekort: Meldekort,
        personIdent: Ident,
        saksnummer: Saksnummer,
        behandlingId: BehandlingId,
        meldeperiode: Periode,
    ): DetaljertMeldekortDTO {
        return DetaljertMeldekortDTO(
            personIdent = personIdent.identifikator,
            saksnummer = saksnummer,
            behandlingId = behandlingId.id,
            journalpostId = meldekort.journalpostId.identifikator,
            meldeperiodeFom = meldeperiode.fom,
            meldeperiodeTom = meldeperiode.tom,
            mottattTidspunkt = meldekort.mottattTidspunkt,
            timerArbeidPerPeriode = meldekort.timerArbeidPerPeriode.map {
                ArbeidIPeriodeDTO(
                    it.periode.fom, it.periode.tom, it.timerArbeid.antallTimer
                )
            },
        )
    }

    private fun finnMeldekortetsPeriode(
        arbeidsperiode: Periode, meldeperioder: List<Periode>
    ): Periode? {
        return meldeperioder.firstOrNull { it.overlapper(arbeidsperiode) }
    }
}
