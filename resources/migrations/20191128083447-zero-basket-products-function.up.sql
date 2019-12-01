CREATE FUNCTION delete_basket_product() RETURNS trigger
AS
$BODY$
BEGIN
  if NEW.count = 0 then
    DELETE FROM basket_products WHERE basket_products.id = NEW.id;
  end if;
  return NEW;
END;
$BODY$
  LANGUAGE plpgsql
  VOLATILE;
