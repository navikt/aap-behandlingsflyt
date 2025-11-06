package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.IngenInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.IngenRegisterData
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.FØRSTEGANGSBEHANDLING
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.MELDEKORT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType.REVURDERING
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class MeldekortInformasjonskrav private constructor(
    private val mottaDokumentService: MottaDokumentService,
    private val meldekortRepository: MeldekortRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav<IngenInput, IngenRegisterData> {
    override val navn = Companion.navn

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.MELDEKORT

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): MeldekortInformasjonskrav {
            return MeldekortInformasjonskrav(
                MottaDokumentService(repositoryProvider),
                repositoryProvider.provide<MeldekortRepository>(),
                TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.vurderingType in setOf(FØRSTEGANGSBEHANDLING, REVURDERING, MELDEKORT) &&
                !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun klargjør(kontekst: FlytKontekstMedPerioder) = IngenInput

    override fun hentData(input: IngenInput) = IngenRegisterData

    override fun oppdater(
        input: IngenInput,
        registerdata: IngenRegisterData,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
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
            mottaDokumentService.markerSomBehandlet(
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
        val forrigeMeldekortGrunnlag =
            meldekortRepository.hentHvisEksisterer(forrigeBehandlingId) ?: return IKKE_ENDRET

        val nyeMeldekort = meldekortRepository.hentHvisEksisterer(kontekst.behandlingId)
            ?.meldekortene
            .orEmpty()

        val journalpostIderFraNyeMeldekort = nyeMeldekort.map { it.journalpostId }
        val meldekortBareIForrigeGrunnlag = mutableSetOf<Meldekort>()
        for (meldekort in forrigeMeldekortGrunnlag.meldekortene) {
            if (meldekort.journalpostId !in journalpostIderFraNyeMeldekort) {
                meldekortBareIForrigeGrunnlag.add(meldekort)
            }
        }

        if (meldekortBareIForrigeGrunnlag.isEmpty()) {
            return IKKE_ENDRET
        } else {
            // Lagrer et nytt grunnlag med alle meldekortene på ny behandlingId
            meldekortRepository.lagre(kontekst.behandlingId, nyeMeldekort + meldekortBareIForrigeGrunnlag)
            return ENDRET
        }
    }
}
