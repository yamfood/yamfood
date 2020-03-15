create table params
(
    id    serial,
    name  varchar(200) not null,
    value varchar      not null,

    primary key (id)
);