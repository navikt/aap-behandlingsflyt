package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.Medlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderLovvalgSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val personopplysningRepository: PersonopplysningRepository,
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository,
    private val sakRepository: SakRepository
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val manuellVurdering = medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)?.manuellVurdering
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)

        /*
        // TODO: Henrik - fiks her
        when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> vurderVilkår(kontekst) // TODO: Stopp ved behov
            VurderingType.REVURDERING -> vurderVilkår(kontekst) // TODO: Stopp ved behov
            VurderingType.FORLENGELSE -> {
                // Forleng vilkåret
                val forlengensePeriode = requireNotNull(kontekst.vurdering.forlengensePerioder)
                val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET).forleng(
                    forlengensePeriode
                )
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            }

            VurderingType.IKKE_RELEVANT -> {
                // Do nothing
            }
        }*/
        if (kontekst.harNoeTilBehandling()) {
            val sak = sakRepository.hent(kontekst.sakId)
            val personopplysningGrunnlag = personopplysningRepository.hentHvisEksisterer(kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne personopplysninger")
            val medlemskapArbeidInntektGrunnlag = medlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)
            val oppgittUtenlandsOppholdGrunnlag = medlemskapArbeidInntektRepository.hentOppgittUtenlandsOppholdHvisEksisterer(kontekst.behandlingId)
            Medlemskapvilkåret(vilkårsresultat, sak.rettighetsperiode).vurder(
                MedlemskapLovvalgGrunnlag(medlemskapArbeidInntektGrunnlag, personopplysningGrunnlag, oppgittUtenlandsOppholdGrunnlag)
            )
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }

        val måOverføresTilAnnetLand = vilkårsresultatRepository.hent(kontekst.behandlingId).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder().any{it.avslagsårsak == Avslagsårsak.NORGE_IKKE_KOMPETENT_STAT}
        val alleVilkårOppfylt = vilkårsresultatRepository.hent(kontekst.behandlingId).finnVilkår(Vilkårtype.LOVVALG).vilkårsperioder().all{it.erOppfylt()}

        if (måOverføresTilAnnetLand) {
            return FantVentebehov(
                Ventebehov(
                    definisjon = Definisjon.VENTE_PÅ_UTENLANDSK_VIDEREFØRING_AVKLARING,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_UTENLANDSK_VIDEREFORING_AVKLARING
                )
            )
        }
        if (!alleVilkårOppfylt && manuellVurdering == null) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP)
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
            val personopplysningRepository = repositoryProvider.provide<PersonopplysningRepository>()
            val medlemskapArbeidInntektRepository = repositoryProvider.provide<MedlemskapArbeidInntektRepository>()
            val sakRepository = repositoryProvider.provide<SakRepository>()
            return VurderLovvalgSteg(vilkårsresultatRepository, personopplysningRepository, medlemskapArbeidInntektRepository, sakRepository)
        }

        override fun type(): StegType {
            return StegType.VURDER_LOVVALG
        }
    }
}
