package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArbeidIPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArbeidsgraderingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
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
                    dokument.opprettetTid,
                    requireNotNull(m) { "Meldekort mangler strukturert data. JournalpostId=${dokument.referanse.asJournalpostId}" })
            }.ifEmpty { return null }

        val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandlingId)
        val underveistidslinje = underveisGrunnlag?.somTidslinje().orEmpty()

        val helePerioden =
            if (underveistidslinje.isNotEmpty()) underveistidslinje.helePerioden() else sak.rettighetsperiodeEttÅrFraStartDato()
        val meldePeriodene = meldeperiodeRepository.hentMeldeperioder(behandlingId, helePerioden)

        val arbeidsgradering = underveistidslinje
            .orEmpty().map {
                Arbeidsgradering(
                    benyttetGrenseverdi = it.grenseverdi.prosentverdi(),
                    gradering = it.arbeidsgradering.gradering.prosentverdi(),
                    fastsattArbeidsevne = it.arbeidsgradering.fastsattArbeidsevne.prosentverdi(),
                    arbeidetOverGrenseverdi = it.arbeidsgradering.andelArbeid.prosentverdi() > it.grenseverdi.prosentverdi(),
                    meldekortForSent = it.meldepliktStatus == MeldepliktStatus.IKKE_MELDT_SEG
                )
            }

        return if (meldePeriodene.isNotEmpty()) {
            filtrerOgMapMeldekort(
                meldekortene,
                meldePeriodene,
                personIdent,
                sak,
                behandlingId,
                arbeidsgradering,
                underveistidslinje
            )
        } else {
            log.warn("Ingen meldeperioder funnet for behandlingId=${behandlingId.id}")
            null
        }
    }

    private fun filtrerOgMapMeldekort(
        meldekortene: List<Meldekort>,
        meldePeriodene: List<Periode>,
        personIdent: Ident,
        sak: Sak,
        behandlingId: BehandlingId,
        arbeidsgradering: Tidslinje<Arbeidsgradering>,
        underveistidslinje: Tidslinje<Underveisperiode>
    ): List<DetaljertMeldekortDTO> {
        return meldekortene.mapNotNull { meldekort ->
            val arbeidsperiode = meldekort.arbeidsperiode()
            val meldekortetsPeriode = finnMeldekortetsPeriode(meldekort, meldePeriodene)

            when {
                meldekortetsPeriode == null -> {
                    log.warn(
                        "Meldekort med arbeidstimer som ikke samsvarer med noen meldekortperiode for behandlingen ble ignorert. journalpostId=${meldekort.journalpostId.identifikator}, behandlingId=${behandlingId.id}, arbeidsperiode=$arbeidsperiode"
                    )
                    null
                }
                // TODO: hvordan dele dette fornuftig!?!?

                // hvis ikke levert, hvordan identifisere meldeperioder

                // og hva med arbeidsperioder, de kommer som en og en dag. blir rotete??

                else -> tilKontrakt(
                    meldekort = meldekort,
                    personIdent = personIdent,
                    saksnummer = sak.saksnummer,
                    behandlingId = behandlingId,
                    meldeperiode = meldekortetsPeriode,
                    arbeidsgradering = arbeidsgradering,
                    underveistidslinje.begrensetTil(meldekortetsPeriode).komprimer()
                        .map { it.meldepliktStatus == MeldepliktStatus.IKKE_MELDT_SEG }.segmenter().any { it.verdi }
                )
            }
        }
    }

    private data class Arbeidsgradering(
        val benyttetGrenseverdi: Int,
        val gradering: Int,
        val fastsattArbeidsevne: Int,
        val arbeidetOverGrenseverdi: Boolean,
        val meldekortForSent: Boolean,
    )

    private fun tilKontrakt(
        meldekort: Meldekort,
        personIdent: Ident,
        saksnummer: Saksnummer,
        behandlingId: BehandlingId,
        meldeperiode: Periode,
        arbeidsgradering: Tidslinje<Arbeidsgradering>,
        forSentMeldekort: Boolean,
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
            forsentMeldekort = forSentMeldekort,
            arbeidsgradering = arbeidsgradering.begrensetTil(Periode(meldeperiode.fom, meldeperiode.tom).flytt(0)).komprimer()
                .segmenter().map { (periode, arbeidsgradering) ->
                    ArbeidsgraderingDTO(
                        periodeFom = periode.fom,
                        periodeTom = periode.tom,
                        benyttetGrenseverdi = arbeidsgradering.benyttetGrenseverdi,
                        gradering = arbeidsgradering.gradering,
                        fastsattArbeidsevne = arbeidsgradering.fastsattArbeidsevne,
                        harArbeidetOverGrenseverdi = arbeidsgradering.arbeidetOverGrenseverdi
                    )
                }
        )
    }

    internal fun finnMeldekortetsPeriode(
        meldekort: Meldekort, meldeperioder: List<Periode>
    ): Periode? {
        val arbeidsperiode = meldekort.arbeidsperiode()

        val periodeViaArbeidsperiode =
            meldeperioder.firstOrNull { arbeidsperiode != null && it.overlapper(arbeidsperiode) }

        if (periodeViaArbeidsperiode != null) return periodeViaArbeidsperiode

        return meldeperioder.filterIndexed { index, _ ->
            meldeperioder[index + 1].inneholder(meldekort.mottattTidspunkt.toLocalDate())
        }.firstOrNull()
    }
}
