package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class AktivitetspliktInformasjonskrav(
    private val tidligereVurderinger: TidligereVurderinger,
    private val behandlingRepository: BehandlingRepository,
    private val aktivitetsplikt11_7Repository: Aktivitetsplikt11_7Repository
) : Informasjonskrav {
    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.AKTIVITETSPLIKT

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): AktivitetspliktInformasjonskrav {
            return AktivitetspliktInformasjonskrav(
                TidligereVurderingerImpl(repositoryProvider),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
            )
        }
    }

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.vurderingType in listOf(VurderingType.REVURDERING, VurderingType.AKTIVITETSPLIKT) &&
                !tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        if (kontekst.vurderingType == VurderingType.AKTIVITETSPLIKT) {
            val nyesteIverksatteAktivitetspliktBehandling =
                behandlingRepository
                    .hentAlleFor(kontekst.sakId, listOf(TypeBehandling.Aktivitetsplikt))
                    .filter { it.status().erAvsluttet() }
                    .maxByOrNull { it.opprettetTidspunkt }
            requireNotNull(nyesteIverksatteAktivitetspliktBehandling) {
                "Fant ingen iverksatte aktivitetspliktbehandlinger for sak ${kontekst.sakId}, men vurderingstype er ${VurderingType.AKTIVITETSPLIKT}"
            }
            aktivitetsplikt11_7Repository.kopier(nyesteIverksatteAktivitetspliktBehandling.id, kontekst.behandlingId)
            return IKKE_ENDRET
        }
        return IKKE_ENDRET
    }

    override fun flettOpplysningerFraAtomærBehandling(kontekst: FlytKontekst): Informasjonskrav.Endret {
        val grunnlag = aktivitetsplikt11_7Repository.hentHvisEksisterer(kontekst.behandlingId)
        val forrigeGrunnlag =
            kontekst.forrigeBehandlingId.let { aktivitetsplikt11_7Repository.hentHvisEksisterer(it!!) }
        if (grunnlag != forrigeGrunnlag) {
            aktivitetsplikt11_7Repository.kopier(kontekst.forrigeBehandlingId!!, kontekst.behandlingId)
            return ENDRET
        }
        return IKKE_ENDRET
    }
}