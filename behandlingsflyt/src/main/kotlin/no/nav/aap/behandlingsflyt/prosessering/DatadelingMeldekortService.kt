package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertTimeArbeidetListeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.MeldtArbeidIPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.type.Periode

class DatadelingMeldekortService(
    private val saksRepository: SakRepository,
    private val underveisRepository: UnderveisRepository,
    private val meldekortRepository: MeldekortRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository,
) {
    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

    internal fun opprettKontraktObjekter(
        sakId: SakId, behandlingId: BehandlingId
    ): List<DetaljertTimeArbeidetListeDTO> {
        val sak = saksRepository.hent(sakId)
        val personIdent = sak.person.aktivIdent()

        val meldekortene = meldekortRepository.hentHvisEksisterer(behandlingId)?.meldekort().orEmpty()
        if (meldekortene.isEmpty()) {
            return emptyList()
        }

        val meldePeriodene = meldeperiodeRepository.hent(behandlingId)
        if (meldePeriodene.isEmpty()) {
            log.warn("Ingen meldeperioder funnet for behandlingId=${behandlingId.id}")
            return emptyList()
        }

        val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandlingId)
        if (underveisGrunnlag == null) {
            log.warn("Ingen UnderveisGrunnlag funnet for behandlingId=${behandlingId.id}")
            // hvis det ikke finnes UnderveisGrunnlag, finnes det heller ingen meldekort å rapportere
            return emptyList()
        }

        val arbeidIPeriodeTilMeldekort = meldekortene.mapNotNull { meldekort ->
            val arbeidsperiode = arbeidsperiodeFraMeldekort(meldekort)
            // et meldekort kan noen ganger ha data for flere meldeperioder
            val meldekortetsPerioder = finnMeldekortetsPerioder(arbeidsperiode, meldePeriodene)

            if (arbeidsperiode == null) {
                log.warn(
                    "Meldekort uten arbeidstimer ble ignorert. journalpostId=${meldekort.journalpostId.identifikator}, behandlingId=${behandlingId.id}"
                )
                null
            } else if (meldekortetsPerioder.isEmpty()) {
                log.warn(
                    "Meldekort med arbeidstimer som ikke samsvarer med noen meldeperiode for behandlingen ble ignorert. journalpostId=${meldekort.journalpostId.identifikator}, behandlingId=${behandlingId.id}, arbeidsperiode=$arbeidsperiode"
                )
                null
            } else {
                meldekort
            }
        }
            // Flater ut til par (ArbeidIPeriode -> Meldekort)
            .flatMap { meldekort -> meldekort.timerArbeidPerPeriode.map { it to meldekort } }
            // Hvis samme ArbeidIPeriode finnes i flere meldekort vil siste vinne.
            .toMap()

        val meldeperiodeTilArbeidIPeriode = meldePeriodene.associateWith { periode ->
                arbeidIPeriodeTilMeldekort.keys.filter {
                    it.periode.overlapper(
                        periode
                    )
                }
            }.filterValues { it.isNotEmpty() }

        return meldeperiodeTilArbeidIPeriode.map { (meldeperiode, arbeidstimerListe) ->
            val arbeidsTimerListeMedKilde =
                arbeidIPeriodeTilMeldekort.filter { (key, _) -> arbeidstimerListe.contains(key) }
            tilKontrakt(
                meldeperiode, arbeidstimerListe, personIdent, sak.saksnummer, behandlingId, underveisGrunnlag,
                arbeidsTimerListeMedKilde
            )
        }

    }

    internal fun tilKontrakt(
        meldeperiode: Periode,
        arbeidIPerioden: List<ArbeidIPeriode>,
        personIdent: Ident,
        saksnummer: Saksnummer,
        behandlingId: BehandlingId,
        underveisGrunnlag: UnderveisGrunnlag,
        arbeidIPeriodeTilMeldekort: Map<ArbeidIPeriode, Meldekort>
    ): DetaljertTimeArbeidetListeDTO {
        val underVeisPeriode = underveisGrunnlag.perioder.filter {
            it.meldePeriode == meldeperiode
        }.maxByOrNull { it.id!!.asLong } // FIXME antar her at en underveisperiode representerer status for slutten av meldeperioden

        return DetaljertTimeArbeidetListeDTO(
            personIdent = personIdent.identifikator,
            saksnummer = saksnummer,
            behandlingId = behandlingId.id,
            meldeperiodeFom = meldeperiode.fom,
            meldeperiodeTom = meldeperiode.tom,
            detaljertArbeidIPeriode = arbeidIPerioden.map {
                val opplysningskilde = arbeidIPeriodeTilMeldekort.getValue(it)
                MeldtArbeidIPeriodeDTO(
                    it.periode.fom,
                    it.periode.tom,
                    it.timerArbeid.antallTimer,
                    opplysningskilde.mottattTidspunkt,
                    opplysningskilde.journalpostId.identifikator
                )
            },
            meldepliktStatusKode = underVeisPeriode?.meldepliktStatus?.name,
            rettighetsTypeKode = underVeisPeriode?.rettighetsType?.name,
            avslagsårsakKode = underVeisPeriode?.avslagsårsak?.name,
        )

    }

    private fun finnMeldekortetsPerioder(
        arbeidsperiode: Periode?, meldeperioder: List<Periode>
    ): List<Periode> {
        return arbeidsperiode?.let {
            meldeperioder.filter {
                // arbeidsperiode overlapper med meldeperiode
                it.overlapper(arbeidsperiode)
            }
        } ?: emptyList()
    }

    /**
    @return Tidsperioden meldekortet inneholder arbeidstimer for.
    Merk at det kan være dager i perioden meldekortet gjelder for som det ikke er rapportert timer på.
    @param meldekort -
     **/
    private fun arbeidsperiodeFraMeldekort(meldekort: Meldekort): Periode? {
        val timerArbeidPerPeriode = meldekort.timerArbeidPerPeriode
        if (timerArbeidPerPeriode.isEmpty()) {
            return null
        }
        val arbeidPerioder = timerArbeidPerPeriode.map { it.periode }
        val arbeidsPerioderStart = arbeidPerioder.minBy { it.fom }.fom
        val arbeidsPerioderSlutt = arbeidPerioder.maxBy { it.tom }.tom

        return Periode(arbeidsPerioderStart, arbeidsPerioderSlutt)
    }
}
