alter table products
    alter column name type jsonb using json_build_object('ru', name);