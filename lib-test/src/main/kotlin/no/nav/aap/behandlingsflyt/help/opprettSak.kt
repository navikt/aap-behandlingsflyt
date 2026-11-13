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
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryArenaMigreringRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryPersonRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStønadsperiodeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryservice.InMemoryBehandlingService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.behandlingsflyt.repository.sak.ArenaMigreringRepositoryImpl
import no.nav.aap.behandlingsflyt.test.FakeArenaOppslagGateway
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDate

fun opprettSak(connection: DBConnection, søknadsdato: LocalDate): Sak {
    return opprettSak(connection, ident(), søknadsdato)
}

fun opprettSak(connection: DBConnection, ident: Ident, søknadsdato: LocalDate): Sak {
    return PersonOgSakService(
        FakePdlGateway,
        FakeArenaOppslagGateway.konstruer(),
        PersonRepositoryImpl(connection),
        SakRepositoryImpl(connection),
        ArenaMigreringRepositoryImpl(connection)
    ).finnEllerOpprett(ident, søknadsdato)
}

fun opprettInMemorySak(søknadsdato: LocalDate = LocalDate.now(), ident: Ident = ident()): Sak {
    return PersonOgSakService(
        FakePdlGateway,
        FakeArenaOppslagGateway.konstruer(),
        InMemoryPersonRepository,
        InMemorySakRepository,
        InMemoryArenaMigreringRepository
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
    opprettKravOgStønadsperiode(behandling, søknadsdato)
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
    InMemoryKravRepository.kopier(førstegangsbehandling.id, revurdering.id)
    InMemoryStønadsperiodeRepository.kopier(førstegangsbehandling.id, revurdering.id)

    check(førstegangsbehandling.id != revurdering.id)
    return Triple(sak, InMemoryBehandlingRepository.hent(førstegangsbehandling.id), revurdering)
}

private fun opprettKravOgStønadsperiode(behandling: Behandling, søknadsdato: LocalDate) {
    val referanse = Kravreferanse.ny()
    InMemoryKravRepository.lagre(
        behandling.id,
        setOf(
            RelevantKrav(
                referanse = referanse,
                journalpostId = JournalpostId(søknadsdato.toString()),
                vurdertAv = SYSTEMBRUKER,
                begrunnelse = "Automatisk opprettet krav fra søknad",
                vurdertIBehandling = behandling.id,
                opprettet = Instant.now(),
                søknadsdato = Søknadsdato(søknadsdato, SøknadsdatoÅrsak.SøknadMottatt, "Søknad mottatt"),
                overstyrMuligRettFra = null,
                muligRettFra = søknadsdato,
            )
        )
    )
    InMemoryStønadsperiodeRepository.lagre(
        behandling.id,
        setOf(
            StønadsperiodeVurdering(
                referanse = referanse,
                opprettet = Instant.now(),
                vurdertIBehandling = behandling.id,
                vurdertAv = SYSTEMBRUKER,
                begrunnelse = "Automatisk opprettet stønadsperiode fra søknadsdato",
                harHattOrdinærSiste52Uker = false,
                harGjenværendeKvote = false,
                relevantKravType = RelevantKravType.NY_STØNADSPERIODE,
                startDato = søknadsdato,
            )
        )
    )
}
