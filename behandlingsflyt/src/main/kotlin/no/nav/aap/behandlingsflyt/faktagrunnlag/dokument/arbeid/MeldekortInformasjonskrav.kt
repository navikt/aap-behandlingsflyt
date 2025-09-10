package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.MeldekortTilApiInternJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.FØRSTEGANGSBEHANDLING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.MELDEKORT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.REVURDERING
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository

class MeldekortInformasjonskrav private constructor(
    private val mottaDokumentService: MottaDokumentService,
    private val meldekortRepository: MeldekortRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val flytJobbRepository: FlytJobbRepository,

) : Informasjonskrav {
    override val navn = Companion.navn

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.MELDEKORT

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): MeldekortInformasjonskrav {
            return MeldekortInformasjonskrav(
                MottaDokumentService(repositoryProvider),
                repositoryProvider.provide<MeldekortRepository>(),
                TidligereVurderingerImpl(repositoryProvider),
                repositoryProvider.provide<FlytJobbRepository>(),
            )
        }
    }

    override fun erRelevant(kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?): Boolean {
        return kontekst.vurderingType in setOf(FØRSTEGANGSBEHANDLING, REVURDERING, MELDEKORT) &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val meldekortSomIkkeErBehandlet = mottaDokumentService.meldekortSomIkkeErBehandlet(kontekst.sakId)
        if (meldekortSomIkkeErBehandlet.isEmpty()) {
            return IKKE_ENDRET
        }

        val eksisterendeGrunnlag = meldekortRepository.hentHvisEksisterer(kontekst.behandlingId)
        val eksisterendeMeldekort = eksisterendeGrunnlag?.meldekortene.orEmpty()
        val allePlussNye = mutableSetOf<Meldekort>()
        allePlussNye.addAll(eksisterendeMeldekort)

        for (ubehandletMeldekort in meldekortSomIkkeErBehandlet) {
            val nyttMeldekort = Meldekort(
                journalpostId = ubehandletMeldekort.journalpostId,
                timerArbeidPerPeriode = ubehandletMeldekort.timerArbeidPerPeriode,
                mottattTidspunkt = ubehandletMeldekort.mottattTidspunkt
            )
            // TODO en og en er ineffektivt, lagre i batch heller?
            mottaDokumentService.markerSomBehandlet(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                referanse = InnsendingReferanse(ubehandletMeldekort.journalpostId)
            )
            // TODO prematur markering som behandlet, er jo ikke lagret enda? Hva om lagring feiler?
            // rulles alt tilbake isåfall? Hva er transaction boundary vi bruker her?

            allePlussNye.add(nyttMeldekort)
        }

        meldekortRepository.lagre(behandlingId = kontekst.behandlingId, meldekortene = allePlussNye)

        triggOppdateringAvMeldekortHosAndreApper(kontekst.sakId, kontekst.behandlingId)

        return ENDRET // Antar her at alle nye kort gir en endring vi må ta hensyn til
    }


    private fun triggOppdateringAvMeldekortHosAndreApper(sakId: SakId, behandlingId: BehandlingId) {
        // Sende nye meldekort til API-intern
        // TODO Alle meldekort er lagret OK? Ikke async lagring? ser ut som lagre-kallet blokkerer til db er ferdig
        // TODO trenger vi også å gi beskjed til API-intern om at kontekst.forrigeBehandlingId sine meldekort er utdaterte og skal slettes?
        // eller kan vi først slette alle meldekort for samme (saksnummer og meldeperiode) når vi mottar nye der? Tror det
        val sendOppdaterteMeldekortJobb = MeldekortTilApiInternJobbUtfører.nyJobb(sakId, behandlingId)
        flytJobbRepository.leggTil(sendOppdaterteMeldekortJobb)
    }

    override fun flettOpplysningerFraAtomærBehandling(kontekst: FlytKontekst): Informasjonskrav.Endret {
        val forrigeBehandlingId = kontekst.forrigeBehandlingId ?: return IKKE_ENDRET
        val forrigeBehandlingGrunnlag = meldekortRepository.hentHvisEksisterer(forrigeBehandlingId) ?: return IKKE_ENDRET

        val meldekortIBehandling = meldekortRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.meldekortene
            .orEmpty()

        val journalpostIderIBehandling = meldekortIBehandling.map { it.journalpostId }

        val nyeMeldekort = mutableListOf<Meldekort>()
        for (meldekort in forrigeBehandlingGrunnlag.meldekortene) {
            if (meldekort.journalpostId !in journalpostIderIBehandling) {
                nyeMeldekort.add(meldekort)
            }
        }

        if (nyeMeldekort.isEmpty()) {
            return IKKE_ENDRET
        } else {
            meldekortRepository.lagre(kontekst.behandlingId, meldekortIBehandling + nyeMeldekort)

            triggOppdateringAvMeldekortHosAndreApper(kontekst.sakId, kontekst.behandlingId)

            return ENDRET
        }
    }
}
