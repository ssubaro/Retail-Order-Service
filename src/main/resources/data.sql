-- Datos de ejemplo para el catálogo de productos.
-- Se ejecuta en cada arranque contra la base H2 en memoria (ver spring.jpa.defer-datasource-initialization
-- en application.properties, necesario para que corra DESPUES de que Hibernate cree las tablas).
-- Al detener la aplicación esta base desaparece (jdbc:h2:mem:...), así que estos datos nunca persisten
-- entre ejecuciones: solo sirven para tener algo con qué probar Swagger UI / H2 console / Postman sin
-- tener que dar de alta productos a mano cada vez.


/* 
INSERT INTO products (sku, name, description, price, stock, category, active, version) VALUES
    ('SKU-DEMO-001', 'Teclado Mecanico RGB', 'Switches rojos, retroiluminado', 899.00, 15, 'Electronica', true, 0),
    ('SKU-DEMO-002', 'Mouse Inalambrico', 'Sensor optico 1600dpi', 299.50, 40, 'Electronica', true, 0),
    ('SKU-DEMO-003', 'Monitor 24 pulgadas', 'Full HD, panel IPS', 2499.00, 8, 'Electronica', true, 0),
    ('SKU-DEMO-004', 'Playera Basica', 'Algodon 100%, varias tallas', 149.90, 120, 'Ropa', true, 0),
    ('SKU-DEMO-005', 'Chamarra Impermeable', 'Resistente al agua, talla M', 899.00, 4, 'Ropa', true, 0),
    ('SKU-DEMO-006', 'Cafetera Automatica', '12 tazas, programable', 1199.00, 10, 'Hogar', true, 0),
    ('SKU-DEMO-007', 'Set de Sartenes', 'Antiadherente, 3 piezas', 699.00, 25, 'Hogar', true, 0),
    ('SKU-DEMO-008', 'Audifonos Bluetooth', 'Cancelacion de ruido activa', 1599.00, 3, 'Electronica', true, 0); */
