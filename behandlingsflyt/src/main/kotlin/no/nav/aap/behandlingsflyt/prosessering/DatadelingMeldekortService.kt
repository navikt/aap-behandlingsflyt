package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArbeidIPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
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
    ): List<DetaljertMeldekortDTO> {
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

        val kontraktObjekter = meldekortene.mapNotNull { meldekort ->
            val arbeidsperiode = arbeidsperiodeFraMeldekort(meldekort)
            val meldekortetsPeriode = finnMeldekortetsPeriode(arbeidsperiode, meldePeriodene)

            if (arbeidsperiode == null) {
                log.warn(
                    "Meldekort uten arbeidstimer ble ignorert. journalpostId=${meldekort.journalpostId.identifikator}, behandlingId=${behandlingId.id}"
                )
                null
            } else if (meldekortetsPeriode == null) {
                log.warn(
                    "Meldekort med arbeidstimer som ikke samsvarer med noen meldekortperiode for behandlingen ble ignorert. journalpostId=${meldekort.journalpostId.identifikator}, behandlingId=${behandlingId.id}, arbeidsperiode=$arbeidsperiode"
                )
                null
            } else {
                tilKontrakt(
                    meldekort, personIdent, sak.saksnummer, behandlingId, underveisGrunnlag, meldekortetsPeriode
                )
            }
        }
        return kontraktObjekter
    }

    internal fun tilKontrakt(
        meldekort: Meldekort,
        personIdent: Ident,
        saksnummer: Saksnummer,
        behandlingId: BehandlingId,
        underveisGrunnlag: UnderveisGrunnlag,
        meldeperiode: Periode,
    ): DetaljertMeldekortDTO {
        val meldekortetsUnderveisperiode = underveisperiodeOmBareEn(underveisGrunnlag, meldeperiode, behandlingId)
        val meldepliktStatus = meldekortetsUnderveisperiode?.meldepliktStatus
        val rettighetsType = meldekortetsUnderveisperiode?.rettighetsType
        val avslagsårsak = meldekortetsUnderveisperiode?.avslagsårsak

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
            meldepliktStatusKode = meldepliktStatus?.name,
            rettighetsTypeKode = rettighetsType?.name,
            avslagsårsakKode = avslagsårsak?.name
        )
    }

    private fun underveisperiodeOmBareEn(
        underveisGrunnlag: UnderveisGrunnlag, meldeperiode: Periode, behandlingId: BehandlingId
    ): Underveisperiode? {
        val underveisPerioderEksakt = underveisGrunnlag.perioder.filter {
            it.meldePeriode == meldeperiode
        }
        if (underveisPerioderEksakt.size == 1) {
            return underveisPerioderEksakt.first()
        } else if (underveisPerioderEksakt.isEmpty()) {
            log.warn(
                "Ingen underveisperiode som matcher meldeperiode=$meldeperiode funnet på behandlingId=${behandlingId}. "
            )
            return null
        } else {
            log.info(
                "Flere underveisperioder funnet for samme meldeperiode på behandlingId=${behandlingId}. " +
                        "Kan da ikke entydig angi statuser for hele meldeperioden under ett."
            )
            return null
        }

    }

    private fun finnMeldekortetsPeriode(
        arbeidsperiode: Periode?, meldeperioder: List<Periode>
    ): Periode? {
        return arbeidsperiode?.let {
            meldeperioder.firstOrNull {
                // Alle arbeidsperiodene i meldekortet må være innenfor en og samme meldekortperiode.
                // MeldekortPerioder overlapper ikke med hverandre.
                it.inneholder(arbeidsperiode)
            }
        }
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
