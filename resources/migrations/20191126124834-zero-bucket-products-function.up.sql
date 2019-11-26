CREATE FUNCTION delete_bucket_product() RETURNS trigger
AS $BODY$
BEGIN
  if NEW.count = 0 then
    DELETE FROM bucket_products WHERE bucket_products.id = NEW.id;
  end if;
  return NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE;
