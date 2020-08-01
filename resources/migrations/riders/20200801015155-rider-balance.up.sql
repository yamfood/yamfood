create table "rider_balance"
(
    id          serial                   not null,
    rider_id    int                      not null references riders (id),
    admin_id    int                      null references admins (id),
    amount      bigint                   not null,
    created_at  timestamp with time zone not null default current_timestamp,
    payload     jsonb                    not null default '{}',
    description varchar                           default '',

    primary key (id)
)