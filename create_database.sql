-- Vehicle Service System Database Creation Script
-- Run this script in SQL Server Management Studio (SSMS)
-- Make sure to run as a user with database creation permissions

-- Create the database
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'VehicleServiceDB')
BEGIN
    CREATE DATABASE VehicleServiceDB;
    PRINT 'Database VehicleServiceDB created successfully.';
END
ELSE
BEGIN
    PRINT 'Database VehicleServiceDB already exists.';
END
GO

-- Use the database
USE VehicleServiceDB;
GO

-- Create roles table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[roles]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[roles](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [role_name] [nvarchar](255) NOT NULL,
        [created_at] [datetime2](7) NOT NULL,
        [updated_at] [datetime2](7) NOT NULL,
        [is_active] [bit] NOT NULL DEFAULT 1,
        CONSTRAINT [PK_roles] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [UK_roles_role_name] UNIQUE ([role_name])
    );
    PRINT 'Table roles created successfully.';
END
ELSE
BEGIN
    PRINT 'Table roles already exists.';
END
GO

-- Create users table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[users]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[users](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [username] [nvarchar](255) NOT NULL,
        [password] [nvarchar](255) NOT NULL,
        [email] [nvarchar](255) NOT NULL,
        [first_name] [nvarchar](255) NOT NULL,
        [last_name] [nvarchar](255) NOT NULL,
        [phone_number] [nvarchar](20) NULL,
        [address] [nvarchar](500) NULL,
        [city] [nvarchar](100) NULL,
        [state] [nvarchar](100) NULL,
        [zip_code] [nvarchar](20) NULL,
        [date_of_birth] [datetime2](7) NULL,
        [last_login] [datetime2](7) NULL,
        [role_id] [bigint] NOT NULL,
        [created_at] [datetime2](7) NOT NULL,
        [updated_at] [datetime2](7) NOT NULL,
        [is_active] [bit] NOT NULL DEFAULT 1,
        CONSTRAINT [PK_users] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [UK_users_username] UNIQUE ([username]),
        CONSTRAINT [UK_users_email] UNIQUE ([email]),
        CONSTRAINT [UK_users_phone_number] UNIQUE ([phone_number])
    );
    PRINT 'Table users created successfully.';
END
ELSE
BEGIN
    PRINT 'Table users already exists.';
END
GO

-- Create technicians table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[technicians]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[technicians](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [user_id] [bigint] NOT NULL,
        [employee_id] [nvarchar](50) NOT NULL,
        [specialization] [nvarchar](255) NULL,
        [max_daily_workload] [int] NOT NULL DEFAULT 6,
        [current_workload] [int] NOT NULL DEFAULT 0,
        [hourly_rate] [decimal](10,2) NULL,
        [experience_years] [int] NOT NULL DEFAULT 0,
        [created_at] [datetime2](7) NOT NULL,
        [updated_at] [datetime2](7) NOT NULL,
        [is_active] [bit] NOT NULL DEFAULT 1,
        CONSTRAINT [PK_technicians] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [UK_technicians_user_id] UNIQUE ([user_id]),
        CONSTRAINT [UK_technicians_employee_id] UNIQUE ([employee_id])
    );
    PRINT 'Table technicians created successfully.';
END
ELSE
BEGIN
    PRINT 'Table technicians already exists.';
END
GO

-- Create bookings table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[bookings]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[bookings](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [booking_number] [nvarchar](50) NOT NULL,
        [customer_name] [nvarchar](100) NOT NULL,
        [vehicle_number] [nvarchar](20) NOT NULL,
        [service_type] [nvarchar](100) NOT NULL,
        [booking_date] [datetime2](7) NOT NULL,
        [payment_status] [nvarchar](20) NOT NULL,
        [service_price] [decimal](10,2) NULL,
        [additional_charges] [decimal](10,2) NULL,
        [total_price] [decimal](10,2) NOT NULL,
        [paid_amount] [decimal](10,2) NULL,
        [remaining_amount] [decimal](10,2) NULL,
        [notes] [nvarchar](1000) NULL,
        [created_at] [datetime2](7) NOT NULL,
        [updated_at] [datetime2](7) NOT NULL,
        CONSTRAINT [PK_bookings] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [UK_bookings_booking_number] UNIQUE ([booking_number])
    );
    PRINT 'Table bookings created successfully.';
END
ELSE
BEGIN
    PRINT 'Table bookings already exists.';
END
GO

-- Create technician_assignments table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[technician_assignments]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[technician_assignments](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [booking_id] [bigint] NOT NULL,
        [technician_id] [bigint] NOT NULL,
        [assigned_by] [bigint] NOT NULL,
        [assignment_date] [datetime2](7) NOT NULL,
        [status] [nvarchar](20) NOT NULL,
        [notes] [nvarchar](500) NULL,
        [created_at] [datetime2](7) NOT NULL,
        [updated_at] [datetime2](7) NOT NULL,
        [is_active] [bit] NOT NULL DEFAULT 1,
        CONSTRAINT [PK_technician_assignments] PRIMARY KEY CLUSTERED ([id] ASC)
    );
    PRINT 'Table technician_assignments created successfully.';
END
ELSE
BEGIN
    PRINT 'Table technician_assignments already exists.';
END
GO

-- Create inventory_items table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[inventory_items]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[inventory_items](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [item_name] [nvarchar](255) NOT NULL,
        [category] [nvarchar](100) NOT NULL,
        [quantity] [int] NOT NULL,
        [unit_price] [decimal](10,2) NOT NULL,
        [reorder_level] [int] NOT NULL,
        [created_at] [datetime2](7) NOT NULL,
        [updated_at] [datetime2](7) NOT NULL,
        [is_active] [bit] NOT NULL DEFAULT 1,
        CONSTRAINT [PK_inventory_items] PRIMARY KEY CLUSTERED ([id] ASC)
    );
    PRINT 'Table inventory_items created successfully.';
END
ELSE
BEGIN
    PRINT 'Table inventory_items already exists.';
END
GO

-- Create inventory_transactions table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[inventory_transactions]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[inventory_transactions](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [item_id] [bigint] NOT NULL,
        [transaction_type] [nvarchar](10) NOT NULL,
        [quantity] [int] NOT NULL,
        [date] [datetime2](7) NOT NULL,
        [staff_id] [bigint] NOT NULL,
        [created_at] [datetime2](7) NOT NULL,
        [updated_at] [datetime2](7) NOT NULL,
        [is_active] [bit] NOT NULL DEFAULT 1,
        CONSTRAINT [PK_inventory_transactions] PRIMARY KEY CLUSTERED ([id] ASC)
    );
    PRINT 'Table inventory_transactions created successfully.';
END
ELSE
BEGIN
    PRINT 'Table inventory_transactions already exists.';
END
GO

-- Create feedback table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[feedback]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[feedback](
        [id] [bigint] IDENTITY(1,1) NOT NULL,
        [rating] [int] NOT NULL,
        [comment] [nvarchar](500) NULL,
        [bookings_id] [bigint] NOT NULL,
        [users_id] [bigint] NOT NULL,
        [created_at] [datetime2](7) NOT NULL,
        [updated_at] [datetime2](7) NOT NULL,
        [is_active] [bit] NOT NULL DEFAULT 1,
        CONSTRAINT [PK_feedback] PRIMARY KEY CLUSTERED ([id] ASC)
    );
    PRINT 'Table feedback created successfully.';
END
ELSE
BEGIN
    PRINT 'Table feedback already exists.';
END
GO

-- Add Foreign Key Constraints
-- Users -> Roles
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_users_role_id')
BEGIN
    ALTER TABLE [dbo].[users] 
    ADD CONSTRAINT [FK_users_role_id] 
    FOREIGN KEY([role_id]) REFERENCES [dbo].[roles] ([id]);
    PRINT 'Foreign key FK_users_role_id added.';
END

-- Technicians -> Users
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_technicians_user_id')
BEGIN
    ALTER TABLE [dbo].[technicians] 
    ADD CONSTRAINT [FK_technicians_user_id] 
    FOREIGN KEY([user_id]) REFERENCES [dbo].[users] ([id]);
    PRINT 'Foreign key FK_technicians_user_id added.';
END

-- Technician Assignments -> Bookings
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_technician_assignments_booking_id')
BEGIN
    ALTER TABLE [dbo].[technician_assignments] 
    ADD CONSTRAINT [FK_technician_assignments_booking_id] 
    FOREIGN KEY([booking_id]) REFERENCES [dbo].[bookings] ([id]);
    PRINT 'Foreign key FK_technician_assignments_booking_id added.';
END

-- Technician Assignments -> Technicians
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_technician_assignments_technician_id')
BEGIN
    ALTER TABLE [dbo].[technician_assignments] 
    ADD CONSTRAINT [FK_technician_assignments_technician_id] 
    FOREIGN KEY([technician_id]) REFERENCES [dbo].[technicians] ([id]);
    PRINT 'Foreign key FK_technician_assignments_technician_id added.';
END

-- Technician Assignments -> Users (assigned_by)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_technician_assignments_assigned_by')
BEGIN
    ALTER TABLE [dbo].[technician_assignments] 
    ADD CONSTRAINT [FK_technician_assignments_assigned_by] 
    FOREIGN KEY([assigned_by]) REFERENCES [dbo].[users] ([id]);
    PRINT 'Foreign key FK_technician_assignments_assigned_by added.';
END

-- Inventory Transactions -> Inventory Items
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_inventory_transactions_item_id')
BEGIN
    ALTER TABLE [dbo].[inventory_transactions] 
    ADD CONSTRAINT [FK_inventory_transactions_item_id] 
    FOREIGN KEY([item_id]) REFERENCES [dbo].[inventory_items] ([id]);
    PRINT 'Foreign key FK_inventory_transactions_item_id added.';
END

-- Inventory Transactions -> Users (staff_id)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_inventory_transactions_staff_id')
BEGIN
    ALTER TABLE [dbo].[inventory_transactions] 
    ADD CONSTRAINT [FK_inventory_transactions_staff_id] 
    FOREIGN KEY([staff_id]) REFERENCES [dbo].[users] ([id]);
    PRINT 'Foreign key FK_inventory_transactions_staff_id added.';
END

-- Feedback -> Bookings
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_feedback_bookings_id')
BEGIN
    ALTER TABLE [dbo].[feedback] 
    ADD CONSTRAINT [FK_feedback_bookings_id] 
    FOREIGN KEY([bookings_id]) REFERENCES [dbo].[bookings] ([id]);
    PRINT 'Foreign key FK_feedback_bookings_id added.';
END

-- Feedback -> Users
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_feedback_users_id')
BEGIN
    ALTER TABLE [dbo].[feedback] 
    ADD CONSTRAINT [FK_feedback_users_id] 
    FOREIGN KEY([users_id]) REFERENCES [dbo].[users] ([id]);
    PRINT 'Foreign key FK_feedback_users_id added.';
END

PRINT 'Database schema creation completed successfully!';
PRINT 'You can now run the populate_data.sql script to add sample data.';
GO
