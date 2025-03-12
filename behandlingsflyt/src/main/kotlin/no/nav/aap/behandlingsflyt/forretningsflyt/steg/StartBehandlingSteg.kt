package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class StartBehandlingSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sakService: SakService,
    private val samordningVurderingRepository: SamordningVurderingRepository,
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.behandlingType == TypeBehandling.Førstegangsbehandling) {
            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            val rettighetsperiode = sakService.hent(kontekst.sakId).rettighetsperiode
            Vilkårtype
                .entries
                .filter { it.obligatorisk }
                .forEach { vilkårstype ->
                    vilkårsresultat
                        .leggTilHvisIkkeEksisterer(vilkårstype)
                        .leggTilIkkeVurdertPeriode(rettighetsperiode)
                }

            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        if (kontekst.behandlingType == TypeBehandling.Revurdering) {
            if (kontekst.vurdering.årsakerTilBehandling.contains(ÅrsakTilBehandling.REVURDER_SAMORDNING)) {
                val ventTil = samordningVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)!!


                return FantVentebehov(
                    Ventebehov(
                        definisjon = Definisjon.SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT,
                        grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER,
                        frist = ventTil.maksDato!!
                    )
                )
            }
        }

        if (kontekst.behandlingType == TypeBehandling.Klage) {
            return FantVentebehov(
                Ventebehov(
                    definisjon = Definisjon.VENTE_PÅ_KLAGE_IMPLEMENTASJON,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_KLAGE_IMPLEMENTASJON
                )
            )
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val vilkårsresultatRepository =
                repositoryProvider.provide<VilkårsresultatRepository>()
            return StartBehandlingSteg(
                vilkårsresultatRepository,
                SakService(sakRepository),
                repositoryProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.START_BEHANDLING
        }
    }
}
