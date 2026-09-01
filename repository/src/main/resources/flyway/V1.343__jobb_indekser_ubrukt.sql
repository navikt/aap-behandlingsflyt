-- Denne har tilnærmet null treff i bruk
drop index idx_jobb_neste_kjoring_sak_behandling;

-- Denne har null treff i bruk
drop index idx_jobb_sak;

-- Denne har null treff i bruk etter overgang til prioritet med egen indeks på idx_jobb_plukkbar_prioritet
drop index idx_jobb_plukkbar;

-- Denne overlapper med en annen index (idx_jobb_status_neste_kjoring) som vi uansett må ha, og kan derfor slettes siden den er duplikat
drop index idx_jobb_status;

-- Denne dekkes delvis av idx_jobb_behandling og brukes svært lite - alltid i kombinasjon med status og dermed dekkes den av idx_jobb_status_neste_kjoring
drop index idx_jobb_sak_behandling;