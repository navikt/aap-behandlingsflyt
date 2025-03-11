package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.ForutgåendeMedlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningForutgåendeRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderForutgåendeMedlemskapSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val forutgåendeMedlemskapArbeidInntektRepository: MedlemskapArbeidInntektForutgåendeRepository,
    private val personopplysningForutgåendeRepository: PersonopplysningForutgåendeRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avslag = harTidligereAvslag(kontekst.behandlingId)
        if (avslag) {
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
            val medlemskapBehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
            if (medlemskapBehov != null && medlemskapBehov.erÅpent()) {
                avklaringsbehovene.avbryt(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
            }

            return Fullført
        }

        when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                val vilkårsVurdering = vurderVilkår(kontekst)
                if (vilkårsVurdering != null ) return vilkårsVurdering
            }

            VurderingType.REVURDERING -> {
                val vilkårsVurdering = vurderVilkår(kontekst)
                if (vilkårsVurdering != null) return vilkårsVurdering
            }

            VurderingType.FORLENGELSE -> {
                val forlengensePeriode = requireNotNull(kontekst.vurdering.forlengelsePeriode)
                val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
                vilkårsresultat.finnVilkår(Vilkårtype.MEDLEMSKAP).forleng(
                    forlengensePeriode
                )
                vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            }

            VurderingType.IKKE_RELEVANT -> {
                // Do nothing
            }
        }

        return Fullført
    }

    private fun vurderVilkår(kontekst: FlytKontekstMedPerioder): StegResultat? {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        val manuellVurdering = forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)?.manuellVurdering

        if (vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET).vilkårsperioder().any { it.innvilgelsesårsak == Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG }){
            ForutgåendeMedlemskapvilkåret(vilkårsresultat, kontekst.vurdering.rettighetsperiode).leggTilYrkesskadeVurdering()
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
            return Fullført
        }

        if (kontekst.vurdering.skalVurdereNoe()) {
            val personopplysningForutgåendeGrunnlag = personopplysningForutgåendeRepository.hentHvisEksisterer(kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne personopplysninger")

            val forutgåendeMedlemskapArbeidInntektGrunnlag = forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)
            val oppgittUtenlandsOppholdGrunnlag = forutgåendeMedlemskapArbeidInntektRepository.hentOppgittUtenlandsOppholdHvisEksisterer(kontekst.behandlingId)

            ForutgåendeMedlemskapvilkåret(vilkårsresultat, kontekst.vurdering.rettighetsperiode).vurder(
                ForutgåendeMedlemskapGrunnlag(forutgåendeMedlemskapArbeidInntektGrunnlag, personopplysningForutgåendeGrunnlag, oppgittUtenlandsOppholdGrunnlag)
            )
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }
        val alleVilkårOppfylt = vilkårsresultatRepository.hent(kontekst.behandlingId).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder().all{it.erOppfylt()}

        if ((!alleVilkårOppfylt && manuellVurdering == null)
            || spesifiktTriggetRevurderMedlemskapUtenManuellVurdering(kontekst, manuellVurdering)) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
        }
        return null
    }

    private fun spesifiktTriggetRevurderMedlemskapUtenManuellVurdering(kontekst: FlytKontekstMedPerioder, manuellVurdering: ManuellVurderingForForutgåendeMedlemskap?): Boolean {
        val erSpesifiktTriggetRevurderMedlemskap = kontekst.vurdering.årsakerTilBehandling.any{it == ÅrsakTilBehandling.REVURDER_MEDLEMSKAP}
        return erSpesifiktTriggetRevurderMedlemskap && manuellVurdering == null
    }

    private fun harTidligereAvslag(behandlingId: BehandlingId): Boolean {
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)

        val lovvalgvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.LOVVALG)
        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        val bistandsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
        val bistandsvilkåretEllerSykepengerErstatningHvisIkke = if (!bistandsvilkåret.harPerioderSomErOppfylt()) {
            vilkårsresultat.optionalVilkår(Vilkårtype.SYKEPENGEERSTATNING)?.harPerioderSomErOppfylt() == true
        } else {
            bistandsvilkåret.harPerioderSomErOppfylt()
        }

        return !sykdomsvilkåret.harPerioderSomErOppfylt()
            && !bistandsvilkåretEllerSykepengerErstatningHvisIkke
            && !lovvalgvilkåret.harPerioderSomErOppfylt()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
            val forutgåendeRepository = repositoryProvider.provide<MedlemskapArbeidInntektForutgåendeRepository>()
            val personopplysningForutgåendeRepository = repositoryProvider.provide<PersonopplysningForutgåendeRepository>()
            val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
            return VurderForutgåendeMedlemskapSteg(
                vilkårsresultatRepository,
                forutgåendeRepository,
                personopplysningForutgåendeRepository,
                avklaringsbehovRepository
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_MEDLEMSKAP
        }
    }
}
