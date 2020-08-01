create table "rider_deposits"
(
    id         serial                   not null,
    rider_id   int                      not null references riders (id),
    admin_id   int                      not null references admins (id),
    amount     int                      not null,
    created_at timestamp with time zone not null default current_timestamp,
    payload    jsonb                    not null default '{}',

    primary key (id)
)