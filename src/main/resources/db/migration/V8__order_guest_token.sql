-- SECURITY (H5): token opaco per-ordine per autorizzare le mutazioni dei guest order.
-- NULL per gli ordini esistenti/autenticati; valorizzato alla creazione di un guest order.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS guest_token VARCHAR(64);
