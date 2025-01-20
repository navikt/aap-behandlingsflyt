alter table brevbestilling
add column opprettet timestamp(3) default current_timestamp not null;
