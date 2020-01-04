create table regions
(
  id      serial       not null
    constraint regions_pk
      primary key,
  name    varchar(200) not null,
  polygon polygon      not null
);
