package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOverstyrtForutgåendeMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.VurderingsResultat
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.ForutgåendeMedlemskapvilkåret
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningForutgåendeRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarOverstyrtForutgåendeMedlemskapLøser(connection: DBConnection): AvklaringsbehovsLøser<AvklarOverstyrtForutgåendeMedlemskapLøsning> {
    private val repositoryProvider = RepositoryProvider(connection)
    private val forutgåendeMedlemskapArbeidInntektRepository = repositoryProvider.provide<MedlemskapArbeidInntektForutgåendeRepository>()
    private val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
    private val sakRepository = repositoryProvider.provide<SakRepository>()
    private val personopplysningForutgåendeRepository = repositoryProvider.provide<PersonopplysningForutgåendeRepository>()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarOverstyrtForutgåendeMedlemskapLøsning): LøsningsResultat {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId())
        if (vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET).vilkårsperioder().any { it.innvilgelsesårsak == Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG }){
            return LøsningsResultat("OVERSTYRT: Fant yrkesskade, overstyring ikke tillat.")
        }

        forutgåendeMedlemskapArbeidInntektRepository.lagreManuellVurdering(kontekst.behandlingId(),
            ManuellVurderingForForutgåendeMedlemskap(
                løsning.manuellVurderingForForutgåendeMedlemskap.begrunnelse,
                løsning.manuellVurderingForForutgåendeMedlemskap.harForutgåendeMedlemskap,
                løsning.manuellVurderingForForutgåendeMedlemskap.varMedlemMedNedsattArbeidsevne,
                løsning.manuellVurderingForForutgåendeMedlemskap.medlemMedUnntakAvMaksFemAar,
                true
            )
        )
        val overstyrtManuellVurdering = forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId())?.manuellVurdering

        val sak = sakRepository.hent(kontekst.kontekst.sakId)
        val personopplysningGrunnlag = personopplysningForutgåendeRepository.hentHvisEksisterer(kontekst.behandlingId())
            ?: throw IllegalStateException("Forventet å finne personopplysninger")
        val medlemskapArbeidInntektGrunnlag = forutgåendeMedlemskapArbeidInntektRepository.hentHvisEksisterer(kontekst.behandlingId())
        val oppgittUtenlandsOppholdGrunnlag = forutgåendeMedlemskapArbeidInntektRepository.hentOppgittUtenlandsOppholdHvisEksisterer(kontekst.behandlingId())

        ForutgåendeMedlemskapvilkåret(vilkårsresultat, sak.rettighetsperiode, overstyrtManuellVurdering).vurderOverstyrt(
            ForutgåendeMedlemskapGrunnlag(medlemskapArbeidInntektGrunnlag, personopplysningGrunnlag, oppgittUtenlandsOppholdGrunnlag)
        )
        vilkårsresultatRepository.lagre(kontekst.behandlingId(), vilkårsresultat)
        return LøsningsResultat("OVERSTYRT: Vurdert forutgående medlemskap manuelt.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.MANUELL_OVERSTYRING_MEDLEMSKAP
    }
}