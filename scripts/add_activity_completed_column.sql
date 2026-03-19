-- Migration script to add activity_completed column to stocks table
-- Run this manually if Hibernate doesn't create the column automatically

ALTER TABLE stocks 
ADD COLUMN IF NOT EXISTS activity_completed BOOLEAN NOT NULL DEFAULT false;

-- Add index for better query performance
CREATE INDEX IF NOT EXISTS idx_stocks_activity_completed ON stocks(activity_completed);

-- Update any existing records to have default value
UPDATE stocks 
SET activity_completed = false 
WHERE activity_completed IS NULL;
