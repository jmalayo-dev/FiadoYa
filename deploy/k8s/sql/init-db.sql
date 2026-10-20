-- Crea las 5 bases de FiadoYa y el login de aplicación (Anexo §2.2). Idempotente: se puede correr varias veces.
-- Se ejecuta con: sqlcmd -v DB_USER="..." DB_PASSWORD="..." -i init-db.sql
IF DB_ID('fiadoya_auth')       IS NULL CREATE DATABASE fiadoya_auth;
IF DB_ID('fiadoya_inventario') IS NULL CREATE DATABASE fiadoya_inventario;
IF DB_ID('fiadoya_clientes')   IS NULL CREATE DATABASE fiadoya_clientes;
IF DB_ID('fiadoya_ventas')     IS NULL CREATE DATABASE fiadoya_ventas;
IF DB_ID('fiadoya_reportes')   IS NULL CREATE DATABASE fiadoya_reportes;
GO

-- Login de aplicación sin permisos de sa.
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = '$(DB_USER)')
    CREATE LOGIN [$(DB_USER)] WITH PASSWORD = '$(DB_PASSWORD)', CHECK_POLICY = OFF;
ELSE
    ALTER LOGIN [$(DB_USER)] WITH PASSWORD = '$(DB_PASSWORD)';
GO

-- db_owner solo sobre cada una de las 5 bases.
USE fiadoya_auth;
IF USER_ID('$(DB_USER)') IS NULL CREATE USER [$(DB_USER)] FOR LOGIN [$(DB_USER)];
ALTER ROLE db_owner ADD MEMBER [$(DB_USER)];
GO
USE fiadoya_inventario;
IF USER_ID('$(DB_USER)') IS NULL CREATE USER [$(DB_USER)] FOR LOGIN [$(DB_USER)];
ALTER ROLE db_owner ADD MEMBER [$(DB_USER)];
GO
USE fiadoya_clientes;
IF USER_ID('$(DB_USER)') IS NULL CREATE USER [$(DB_USER)] FOR LOGIN [$(DB_USER)];
ALTER ROLE db_owner ADD MEMBER [$(DB_USER)];
GO
USE fiadoya_ventas;
IF USER_ID('$(DB_USER)') IS NULL CREATE USER [$(DB_USER)] FOR LOGIN [$(DB_USER)];
ALTER ROLE db_owner ADD MEMBER [$(DB_USER)];
GO
USE fiadoya_reportes;
IF USER_ID('$(DB_USER)') IS NULL CREATE USER [$(DB_USER)] FOR LOGIN [$(DB_USER)];
ALTER ROLE db_owner ADD MEMBER [$(DB_USER)];
GO
PRINT 'Bases de FiadoYa listas';
