alter table categories
    alter column name type jsonb using json_build_object('ru', name);