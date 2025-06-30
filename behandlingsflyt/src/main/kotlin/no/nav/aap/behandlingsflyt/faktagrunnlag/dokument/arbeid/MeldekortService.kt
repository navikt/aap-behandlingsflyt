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
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.lookup.repository.RepositoryProvider

class MeldekortService private constructor(
    private val mottaDokumentService: MottaDokumentService,
    private val meldekortRepository: MeldekortRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav {
    override val navn = Companion.navn

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.MELDEKORT

        override fun konstruer(repositoryProvider: RepositoryProvider): MeldekortService {
            return MeldekortService(
                MottaDokumentService(repositoryProvider),
                repositoryProvider.provide<MeldekortRepository>(),
                TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }

    override fun erRelevant(kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val meldekortSomIkkeErBehandlet = mottaDokumentService.meldekortSomIkkeErBehandlet(kontekst.sakId)
        if (meldekortSomIkkeErBehandlet.isEmpty()) {
            return IKKE_ENDRET
        }

        val eksisterendeGrunnlag = meldekortRepository.hentHvisEksisterer(kontekst.behandlingId)
        val eksisterendeMeldekort = eksisterendeGrunnlag?.meldekortene ?: emptySet()
        val allePlussNye = HashSet<Meldekort>(eksisterendeMeldekort)

        for (ubehandletMeldekort in meldekortSomIkkeErBehandlet) {
            val nyttMeldekort = Meldekort(
                journalpostId = ubehandletMeldekort.journalpostId,
                timerArbeidPerPeriode = ubehandletMeldekort.timerArbeidPerPeriode,
                mottattTidspunkt = ubehandletMeldekort.mottattTidspunkt
            )
            mottaDokumentService.knyttTilBehandling(
                sakId = kontekst.sakId,
                behandlingId = kontekst.behandlingId,
                referanse = InnsendingReferanse(ubehandletMeldekort.journalpostId)
            )
            allePlussNye.add(nyttMeldekort)
        }

        meldekortRepository.lagre(behandlingId = kontekst.behandlingId, meldekortene = allePlussNye)

        return ENDRET // Antar her at alle nye kort gir en endring vi må ta hensyn til
    }

    override fun flettOpplysningerFraAtomærBehandling(kontekst: FlytKontekst): Informasjonskrav.Endret {
        val forrigeBehandlingId = kontekst.forrigeBehandlingId ?: return IKKE_ENDRET
        val forrigeBehandlingGrunnlag = meldekortRepository.hentHvisEksisterer(forrigeBehandlingId) ?: return IKKE_ENDRET

        val meldekortIBehandling = meldekortRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.meldekortene
            ?: emptySet()

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
            return ENDRET
        }
    }
}
