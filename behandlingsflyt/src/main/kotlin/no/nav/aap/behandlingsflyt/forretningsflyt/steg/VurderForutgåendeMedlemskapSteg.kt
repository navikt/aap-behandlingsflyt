package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.ForutgåendeMedlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderForutgåendeMedlemskapSteg private constructor(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
    private val forutgåendeMedlemskapArbeidInntektRepository: MedlemskapArbeidInntektForutgåendeRepository,
    private val sakRepository: SakRepository,
    private val personopplysningForutgåendeRepository: PersonopplysningForutgåendeRepository
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val yrkesskadeGrunnlag = yrkesskadeRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)
        if (yrkesskadeGrunnlag?.yrkesskader?.harYrkesskade() == true) return Fullført

        val manuellVurdering = forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)?.manuellVurdering

        if (kontekst.perioderTilVurdering.isNotEmpty()) {
            val sak = sakRepository.hent(kontekst.sakId)
            val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
            val personopplysningForutgåendeGrunnlag = personopplysningForutgåendeRepository.hentHvisEksisterer(kontekst.behandlingId)
                ?: throw IllegalStateException("Forventet å finne personopplysninger")

            val forutgåendeMedlemskapArbeidInntektGrunnlag = forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId)
            val oppgittUtenlandsOppholdGrunnlag = forutgåendeMedlemskapArbeidInntektRepository.hentOppgittUtenlandsOppholdHvisEksisterer(kontekst.behandlingId)

            ForutgåendeMedlemskapvilkåret(vilkårsresultat, sak.rettighetsperiode, manuellVurdering).vurder(
                ForutgåendeMedlemskapGrunnlag(forutgåendeMedlemskapArbeidInntektGrunnlag, personopplysningForutgåendeGrunnlag, oppgittUtenlandsOppholdGrunnlag)
            )
            vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
        }
        // TODO: Revurdering må inn her

        val alleVilkårOppfylt = vilkårsresultatRepository.hent(kontekst.behandlingId).finnVilkår(Vilkårtype.MEDLEMSKAP).vilkårsperioder().all{it.erOppfylt()}
        if (!alleVilkårOppfylt && manuellVurdering == null) {
            return FantAvklaringsbehov(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
            val yrkesskadeRepository = repositoryProvider.provide<YrkesskadeRepository>()
            val forutgåendeRepository = repositoryProvider.provide<MedlemskapArbeidInntektForutgåendeRepository>()
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val personopplysningForutgåendeRepository = repositoryProvider.provide<PersonopplysningForutgåendeRepository>()
            return VurderForutgåendeMedlemskapSteg(
                vilkårsresultatRepository,
                yrkesskadeRepository,
                forutgåendeRepository,
                sakRepository,
                personopplysningForutgåendeRepository
            )
        }

        override fun type(): StegType {
            return StegType.VURDER_MEDLEMSKAP
        }
    }
}
