package no.nav.aap.behandlingsflyt.help

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeApiInternGateway
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemoryBehandlingService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import java.time.LocalDate

fun opprettSak(connection: DBConnection, søknadsdato: LocalDate): Sak {
    return opprettSak(connection, ident(), søknadsdato)
}

fun opprettSak(connection: DBConnection, ident: Ident, søknadsdato: LocalDate): Sak {
    return PersonOgSakService(
        FakePdlGateway,
        FakeApiInternGateway.konstruer(),
        PersonRepositoryImpl(connection),
        SakRepositoryImpl(connection)
    ).finnEllerOpprett(ident, søknadsdato)
}

fun opprettInMemorySak(søknadsdato: LocalDate = LocalDate.now(), ident: Ident = ident()): Sak {
    return PersonOgSakService(
        FakePdlGateway,
        FakeApiInternGateway.konstruer(),
        InMemoryPersonRepository,
        InMemorySakRepository
    ).finnEllerOpprett(ident, søknadsdato)
}

fun opprettInMemorySakOgBehandling(
    søknadsdato: LocalDate = LocalDate.now(),
    vurderingsbehov: List<Vurderingsbehov> = listOf(Vurderingsbehov.MOTTATT_SØKNAD),
    årsakTilOpprettelse: ÅrsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
    ident: Ident = ident(),
): Pair<Sak, Behandling> {
    val vurderingsbehovMedPeriode = vurderingsbehov.map { VurderingsbehovMedPeriode(it) }
    val sak = opprettInMemorySak(søknadsdato, ident)
    val behandling = InMemoryBehandlingService.finnEllerOpprettOrdinærBehandling(
        sak.id,
        VurderingsbehovOgÅrsak(vurderingsbehovMedPeriode, årsakTilOpprettelse)
    )
    return sak to behandling
}

fun opprettInMemorySakOgRevurdering(
    søknadsdato: LocalDate = LocalDate.now(),
    vurderingsbehov: List<Vurderingsbehov> = listOf(Vurderingsbehov.MOTTATT_SØKNAD),
    årsakTilOpprettelse: ÅrsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
    ident: Ident = ident(),
): Triple<Sak, Behandling, Behandling> {
    val (sak, førstegangsbehandling) = opprettInMemorySakOgBehandling(
        søknadsdato = søknadsdato,
        ident = ident,
    )

    InMemoryBehandlingRepository.leggTilNyttAktivtSteg(
        førstegangsbehandling.id, StegTilstand(
            stegStatus = StegStatus.AVSLUTTER,
            stegType = Førstegangsbehandling.flyt().stegene().last(),
        )
    )

    InMemoryBehandlingService.lukkBehandling(førstegangsbehandling.id)

    /* Legg på en vurdering av aldersvilkåret like lang som rettighetsperioden, siden vi har
     * noe kode som utleder rettighetsperiode for gamle behandlinger ved å se på aldersvilkåret.
     */
    val vilkårsresultat = InMemoryVilkårsresultatRepository.hent(førstegangsbehandling.id)
    vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.ALDERSVILKÅRET)
        .leggTilVurderinger(
            tidslinjeOf(
                sak.rettighetsperiode to Vilkårsvurdering(
                    utfall = Utfall.IKKE_VURDERT,
                    manuellVurdering = true,
                    begrunnelse = "automatisk vurdering for å simulere rettighetsperiode",
                    faktagrunnlag = null,
                )
            )
        )
    InMemoryVilkårsresultatRepository.lagre(førstegangsbehandling.id, vilkårsresultat)

    val vurderingsbehovMedPeriode = vurderingsbehov.map { VurderingsbehovMedPeriode(it) }
    val revurdering = InMemoryBehandlingService.finnEllerOpprettOrdinærBehandling(
        sak.id,
        VurderingsbehovOgÅrsak(vurderingsbehovMedPeriode, årsakTilOpprettelse)
    )

    check(førstegangsbehandling.id != revurdering.id)
    return Triple(sak, InMemoryBehandlingRepository.hent(førstegangsbehandling.id), revurdering)
}
