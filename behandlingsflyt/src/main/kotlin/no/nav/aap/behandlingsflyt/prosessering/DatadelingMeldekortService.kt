package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArbeidIPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.type.Periode

class DatadelingMeldekortService(
    private val saksRepository: SakRepository,
    private val underveisRepository: UnderveisRepository,
    private val meldekortRepository: MeldekortRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository,
) {

    internal fun opprettKontraktObjekter(
        sakId: SakId,
        behandlingId: BehandlingId
    ): List<DetaljertMeldekortDTO> {
        val sak = saksRepository.hent(sakId)
        val personIdent = sak.person.aktivIdent()
        val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandlingId)

        if(underveisGrunnlag == null) {
            // hvis det ikke finnes UnderveisGrunnlag, finnes det heller ingen meldekort å rapportere
            return emptyList()
        }

        // hvis Behandlingen ikke er aktiv, returneres tom liste
        val meldekortene = meldekortRepository.hentHvisEksisterer(behandlingId)?.meldekort().orEmpty()

        val kontraktObjekter = meldekortene.map { meldekort ->
            tilKontrakt(meldekort, personIdent, sak, behandlingId, underveisGrunnlag)
        }
        return kontraktObjekter
    }

    internal fun tilKontrakt(
        meldekort: Meldekort,
        personIdent: Ident,
        sak: Sak,
        behandlingId: BehandlingId,
        underveisGrunnlag: UnderveisGrunnlag
    ): DetaljertMeldekortDTO {
        val meldekortPerioder = meldeperiodeRepository.hent(behandlingId)
        val meldekortetsPeriode = finnMeldekortPeriode(meldekort, meldekortPerioder)

        val meldekortetsUnderveisperiode = underveisperiodeOmBareEn(underveisGrunnlag, meldekortetsPeriode)
        val meldepliktStatus = meldekortetsUnderveisperiode?.meldepliktStatus
        val rettighetsType = meldekortetsUnderveisperiode?.rettighetsType
        val avslagsårsak = meldekortetsUnderveisperiode?.avslagsårsak

        return DetaljertMeldekortDTO(
            personIdent = personIdent.identifikator,
            saksnummer = sak.saksnummer,
            behandlingId = behandlingId.id,
            journalpostId = meldekort.journalpostId.identifikator,
            meldeperiodeFom = meldekortetsPeriode.fom,
            meldeperiodeTom = meldekortetsPeriode.tom,
            mottattTidspunkt = meldekort.mottattTidspunkt,
            timerArbeidPerPeriode = meldekort.timerArbeidPerPeriode.map {
                ArbeidIPeriodeDTO(
                    it.periode.fom,
                    it.periode.tom,
                    it.timerArbeid.antallTimer
                )
            },
            meldepliktStatusKode = meldepliktStatus?.name,
            rettighetsTypeKode = rettighetsType?.name,
            avslagsårsakKode = avslagsårsak?.name
        )
    }

    private fun underveisperiodeOmBareEn(
        underveisGrunnlag: UnderveisGrunnlag,
        meldekortetsPeriode: Periode
    ): Underveisperiode? {
        // TODO: det kan være flere underveisperioder som overlapper med meldekortets periode,
        // vi støtter her bare at det er en. Hva gjør vi hvis det er flere?
        // hva om det ikke finnest noen?
        val underveisPerioder = underveisGrunnlag.perioder.filter {
            it.meldePeriode.overlapp(meldekortetsPeriode) != null
        }
        val meldekortetsUnderveisperiode = if (underveisPerioder.size == 1) {
            underveisPerioder.first()
        } else {
            null
        }
        return meldekortetsUnderveisperiode
    }

    private fun finnMeldekortPeriode(
        meldekort: Meldekort, meldeperioder: List<Periode>
    ): Periode {
        val arbeidsperiode = arbeidsperiodeFraMeldekort(meldekort)
        return meldeperioder.first {
            // Alle arbeidsperiodene i meldekortet må være innenfor en og samme meldekortperiode.
            // MeldekortPerioder overlapper ikke med hverandre.
            it.inneholder(arbeidsperiode)
        }
    }

    /**
    @return Tidsperioden meldekortet inneholder arbeidstimer for.
    Merk at det kan være dager i perioden meldekortet gjelder for som det ikke er rapportert timer på.
    @param meldekort -
     **/
    private fun arbeidsperiodeFraMeldekort(meldekort: Meldekort): Periode {
        val arbeidPerioder = meldekort.timerArbeidPerPeriode.map { it.periode }
        val arbeidsPerioderStart = arbeidPerioder.minBy { it.fom }.fom
        val arbeidsPerioderSlutt = arbeidPerioder.maxBy { it.tom }.tom

        return Periode(arbeidsPerioderStart, arbeidsPerioderSlutt)
    }
}
