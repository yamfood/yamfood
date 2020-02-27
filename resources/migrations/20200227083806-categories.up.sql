create table categories
(
    id    serial       not null,
    name  varchar(200) not null,
    emoji varchar(100) not null,

    primary key (id)
)