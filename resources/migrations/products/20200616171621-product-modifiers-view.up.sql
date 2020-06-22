CREATE VIEW product_modifiers AS
(
select id                                                                 as product_id,
       (jsonb_array_elements(payload -> 'groupModifiers') ->> 'required') as group_required,
       (jsonb_array_elements(payload -> 'groupModifiers') ->> 'group_id')::uuid                            as group_id,
       jsonb_array_elements_text(jsonb_array_elements(payload -> 'groupModifiers') -> 'modifiers') ::uuid as modifier_id
from products
where payload ->> 'groupModifiers' is not null
group by id)
