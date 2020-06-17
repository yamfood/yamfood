CREATE VIEW product_modifiers AS
(
select m.id,
       m.name,
       m.price,
       m.group_id,
       products.id          products_id,
       products.name        products_name,
       products.photo       products_photo,
       products.energy      products_energy,
       products.price       products_price,
       products.thumbnail   products_thumbnail,
       products.is_active   products_is_active,
       products.category_id products_category_id,
       products.payload     products_payload,
       products.position    products_position,
       products.description products_description
from (select p.*, modifiers
      from products as p
               cross join lateral (
          select jsonb_agg(value::jsonb) AS modifiersGroup
          from jsonb_array_elements_text(p.payload -> 'groupModifiers')) as d,
          jsonb_populate_recordset(null::record, d.modifiersGroup) AS (modifiers uuid[])
      where modifiersGroup is not null) as products
         right join modifiers as m on m.id = any (products.modifiers))
