alter table kitchens
    add bot_id int references bots (id);
