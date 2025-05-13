package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class RettighetsperiodeSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val sakService: SakService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) : BehandlingSteg {

    private val logger = LoggerFactory.getLogger(RettighetsperiodeSteg::class.java)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        logger.info("Utfører rettighetsperiodesteg for behandling=${kontekst.behandlingId}")

        when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                if (erRelevant(kontekst)) {
                    val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
                    if (erIkkeVurdertTidligereIBehandlingen(avklaringsbehovene)) {
                        avklaringsbehovene.avbrytÅpneAvklaringsbehov()
                        return FantAvklaringsbehov(Definisjon.VURDER_RETTIGHETSPERIODE)
                    } else {
                        oppdaterVilkårsresultatForNyPeriode(kontekst)
                    }
                }
            }

            VurderingType.IKKE_RELEVANT, VurderingType.MELDEKORT -> {
                // Ikke relevant
            }
        }

        return Fullført
    }

    private fun erIkkeVurdertTidligereIBehandlingen(
        avklaringsbehovene: Avklaringsbehovene
    ): Boolean {
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VURDER_RETTIGHETSPERIODE)
        return (avklaringsbehov == null || avklaringsbehov.erÅpent())
    }

    private fun erRelevant(kontekst: FlytKontekstMedPerioder): Boolean {
        return (ÅrsakTilBehandling.VURDER_RETTIGHETSPERIODE in kontekst.vurdering.årsakerTilBehandling)
    }

    private fun oppdaterVilkårsresultatForNyPeriode(kontekst: FlytKontekstMedPerioder) {
        // TODO: Hva hvis man innskrenker perioden - må innskrenke vilkår og underveis?
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val rettighetsperiode = sakService.hent(kontekst.sakId).rettighetsperiode

        Vilkårtype
            .entries
            .filter { it.obligatorisk }
            .forEach { vilkårstype ->
                vilkårsresultat
                    .leggTilHvisIkkeEksisterer(vilkårstype)
                    .leggTilIkkeVurdertPeriode(rettighetsperiode)
                    .fjernHvisUtenforRettighetsperiode(rettighetsperiode)

            }

        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val vilkårsresultatRepository =
                repositoryProvider.provide<VilkårsresultatRepository>()
            val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
            return RettighetsperiodeSteg(
                vilkårsresultatRepository,
                SakService(sakRepository),
                avklaringsbehovRepository
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_RETTIGHETSPERIODE
        }

        override fun toString(): String {
            return "FlytSteg(type:${type()})"
        }

    }
}
